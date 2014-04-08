/*
The MIT License

Copyright (c) 2009-2014 NOrad Ltd.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package com.noradltd.easybuilder;

import java.util.*;
import java.lang.reflect.Field;

import com.noradltd.easybuilder.EasyBuilder;
import com.noradltd.easybuilder.EasyBuilder.InstantiateInstruction;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Nilanjan Raychaundhrui, nraychaudhrui@pillartechnology.com
 * @author Rich Dammkoehler, rdammkoehler@gmail.com (NOrad Ltd.)
 */
public class EasyBuilderTest extends TestCase {

	public void testShouldReinitiateAnInstanceUsingFieldsDirectly() {
		SomeNonJavaBean nonBean = new SomeNonJavaBean("someValue", 42);

		EasyBuilder builder = new EasyBuilder(SomeNonJavaBean.class) {
			{
				bypassConstructor();
				setField("param1", "someValue");
				setField("param2", 42);
			}
		};
		SomeNonJavaBean anotherNonBean = (SomeNonJavaBean) builder.build();
		assertEquals(nonBean, anotherNonBean);
	}

	public void testShouldReinitiateAnInstanceUsingFieldsDirectlyEvenWhenItsComplex() {
		final Date today = new Date();
		SomeParameter someParameter = new SomeParameter(today, 0.0);
		SomeNonJavaBean nonBean = new SomeNonJavaBean("someValue", 42, someParameter);

		final EasyBuilder paramBuilder = new EasyBuilder(SomeParameter.class) {
			{
				bypassConstructor();
				setField("someDate", today);
				setField("money", 0.0);
			}
		};

		EasyBuilder nonBeanBuilder = new EasyBuilder(SomeNonJavaBean.class) {
			{
				bypassConstructor();
				setField("param1", "someValue");
				setField("param2", 42);
				setField("someParameter", paramBuilder.build());
			}
		};
		SomeNonJavaBean anotherNonBean = (SomeNonJavaBean) nonBeanBuilder.build();

		assertEquals(nonBean, anotherNonBean);
		assertEquals(someParameter.someDate, anotherNonBean.getSomeParameter().someDate);
	}

	public void testShouldInitializeSuperClassFieldsWhenInitializingChildClassInstance() {
		Child c = new Child("child", "parent");
		final EasyBuilder builder = new EasyBuilder(Child.class) {
			{
				bypassConstructor();
				setField("childName", "child");
				setField("parentName", "parent");
			}
		};
		Child anotherChild = (Child) builder.build();
		assertEquals(c.childName, anotherChild.childName);
		assertEquals(c.parentName, anotherChild.parentName);
	}

	public void testConstructorShouldBeCalledByDefault() {
		// setup
		EasyBuilder builder = new EasyBuilder(ConstructorCalledClass.class);
		// execute
		ConstructorCalledClass testClass = (ConstructorCalledClass) builder.build();
		// assert
		Assert.assertTrue(testClass.constructorCalled);
	}

	public void testConstructorShouldBeCalledAndThrowsException() {
		// setup
		EasyBuilder builder = new EasyBuilder(SomeNonJavaBean.class);
		// execute
		try {
			builder.build();
			// assert
			Assert.fail("Should have received an exception from the constructor.");
		} catch (RuntimeException re) {
			// OK
		}
	}

	public void testConstructorByPassedWhenRequested() {
		// setup
		EasyBuilder builder = new EasyBuilder(ConstructorCalledClass.class) {
			{
				bypassConstructor();
			}
		};
		// execute
		ConstructorCalledClass testClass = (ConstructorCalledClass) builder.build();
		// assert
		Assert.assertFalse(testClass.constructorCalled);
	}

	public void testNullInitializationClass_ReceiveExceptionFromUnknownClassByNullPointer() {
		// setup
		EasyBuilder builder = new EasyBuilder(null);
		// execute
		try {
			builder.build();
			// assert
			Assert.fail("Should have received an exception");
		} catch (RuntimeException re) {
			// OK
		}
	}

	public void testFieldNotFoundGeneratesException() {
		// setup
		EasyBuilder builder = new EasyBuilder(SomeNonJavaBean.class) {
			{
				bypassConstructor();
				setField("nonExistentField", "anyvaluewilldo");
			}
		};
		// execute
		try {
			builder.build();
			// assert
			Assert.fail("Should have received an exception");
		} catch (RuntimeException re) {
			// OK
		}
	}

	public void testBuilderBuilderBuilds() {
		// setup/execute
		EasyBuilder builder = (EasyBuilder) new EasyBuilder(EasyBuilder.class) {
			{
				bypassConstructor();
			}
		}.build();
		// assert
		Assert.assertNotNull(builder);
		try {
			builder.build();
			Assert.fail("The builder is uninitialized and therefore should fail to build with a NullPointerException");
		} catch (NullPointerException npe) {
			// OK
		}

	}

	public void testMultipleInstantiateIntructionsResultInSingleInstantiation() {
		// setup
		InstanceCounter.reset();
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				List assemblyInstructions = new ArrayList();
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new BasicInstantiateInstruction());

				bypassConstructor();
				setField("clazz", InstanceCounter.class);
				setField("assemblyInstructions", assemblyInstructions);
			}
		};
		// execute
		InstanceCounter instance = (InstanceCounter) ((EasyBuilder) builder.build()).build();
		// assert
		Assert.assertEquals("Instance Id should be 1", 1, instance.id);
		Assert.assertEquals("Instance Count should be 1", 1, InstanceCounter.count);
		Assert.assertTrue("Instance should have been constructed", instance.constructed);
	}

	public void testNonAssemblyInstructionAtBeginingIsResortedBehindAssemblyInstructions() throws NoSuchFieldException, IllegalAccessException {
		// setup
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				List assemblyInstructions = new ArrayList();
				assemblyInstructions.add(new SetFieldInstruction("childName", "Xanadu"));

				bypassConstructor();
				setField("clazz", Child.class);
				setField("assemblyInstructions", assemblyInstructions);
			}
		};
		// execute
		EasyBuilder result = (EasyBuilder) builder.build();
		result.bypassConstructor();
		// assert
		List assemblyInstructions = (List) getPrivateFieldValue("assemblyInstructions", result, EasyBuilder.class);
		Assert.assertTrue(assemblyInstructions.get(0) instanceof EasyBuilder.InstantiateInstruction);
		EasyBuilder.InstantiateInstruction instantiateInstruction = (InstantiateInstruction) assemblyInstructions.get(0);
		Assert.assertTrue(instantiateInstruction instanceof EasyBuilder.BypassingInstantiateInstruction);
	}

	public void testMultipleInstantiateIntructionsWithByPassResultsInSingleBypassingInstantiation() {
		// setup
		InstanceCounter.reset();
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				List assemblyInstructions = new ArrayList();
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new BypassingInstantiateInstruction());

				bypassConstructor();
				setField("clazz", InstanceCounter.class);
				setField("assemblyInstructions", assemblyInstructions);
			}
		};
		// execute
		InstanceCounter instance = (InstanceCounter) ((EasyBuilder) builder.build()).build();
		// assert
		Assert.assertEquals("Instance Id should be 0", 0, instance.id);
		Assert.assertEquals("Instance Count should be 0", 0, InstanceCounter.count);
		Assert.assertFalse("Instance should not have been constructed", instance.constructed);
	}

	public void testUnorderedIntructionsResultsInInstantiationFirst() {
		// setup
		InstanceCounter.reset();
		final List assemblyInstructions = new ArrayList();
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				assemblyInstructions.add(new SetFieldInstruction("id", 6));
				assemblyInstructions.add(new SetFieldInstruction("id", 4));
				assemblyInstructions.add(new BypassingInstantiateInstruction());
				assemblyInstructions.add(new SetFieldInstruction("id", 2));
				bypassConstructor();
				setField("clazz", InstanceCounter.class);
				setField("assemblyInstructions", assemblyInstructions);
			}
		};
		// execute
		InstanceCounter instance = (InstanceCounter) ((EasyBuilder) builder.build()).build();
		// assert
		Assert.assertEquals("Instance Id should be 2", 2, instance.id);
		Assert.assertEquals("Instance Count should be 0", 0, InstanceCounter.count);
		Assert.assertFalse("Instance should not have been constructed", instance.constructed);
	}

	public void testUnInitializedBuilderIsSafe() {
		// setup
		InstanceCounter.reset();
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				List assemblyInstructions = new ArrayList();
				bypassConstructor();
				setField("clazz", InstanceCounter.class);
				setField("assemblyInstructions", assemblyInstructions);
			}
		};
		// execute
		InstanceCounter instance = (InstanceCounter) ((EasyBuilder) builder.build()).build();
		// assert
		Assert.assertEquals("Instance Id should be 1", 1, instance.id);
		Assert.assertEquals("Instance Count should be 1", 1, InstanceCounter.count);
		Assert.assertTrue("Instance should have been constructed", instance.constructed);
	}

	public void testMultipleInstantiateIntructionsWithAllByPassResultsInSingleBypassingInstantiation() {
		// setup
		InstanceCounter.reset();
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				List assemblyInstructions = new ArrayList();
				assemblyInstructions.add(new BypassingInstantiateInstruction());
				assemblyInstructions.add(new BypassingInstantiateInstruction());

				bypassConstructor();
				setField("clazz", InstanceCounter.class);
				setField("assemblyInstructions", assemblyInstructions);
			}
		};
		// execute
		InstanceCounter instance = (InstanceCounter) ((EasyBuilder) builder.build()).build();
		// assert
		Assert.assertEquals("Instance Id should be 0", 0, instance.id);
		Assert.assertEquals("Instance Count should be 0", 0, InstanceCounter.count);
		Assert.assertFalse("Instance should not have been constructed", instance.constructed);
	}

	public void testBuildeAClassWithOneOfEachPrimitive() {
		// setup
		final char c = 'a';
		final byte b = 1;
		final short s = 2;
		final int i = 3;
		final long l = 4;
		final float f = 12.01f;
		final double d = 42.3;
		final boolean t = true;
		EasyBuilder builder = new EasyBuilder(OneOfEach.class) {
			{
				bypassConstructor();
				setField("c", c);
				setField("b", b);
				setField("s", s);
				setField("i", i);
				setField("l", l);
				setField("f", f);
				setField("d", d);
				setField("t", t);
			}
		};
		// execute
		OneOfEach instance = (OneOfEach) builder.build();
		// assert
		Assert.assertEquals(c, instance.c);
		Assert.assertEquals(b, instance.b);
		Assert.assertEquals(s, instance.s);
		Assert.assertEquals(i, instance.i);
		Assert.assertEquals(l, instance.l);
		Assert.assertEquals(f, instance.f, 0.001);
		Assert.assertEquals(d, instance.d, 0.001);
		Assert.assertEquals(t, instance.t);
	}

	public void testSetWithMap() {
		// setup
		Object obj = new Object();
		final Map map = new HashMap();
		map.put("obj", obj);

		EasyBuilder builder = new EasyBuilder(OneOfEachPlus.class) {
			{
				bypassConstructor();
				setFields(map);
			}
		};
		// execute
		OneOfEachPlus instance = (OneOfEachPlus) builder.build();
		// assert
		Assert.assertEquals(obj, instance.obj);
	}

	/**
	 * @noinspection UnnecessaryBoxing,CachedNumberConstructorCall,BooleanConstructorCall
	 */
	public void testSetWithMapWithPrimitives() {
		// setup
		char c = 'a';
		byte b = 1;
		short s = 2;
		int i = 3;
		long l = 4;
		float f = 12.01f;
		double d = 42.3;
		boolean t = true;
		Object obj = new Object();
		final Map map = new HashMap();
		map.put("c", new Character(c));
		map.put("b", new Byte(b));
		map.put("s", new Short(s));
		map.put("i", new Integer(i));
		map.put("l", new Long(l));
		map.put("f", new Float(f));
		map.put("d", new Double(d));
		map.put("t", new Boolean(t));
		map.put("obj", obj);

		EasyBuilder builder = new EasyBuilder(OneOfEachPlus.class) {
			{
				bypassConstructor();
				setFields(map);
			}
		};
		// execute
		OneOfEachPlus instance = (OneOfEachPlus) builder.build();
		// assert
		Assert.assertEquals(c, instance.c);
		Assert.assertEquals(b, instance.b);
		Assert.assertEquals(s, instance.s);
		Assert.assertEquals(i, instance.i);
		Assert.assertEquals(l, instance.l);
		Assert.assertEquals(f, instance.f, 0.001);
		Assert.assertEquals(d, instance.d, 0.001);
		Assert.assertEquals(t, instance.t);
		Assert.assertEquals(obj, instance.obj);
	}

	// just to show that it can be done
	public void testDateSets() {
		// setup
		final Calendar cal = Calendar.getInstance();
		cal.set(1970, 0, 1, 0, 0, 0); // epoch
		final Date date = Calendar.getInstance().getTime();
		EasyBuilder builder = new EasyBuilder(Dates.class) {
			{
				bypassConstructor();
				setField("cal", cal);
				setField("date", date);
			}
		};
		// execute
		Dates instance = (Dates) builder.build();
		// assert
		Assert.assertEquals(cal, instance.cal);
		Assert.assertEquals(date, instance.date);
	}

	public void testPrivateMethodCalled() {
		// setup
		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("privateMethod", null);
			}
		};
		// execute
		PMethods instance = (PMethods) builder.build();
		// assert
		Assert.assertTrue(instance.privateCalled);
	}

	public void testProtectedMethodCalled() {
		// setup
		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("protectedMethod", null);
			}
		};
		// execute
		PMethods instance = (PMethods) builder.build();
		// assert
		Assert.assertTrue(instance.protectedCalled);
	}

	public void testPackageMethodCalled() {
		// setup
		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("packageMethod", null);
			}
		};
		// execute
		PMethods instance = (PMethods) builder.build();
		// assert
		Assert.assertTrue(instance.packageCalled);
	}

	public void testPublicMethodCalled() {
		// setup
		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("publicMethod", null);
			}
		};
		// execute
		PMethods instance = (PMethods) builder.build();
		// assert
		Assert.assertTrue(instance.publicCalled);
	}

	public void testFinalMethodCalled() {
		// setup
		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("finalMethod", null);
			}
		};
		// execute
		PMethods instance = (PMethods) builder.build();
		// assert
		Assert.assertTrue(instance.finalCalled);
	}

	public void testStaticMethodCalled() {
		// setup
		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("staticMethod", null);
			}
		};
		// execute
		builder.build();
		// assert
		Assert.assertTrue(PMethods.staticCalled);
	}

	public void testReturnMethodCalled() {
		// setup
		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("returnMethod", null);
			}
		};
		// execute
		PMethods instance = (PMethods) builder.build();
		// assert
		Assert.assertTrue(instance.returnCalled);
	}

	public void testNoDefaultConstructorClassWithoutBypassThrowsException() {
		// setup
		EasyBuilder builder = new EasyBuilder(NoDefConst.class);
		// execute
		try {
			builder.build();
			// assert
			Assert.fail("Expected InstantiationExcpetion");
		} catch (Exception e) {
			Assert.assertTrue(e.getCause() instanceof InstantiationException);
		}
	}

	public void testNoDefaultConstructorClassWithBypassDoesNotThrowException() {
		// setup
		EasyBuilder builder = new EasyBuilder(NoDefConst.class) {
			{
				bypassConstructor();
			}
		};
		// execute
		NoDefConst instance = (NoDefConst) builder.build();
		// assert
		Assert.assertFalse(instance.primaryConstCalled);
	}

	public void testPrivateMethodCalledWhenExtended() {
		// setup
		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("privateMethod", null);
			}
		};
		// execute
		ExtensionOfPMethods instance = (ExtensionOfPMethods) builder.build();
		// assert
		Assert.assertTrue(instance.privateCalled);
	}

	public void testProtectedMethodCalledWhenExtended() {
		// setup
		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("protectedMethod", null);
			}
		};
		// execute
		ExtensionOfPMethods instance = (ExtensionOfPMethods) builder.build();
		// assert
		Assert.assertTrue(instance.protectedCalled);
	}

	public void testPackageMethodCalledWhenExtended() {
		// setup
		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("packageMethod", null);
			}
		};
		// execute
		ExtensionOfPMethods instance = (ExtensionOfPMethods) builder.build();
		// assert
		Assert.assertTrue(instance.packageCalled);
	}

	public void testPublicMethodCalledWhenExtended() {
		// setup
		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("publicMethod", null);
			}
		};
		// execute
		ExtensionOfPMethods instance = (ExtensionOfPMethods) builder.build();
		// assert
		Assert.assertTrue(instance.publicCalled);
	}

	public void testFinalMethodCalledWhenExtended() {
		// setup
		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("finalMethod", null);
			}
		};
		// execute
		ExtensionOfPMethods instance = (ExtensionOfPMethods) builder.build();
		// assert
		Assert.assertTrue(instance.finalCalled);
	}

	public void testStaticMethodCalledWhenExtended() {
		// setup
		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("staticMethod", null);
			}
		};
		// execute
		builder.build();
		// assert
		Assert.assertTrue(PMethods.staticCalled);
	}

	public void testReturnMethodCalledWhenExtended() {
		// setup
		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("returnMethod", null);
			}
		};
		// execute
		ExtensionOfPMethods instance = (ExtensionOfPMethods) builder.build();
		// assert
		Assert.assertTrue(instance.returnCalled);
	}

	public void testUseAlternateConstructor() {
		// setup
		final Object[] args = new Object[] { new Object() };
		EasyBuilder builder = new EasyBuilder(AltConstructor.class) {
			{
				useAlternateConstructor(args);
			}
		};
		// execute
		AltConstructor instance = (AltConstructor) builder.build();
		// assert
		Assert.assertFalse(instance.defaultWasCalled);
		Assert.assertTrue(instance.altWasCalled);
		Assert.assertSame(args[0], instance.obj);
	}

	public void testUseAlternateConstructorThroughExtensionClassReturnsSuperType() {
		// setup
		final Object[] args = new Object[] { new Object() };
		EasyBuilder builder = new EasyBuilder(ExtendsAltConstructor.class) {
			{
				useAlternateConstructor(args);
			}
		};
		// execute
		Object obj = builder.build();
		// assert
		Assert.assertTrue("unexpected type " + obj.getClass().getName(), obj instanceof AltConstructor);
		AltConstructor instance = (AltConstructor) obj;
		Assert.assertFalse(instance.defaultWasCalled);
		Assert.assertTrue(instance.altWasCalled);
		Assert.assertSame(args[0], instance.obj);
		// no need to assert that the substance field isn't there, we know that
		// by type.
	}

	private Object getPrivateFieldValue(String fieldName, Object targetObject, Class clazz) throws NoSuchFieldException, IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(targetObject);
	}

	public void testFieldMaskingShouldSetChildTypesField() throws IllegalAccessException, NoSuchFieldException {
		// setup
		final Object obj = new Object();
		EasyBuilder builder = new EasyBuilder(ExtendsSuperType.class) {
			{
				bypassConstructor();
				setField("x", obj);

			}
		};
		// execute
		ExtendsSuperType instance = (ExtendsSuperType) builder.build();
		// assert
		Assert.assertSame(obj, getPrivateFieldValue("x", instance, ExtendsSuperType.class));
		Assert.assertNull(getPrivateFieldValue("x", instance, SuperType.class));
	}

	public void testSetPrivateFieldValueBypassingFieldMaskingShouldSetParentTypesField() throws IllegalAccessException, NoSuchFieldException {
		// setup
		final Object obj = new Object();
		EasyBuilder builder = new EasyBuilder(ExtendsSuperType.class) {
			{
				bypassConstructor();
				setField("x", obj, SuperType.class);
			}
		};
		// execute
		ExtendsSuperType instance = (ExtendsSuperType) builder.build();
		// assert
		Assert.assertSame(obj, getPrivateFieldValue("x", instance, SuperType.class));
		Assert.assertNull(getPrivateFieldValue("x", instance, ExtendsSuperType.class));
	}

	public void testSetPrivatePrimitiveFieldValue() throws NoSuchFieldException, IllegalAccessException {
		// setup
		final char c = 'a';
		final byte b = 1;
		final short s = 2;
		final int i = 3;
		final long l = 4;
		final float f = 12.01f;
		final double d = 42.3;
		final boolean t = true;
		EasyBuilder builder = new EasyBuilder(OneOfEachPrivates.class) {
			{
				bypassConstructor();
				setField("c", c);
				setField("b", b);
				setField("s", s);
				setField("i", i);
				setField("l", l);
				setField("f", f);
				setField("d", d);
				setField("t", t);
			}
		};
		// execute
		OneOfEachPrivates instance = (OneOfEachPrivates) builder.build();
		// assert
		Assert.assertEquals(c, ((Character) getPrivateFieldValue("c", instance, OneOfEachPrivates.class)).charValue());
		Assert.assertEquals(b, ((Byte) getPrivateFieldValue("b", instance, OneOfEachPrivates.class)).byteValue());
		Assert.assertEquals(s, ((Short) getPrivateFieldValue("s", instance, OneOfEachPrivates.class)).shortValue());
		Assert.assertEquals(i, ((Integer) getPrivateFieldValue("i", instance, OneOfEachPrivates.class)).intValue());
		Assert.assertEquals(l, ((Long) getPrivateFieldValue("l", instance, OneOfEachPrivates.class)).longValue());
		Assert.assertEquals(f, ((Float) getPrivateFieldValue("f", instance, OneOfEachPrivates.class)).floatValue(), 0.001);
		Assert.assertEquals(d, ((Double) getPrivateFieldValue("d", instance, OneOfEachPrivates.class)).doubleValue(), 0.001);
		Assert.assertEquals(t, ((Boolean) getPrivateFieldValue("t", instance, OneOfEachPrivates.class)).booleanValue());
	}

	public void testSetParentClassPrivatePrimitiveFieldValue() throws NoSuchFieldException, IllegalAccessException {
		// ExtendsOneOfEachPrivates
		// setup
		final char c = 'a';
		final byte b = 1;
		final short s = 2;
		final int i = 3;
		final long l = 4;
		final float f = 12.01f;
		final double d = 42.3;
		final boolean t = true;
		EasyBuilder builder = new EasyBuilder(ExtendsOneOfEachPrivates.class) {
			{
				bypassConstructor();
				setField("c", c, OneOfEachPrivates.class);
				setField("b", b, OneOfEachPrivates.class);
				setField("s", s, OneOfEachPrivates.class);
				setField("i", i, OneOfEachPrivates.class);
				setField("l", l, OneOfEachPrivates.class);
				setField("f", f, OneOfEachPrivates.class);
				setField("d", d, OneOfEachPrivates.class);
				setField("t", t, OneOfEachPrivates.class);
			}
		};
		// execute
		ExtendsOneOfEachPrivates instance = (ExtendsOneOfEachPrivates) builder.build();
		// assert
		Assert.assertEquals(c, ((Character) getPrivateFieldValue("c", instance, OneOfEachPrivates.class)).charValue());
		Assert.assertEquals(b, ((Byte) getPrivateFieldValue("b", instance, OneOfEachPrivates.class)).byteValue());
		Assert.assertEquals(s, ((Short) getPrivateFieldValue("s", instance, OneOfEachPrivates.class)).shortValue());
		Assert.assertEquals(i, ((Integer) getPrivateFieldValue("i", instance, OneOfEachPrivates.class)).intValue());
		Assert.assertEquals(l, ((Long) getPrivateFieldValue("l", instance, OneOfEachPrivates.class)).longValue());
		Assert.assertEquals(f, ((Float) getPrivateFieldValue("f", instance, OneOfEachPrivates.class)).floatValue(), 0.001);
		Assert.assertEquals(d, ((Double) getPrivateFieldValue("d", instance, OneOfEachPrivates.class)).doubleValue(), 0.001);
		Assert.assertEquals(t, ((Boolean) getPrivateFieldValue("t", instance, OneOfEachPrivates.class)).booleanValue());
	}

	/**
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 */
	public void testConfirmSetMethodOrderIsMaintained() throws IllegalAccessException, NoSuchFieldException {
		// setup
		final char c = 'a';
		final byte b = 1;
		final short s = 2;
		final int i = 3;
		final long l = 4;
		final float f = 12.01f;
		final double d = 42.3;
		final boolean t = true;
		final List assemblyInstructions = new ArrayList();
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new SetFieldInstruction("c", c));
				assemblyInstructions.add(new SetFieldInstruction("b", b));
				assemblyInstructions.add(new SetFieldInstruction("s", s));
				assemblyInstructions.add(new SetFieldInstruction("i", i));
				assemblyInstructions.add(new SetFieldInstruction("l", l));
				assemblyInstructions.add(new SetFieldInstruction("f", f));
				assemblyInstructions.add(new SetFieldInstruction("d", d));
				assemblyInstructions.add(new SetFieldInstruction("t", t));
				bypassConstructor();
				setField("clazz", OneOfEach.class);
				setField("assemblyInstructions", assemblyInstructions);
				// force the order of all those sets to be screwby
				Collections.shuffle(assemblyInstructions);
			}
		};
		// execute
		((EasyBuilder) builder.build()).build();
		// assert
		boolean inOrder = true;
		int lastId = -1;
		for (int idx = 0; inOrder && idx < assemblyInstructions.size(); idx++) {
			EasyBuilder.AssemblyInstruction instruction = (EasyBuilder.AssemblyInstruction) assemblyInstructions.get(idx);
			Integer seqNum = (Integer) getPrivateFieldValue("sequenceId", instruction, EasyBuilder.BaseInstruction.class);
			inOrder = seqNum.intValue() > lastId;
			lastId = seqNum.intValue();
		}
		Assert.assertTrue(inOrder);
	}

	public void testConfirmMethodInvokeOrderIsMaintained() throws IllegalAccessException, NoSuchFieldException {
		// setup
		final List assemblyInstructions = new ArrayList();
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new MethodInvocationInstruction("privateMethod", null));
				assemblyInstructions.add(new MethodInvocationInstruction("protectedMethod", null));
				assemblyInstructions.add(new MethodInvocationInstruction("publicMethod", null));
				bypassConstructor();
				setField("clazz", PMethods.class);
				setField("assemblyInstructions", assemblyInstructions);
				// force the order of all those sets to be screwby
				Collections.shuffle(assemblyInstructions);
			}
		};
		// execute
		((EasyBuilder) builder.build()).build();
		// assert
		boolean inOrder = true;
		int lastId = -1;
		for (int idx = 0; inOrder && idx < assemblyInstructions.size(); idx++) {
			EasyBuilder.AssemblyInstruction instruction = (EasyBuilder.AssemblyInstruction) assemblyInstructions.get(idx);
			Integer seqNum = (Integer) getPrivateFieldValue("sequenceId", instruction, EasyBuilder.BaseInstruction.class);
			inOrder = seqNum.intValue() > lastId;
			lastId = seqNum.intValue();
		}
		Assert.assertTrue(inOrder);
	}

	public void testConfirmSetsAndMethodInvoksOrderIsMaintained() throws IllegalAccessException, NoSuchFieldException {
		// MethodsAndMembers
		// setup
		final char c = 'a';
		final byte b = 1;
		final short s = 2;
		final int i = 3;
		final long l = 4;
		final float f = 12.01f;
		final double d = 42.3;
		final boolean t = true;
		final List assemblyInstructions = new ArrayList();
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new SetFieldInstruction("c", c));
				assemblyInstructions.add(new SetFieldInstruction("b", b));
				assemblyInstructions.add(new SetFieldInstruction("s", s));
				assemblyInstructions.add(new SetFieldInstruction("i", i));
				assemblyInstructions.add(new SetFieldInstruction("l", l));
				assemblyInstructions.add(new SetFieldInstruction("f", f));
				assemblyInstructions.add(new SetFieldInstruction("d", d));
				assemblyInstructions.add(new SetFieldInstruction("t", t));
				assemblyInstructions.add(new MethodInvocationInstruction("privateMethod", null));
				assemblyInstructions.add(new MethodInvocationInstruction("protectedMethod", null));
				assemblyInstructions.add(new MethodInvocationInstruction("publicMethod", null));
				bypassConstructor();
				setField("clazz", MethodsAndMembers.class);
				setField("assemblyInstructions", assemblyInstructions);
				// force the order of all those sets to be screwby
				Collections.shuffle(assemblyInstructions);
			}
		};
		// execute
		((EasyBuilder) builder.build()).build();
		// assert
		boolean inOrder = true;
		int lastId = -1;
		for (int idx = 0; inOrder && idx < assemblyInstructions.size(); idx++) {
			EasyBuilder.AssemblyInstruction instruction = (EasyBuilder.AssemblyInstruction) assemblyInstructions.get(idx);
			Integer seqNum = (Integer) getPrivateFieldValue("sequenceId", instruction, EasyBuilder.BaseInstruction.class);
			inOrder = seqNum.intValue() > lastId;
			lastId = seqNum.intValue();
		}
		Assert.assertTrue(inOrder);
	}

	public void testMethodInvokeCausesException() {
		// setup
		EasyBuilder builder = new EasyBuilder(ExeceptionalExecution.class) {
			{
				invokeMethod("method", null);
			}
		};
		// execute
		try {
			builder.build();
			Assert.fail("should have gotten a runtime exception");
		} catch (RuntimeException re) {
			// assert
			Assert.assertEquals("", re.getMessage());
			Assert.assertNotNull(re.getCause());
		}
	}

	public void testInvokingNonExistentMethodsIsHarmless() {
		// setup
		EasyBuilder builder = new EasyBuilder(Parent.class) {
			{
				bypassConstructor();
				invokeMethod("nonExistentMethod", null);
			}
		};
		// execute
		try {
			builder.build();
		} catch (Throwable t) {
			// assert
			Assert.fail("no exception should be thrown if the named method does not exist");
		}
	}

}

//
// Test classes
// Please note, coverage reporting below this point is meaningless since much of
// the code is loaded by reflection
//
class InstanceCounter {
	static int count = 0;

	// call this the first time the class is used in a test context.
	static void reset() {
		count = 0;
	}

	int id = ++count;
	boolean constructed = false;

	InstanceCounter() {
		constructed = true;
	}
}

class Parent {
	public String parentName;

	public Parent(String name) {
		this.parentName = name;
	}
}

class Child extends Parent {
	public String childName;

	public Child(String childName, String parentName) {
		super(parentName);
		this.childName = childName;
	}
}

class OneOfEach {
	char c;
	byte b;
	short s;
	int i;
	long l;
	float f;
	double d;
	boolean t;
}

class OneOfEachPlus extends OneOfEach {
	Object obj = null;
}

class Dates {
	Calendar cal;
	Date date;
}

class PMethods {
	boolean privateCalled = false;
	boolean protectedCalled = false;
	boolean packageCalled = false;
	boolean publicCalled = false;
	boolean finalCalled = false;
	static boolean staticCalled = false;
	boolean returnCalled = false;

	private void privateMethod() {
		privateCalled = true;
	}

	protected void protectedMethod() {
		protectedCalled = true;
	}

	void packageMethod() {
		packageCalled = true;
	}

	public void publicMethod() {
		publicCalled = true;
	}

	final protected void finalMethod() {
		finalCalled = true;
	}

	static private void staticMethod() {
		staticCalled = true;
	}

	private Object returnMethod() {
		returnCalled = true;
		return new Object();
	}
}

class ExtensionOfPMethods extends PMethods {
}

class MethodsAndMembers extends PMethods {
	char c;
	byte b;
	short s;
	int i;
	long l;
	float f;
	double d;
	boolean t;
}

class AltConstructor {
	boolean defaultWasCalled = false;
	boolean altWasCalled = false;
	Object obj = null;

	public AltConstructor() {
		defaultWasCalled = true;
	}

	public AltConstructor(Object obj_p) {
		altWasCalled = true;
		obj = obj_p;
	}

}

class ExtendsAltConstructor extends AltConstructor {
	boolean substance = false;
}

class SuperType {
	private Object x;
}

class ExtendsSuperType extends SuperType {
	private Object x;
}

class NoDefConst {
	boolean primaryConstCalled = false;

	public NoDefConst(Object o) {
		primaryConstCalled = true;
	}
}

class SomeNonJavaBean {

	private String param1;
	private int param2;
	private SomeParameter someParameter;

	public SomeNonJavaBean() {
		throw new RuntimeException("Should not be invoked");
	}

	public SomeNonJavaBean(String param1, int param2) {
		this.param1 = param1;
		this.param2 = param2;
		this.someParameter = null;
	}

	public SomeNonJavaBean(String param1, int param2, SomeParameter someParameter) {
		this.param1 = param1;
		this.param2 = param2;
		this.someParameter = someParameter;
	}

	public SomeParameter getSomeParameter() {
		return someParameter;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		SomeNonJavaBean another = (SomeNonJavaBean) obj;
		return this.param1.equals(another.param1) && this.param2 == another.param2;
	}

}

class SomeParameter {

	protected Date someDate;
	public double money;

	public SomeParameter(Date someDate, double money) {
		this.someDate = someDate;
		this.money = money;
	}
}

class ConstructorCalledClass {
	boolean constructorCalled = false;

	public ConstructorCalledClass() {
		constructorCalled = true;
	}
}

class OneOfEachPrivates {
	private char c;
	private byte b;
	private short s;
	private int i;
	private long l;
	private float f;
	private double d;
	private boolean t;
}

class ExtendsOneOfEachPrivates extends OneOfEachPrivates {
}

class ExeceptionalExecution {
	private void method() {
		throw new RuntimeException();
	}
}

