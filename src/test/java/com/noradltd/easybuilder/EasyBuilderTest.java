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

import static com.noradltd.easybuilder.FloatCloseToMatcher.closeTo;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.noradltd.easybuilder.EasyBuilder.AssemblyInstruction;
import com.noradltd.easybuilder.EasyBuilder.InstantiateInstruction;

/**
 * @author Nilanjan Raychaundhrui, nraychaudhrui@pillartechnology.com
 * @author Rich Dammkoehler, rdammkoehler@gmail.com (NOrad Ltd.)
 */
public class EasyBuilderTest {

	@Test
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
		assertThat(nonBean, is(anotherNonBean));
	}

	@Test
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

		assertThat(nonBean, is(anotherNonBean));
		assertThat(someParameter.someDate, is(anotherNonBean.getSomeParameter().someDate));
	}

	@Test
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
		assertThat(c.childName, is(anotherChild.childName));
		assertThat(c.parentName, is(anotherChild.parentName));
	}

	@Test
	public void testConstructorShouldBeCalledByDefault() {

		EasyBuilder builder = new EasyBuilder(ConstructorCalledClass.class);

		ConstructorCalledClass testClass = (ConstructorCalledClass) builder.build();

		assertThat(testClass.constructorCalled, is(true));
	}

	@Test
	public void testConstructorShouldBeCalledAndThrowsException() {

		EasyBuilder builder = new EasyBuilder(SomeNonJavaBean.class);

		try {
			builder.build();

			fail("Should have received an exception from the constructor.");
		} catch (RuntimeException re) {
			// OK
		}
	}

	@Test
	public void testConstructorByPassedWhenRequested() {

		EasyBuilder builder = new EasyBuilder(ConstructorCalledClass.class) {
			{
				bypassConstructor();
			}
		};

		ConstructorCalledClass testClass = (ConstructorCalledClass) builder.build();

		assertThat(testClass.constructorCalled, is(false));
	}

	@Test
	public void testNullInitializationClass_ReceiveExceptionFromUnknownClassByNullPointer() {

		EasyBuilder builder = new EasyBuilder(null);

		try {
			builder.build();

			fail("Should have received an exception");
		} catch (RuntimeException re) {
			// OK
		}
	}

	@Test
	public void testFieldNotFoundGeneratesException() {

		EasyBuilder builder = new EasyBuilder(SomeNonJavaBean.class) {
			{
				bypassConstructor();
				setField("nonExistentField", "anyvaluewilldo");
			}
		};

		try {
			builder.build();

			fail("Should have received an exception");
		} catch (RuntimeException re) {
			// OK
		}
	}

	@Test
	public void testBuilderBuilderBuilds() {
		EasyBuilder builder = (EasyBuilder) new EasyBuilder(EasyBuilder.class) {
			{
				bypassConstructor();
			}
		}.build();

		assertThat(builder, is(notNullValue()));
		try {
			builder.build();
			fail("The builder is uninitialized and therefore should fail to build with a NullPointerException");
		} catch (NullPointerException npe) {
			// OK
		}

	}

	@Test
	public void testMultipleInstantiateIntructionsResultInSingleInstantiation() {

		InstanceCounter.reset();
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				List<AssemblyInstruction> assemblyInstructions = new ArrayList<AssemblyInstruction>();
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new BasicInstantiateInstruction());

				bypassConstructor();
				setField("clazz", InstanceCounter.class);
				setField("assemblyInstructions", assemblyInstructions);
			}
		};

		InstanceCounter instance = (InstanceCounter) ((EasyBuilder) builder.build()).build();

		assertThat("Instance Id should be 1", instance.id, is(1));
		assertThat("Instance Count should be 1", InstanceCounter.count, is(1));
		assertThat("Instance should have been constructed", instance.constructed, is(true));
	}

	@Test
	public void testNonAssemblyInstructionAtBeginingIsResortedBehindAssemblyInstructions() throws NoSuchFieldException,
			IllegalAccessException {

		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				List<AssemblyInstruction> assemblyInstructions = new ArrayList<AssemblyInstruction>();
				assemblyInstructions.add(new SetFieldInstruction("childName", "Xanadu"));

				bypassConstructor();
				setField("clazz", Child.class);
				setField("assemblyInstructions", assemblyInstructions);
			}
		};

		EasyBuilder result = (EasyBuilder) builder.build();
		result.bypassConstructor();

		@SuppressWarnings("unchecked")
		List<AssemblyInstruction> assemblyInstructions = (List<AssemblyInstruction>) getPrivateFieldValue(
				"assemblyInstructions", result, EasyBuilder.class);
		assertThat(assemblyInstructions.get(0) instanceof EasyBuilder.InstantiateInstruction, is(true));
		EasyBuilder.InstantiateInstruction instantiateInstruction = (InstantiateInstruction) assemblyInstructions
				.get(0);
		assertThat(instantiateInstruction instanceof EasyBuilder.BypassingInstantiateInstruction, is(true));
	}

	@Test
	public void testMultipleInstantiateIntructionsWithByPassResultsInSingleBypassingInstantiation() {

		InstanceCounter.reset();
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				List<AssemblyInstruction> assemblyInstructions = new ArrayList<AssemblyInstruction>();
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new BasicInstantiateInstruction());
				assemblyInstructions.add(new BypassingInstantiateInstruction());

				bypassConstructor();
				setField("clazz", InstanceCounter.class);
				setField("assemblyInstructions", assemblyInstructions);
			}
		};

		InstanceCounter instance = (InstanceCounter) ((EasyBuilder) builder.build()).build();

		assertThat("Instance Id should be 0", instance.id, is(0));
		assertThat("Instance Count should be 0", InstanceCounter.count, is(0));
		assertThat("Instance should not have been constructed", instance.constructed, is(false));
	}

	@Test
	public void testUnorderedIntructionsResultsInInstantiationFirst() {

		InstanceCounter.reset();
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				List<AssemblyInstruction> assemblyInstructions = new ArrayList<AssemblyInstruction>();
				assemblyInstructions.add(new SetFieldInstruction("id", 6));
				assemblyInstructions.add(new SetFieldInstruction("id", 4));
				assemblyInstructions.add(new BypassingInstantiateInstruction());
				assemblyInstructions.add(new SetFieldInstruction("id", 2));
				bypassConstructor();
				setField("clazz", InstanceCounter.class);
				setField("assemblyInstructions", assemblyInstructions);
			}
		};

		InstanceCounter instance = (InstanceCounter) ((EasyBuilder) builder.build()).build();

		assertThat("Instance Id should be 2", instance.id, is(2));
		assertThat("Instance Count should be 0", InstanceCounter.count, is(0));
		assertThat("Instance should not have been constructed", instance.constructed, is(false));
	}

	@Test
	public void testUnInitializedBuilderIsSafe() {

		InstanceCounter.reset();
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				List<AssemblyInstruction> assemblyInstructions = new ArrayList<AssemblyInstruction>();
				bypassConstructor();
				setField("clazz", InstanceCounter.class);
				setField("assemblyInstructions", assemblyInstructions);
			}
		};

		InstanceCounter instance = (InstanceCounter) ((EasyBuilder) builder.build()).build();

		assertThat("Instance Id should be 1", instance.id, is(1));
		assertThat("Instance Count should be 1", InstanceCounter.count, is(1));
		assertThat("Instance should have been constructed", instance.constructed, is(true));
	}

	@Test
	public void testMultipleInstantiateIntructionsWithAllByPassResultsInSingleBypassingInstantiation() {

		InstanceCounter.reset();
		EasyBuilder builder = new EasyBuilder(EasyBuilder.class) {
			{
				List<AssemblyInstruction> assemblyInstructions = new ArrayList<AssemblyInstruction>();
				assemblyInstructions.add(new BypassingInstantiateInstruction());
				assemblyInstructions.add(new BypassingInstantiateInstruction());

				bypassConstructor();
				setField("clazz", InstanceCounter.class);
				setField("assemblyInstructions", assemblyInstructions);
			}
		};

		InstanceCounter instance = (InstanceCounter) ((EasyBuilder) builder.build()).build();

		assertThat("Instance Id should be 0", instance.id, is(0));
		assertThat("Instance Count should be 0", InstanceCounter.count, is(0));
		assertThat("Instance should not have been constructed", instance.constructed, is(false));
	}

	@Test
	public void testBuildeAClassWithOneOfEachPrimitive() {

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

		OneOfEach instance = (OneOfEach) builder.build();

		assertThat(instance.c, is(c));
		assertThat(instance.b, is(b));
		assertThat(instance.s, is(s));
		assertThat(instance.i, is(i));
		assertThat(instance.l, is(l));
		assertThat(instance.f, is(closeTo(f, 0.001F)));
		assertThat(instance.d, is(closeTo(d, 0.001)));
		assertThat(instance.t, is(t));
	}

	@Test
	public void testSetWithMap() {

		Object obj = new Object();
		final Map<String, Object> map = new HashMap<String, Object>();
		map.put("obj", obj);

		EasyBuilder builder = new EasyBuilder(OneOfEachPlus.class) {
			{
				bypassConstructor();
				setFields(map);
			}
		};

		OneOfEachPlus instance = (OneOfEachPlus) builder.build();

		assertThat(instance.obj, is(obj));
	}

	/**
	 * @noinspection 
	 *               UnnecessaryBoxing,CachedNumberConstructorCall,BooleanConstructorCall
	 */
	@Test
	public void testSetWithMapWithPrimitives() {

		char c = 'a';
		byte b = 1;
		short s = 2;
		int i = 3;
		long l = 4;
		float f = 12.01f;
		double d = 42.3;
		boolean t = true;
		Object obj = new Object();
		final Map<String, Object> map = new HashMap<String, Object>();
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

		OneOfEachPlus instance = (OneOfEachPlus) builder.build();

		assertThat(instance.c, is(c));
		assertThat(instance.b, is(b));
		assertThat(instance.s, is(s));
		assertThat(instance.i, is(i));
		assertThat(instance.l, is(l));
		assertThat(instance.f, is(closeTo(f, 0.001F)));
		assertThat(instance.d, is(closeTo(d, 0.001)));
		assertThat(instance.t, is(t));
		assertThat(instance.obj, is(obj));
	}

	// just to show that it can be done
	@Test
	public void testDateSets() {

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

		Dates instance = (Dates) builder.build();

		assertThat(instance.cal, is(cal));
		assertThat(instance.date, is(date));
	}

	@Test
	public void testPrivateMethodCalled() {

		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("privateMethod", null);
			}
		};

		PMethods instance = (PMethods) builder.build();

		assertThat(instance.privateCalled, is(true));
	}

	@Test
	public void testProtectedMethodCalled() {

		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("protectedMethod", null);
			}
		};

		PMethods instance = (PMethods) builder.build();

		assertThat(instance.protectedCalled, is(true));
	}

	@Test
	public void testPackageMethodCalled() {

		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("packageMethod", null);
			}
		};

		PMethods instance = (PMethods) builder.build();

		assertThat(instance.packageCalled, is(true));
	}

	@Test
	public void testPublicMethodCalled() {

		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("publicMethod", null);
			}
		};

		PMethods instance = (PMethods) builder.build();

		assertThat(instance.publicCalled, is(true));
	}

	@Test
	public void testFinalMethodCalled() {

		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("finalMethod", null);
			}
		};

		PMethods instance = (PMethods) builder.build();

		assertThat(instance.finalCalled, is(true));
	}

	@Test
	public void testStaticMethodCalled() {

		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("staticMethod", null);
			}
		};

		builder.build();

		assertThat(PMethods.staticCalled, is(true));
	}

	@Test
	public void testReturnMethodCalled() {

		EasyBuilder builder = new EasyBuilder(PMethods.class) {
			{
				bypassConstructor();
				invokeMethod("returnMethod", null);
			}
		};

		PMethods instance = (PMethods) builder.build();

		assertThat(instance.returnCalled, is(true));
	}

	@Test
	public void testNoDefaultConstructorClassWithoutBypassThrowsException() {

		EasyBuilder builder = new EasyBuilder(NoDefConst.class);

		try {
			builder.build();

			fail("Expected InstantiationExcpetion");
		} catch (Exception e) {
			assertThat(e.getCause() instanceof InstantiationException, is(true));
		}
	}

	@Test
	public void testNoDefaultConstructorClassWithBypassDoesNotThrowException() {

		EasyBuilder builder = new EasyBuilder(NoDefConst.class) {
			{
				bypassConstructor();
			}
		};

		NoDefConst instance = (NoDefConst) builder.build();

		assertThat(instance.primaryConstCalled, is(false));
	}

	@Test
	public void testPrivateMethodCalledWhenExtended() {

		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("privateMethod", null);
			}
		};

		ExtensionOfPMethods instance = (ExtensionOfPMethods) builder.build();

		assertThat(instance.privateCalled, is(true));
	}

	@Test
	public void testProtectedMethodCalledWhenExtended() {

		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("protectedMethod", null);
			}
		};

		ExtensionOfPMethods instance = (ExtensionOfPMethods) builder.build();

		assertThat(instance.protectedCalled, is(true));
	}

	@Test
	public void testPackageMethodCalledWhenExtended() {

		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("packageMethod", null);
			}
		};

		ExtensionOfPMethods instance = (ExtensionOfPMethods) builder.build();

		assertThat(instance.packageCalled, is(true));
	}

	@Test
	public void testPublicMethodCalledWhenExtended() {

		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("publicMethod", null);
			}
		};

		ExtensionOfPMethods instance = (ExtensionOfPMethods) builder.build();

		assertThat(instance.publicCalled, is(true));
	}

	@Test
	public void testFinalMethodCalledWhenExtended() {

		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("finalMethod", null);
			}
		};

		ExtensionOfPMethods instance = (ExtensionOfPMethods) builder.build();

		assertThat(instance.finalCalled, is(true));
	}

	@Test
	public void testStaticMethodCalledWhenExtended() {

		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("staticMethod", null);
			}
		};

		builder.build();

		assertThat(PMethods.staticCalled, is(true));
	}

	@Test
	public void testReturnMethodCalledWhenExtended() {

		EasyBuilder builder = new EasyBuilder(ExtensionOfPMethods.class) {
			{
				bypassConstructor();
				invokeMethod("returnMethod", null);
			}
		};

		ExtensionOfPMethods instance = (ExtensionOfPMethods) builder.build();

		assertThat(instance.returnCalled, is(true));
	}

	@Test
	public void testUseAlternateConstructor() {

		final Object[] args = new Object[] { new Object() };
		EasyBuilder builder = new EasyBuilder(AltConstructor.class) {
			{
				useAlternateConstructor(args);
			}
		};

		AltConstructor instance = (AltConstructor) builder.build();

		assertThat(instance.defaultWasCalled, is(false));
		assertThat(instance.altWasCalled, is(true));
		assertThat(instance.obj, is(sameInstance(args[0])));
	}

	@Test
	public void testUseAlternateConstructorThroughExtensionClassReturnsSuperType() {

		final Object[] args = new Object[] { new Object() };
		EasyBuilder builder = new EasyBuilder(ExtendsAltConstructor.class) {
			{
				useAlternateConstructor(args);
			}
		};

		Object obj = builder.build();

		assertThat("unexpected type " + obj.getClass().getName(), obj instanceof AltConstructor, is(true));
		AltConstructor instance = (AltConstructor) obj;
		assertThat(instance.defaultWasCalled, is(false));
		assertThat(instance.altWasCalled, is(true));
		assertThat(instance.obj, is(sameInstance(args[0])));
		// no need to assert that the substance field isn't there, we know that
		// by type.
	}

	private Object getPrivateFieldValue(String fieldName, Object targetObject, Class<?> clazz)
			throws NoSuchFieldException, IllegalAccessException {
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(targetObject);
	}

	@Test
	public void testFieldMaskingShouldSetChildTypesField() throws IllegalAccessException, NoSuchFieldException {

		final Object obj = new Object();
		EasyBuilder builder = new EasyBuilder(ExtendsSuperType.class) {
			{
				bypassConstructor();
				setField("x", obj);

			}
		};

		ExtendsSuperType instance = (ExtendsSuperType) builder.build();

		assertThat(getPrivateFieldValue("x", instance, ExtendsSuperType.class), is(sameInstance(obj)));
		assertThat(getPrivateFieldValue("x", instance, SuperType.class), is(nullValue()));
	}

	@Test
	public void testSetPrivateFieldValueBypassingFieldMaskingShouldSetParentTypesField() throws IllegalAccessException,
			NoSuchFieldException {

		final Object obj = new Object();
		EasyBuilder builder = new EasyBuilder(ExtendsSuperType.class) {
			{
				bypassConstructor();
				setField("x", obj, SuperType.class);
			}
		};

		ExtendsSuperType instance = (ExtendsSuperType) builder.build();

		assertThat(getPrivateFieldValue("x", instance, SuperType.class), is(sameInstance(obj)));
		assertThat(getPrivateFieldValue("x", instance, ExtendsSuperType.class), is(nullValue()));
	}

	@Test
	public void testSetPrivatePrimitiveFieldValue() throws NoSuchFieldException, IllegalAccessException {

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

		OneOfEachPrivates instance = (OneOfEachPrivates) builder.build();

		assertThat(((Character) getPrivateFieldValue("c", instance, OneOfEachPrivates.class)).charValue(), is(c));
		assertThat(((Byte) getPrivateFieldValue("b", instance, OneOfEachPrivates.class)).byteValue(), is(b));
		assertThat(((Short) getPrivateFieldValue("s", instance, OneOfEachPrivates.class)).shortValue(), is(s));
		assertThat(((Integer) getPrivateFieldValue("i", instance, OneOfEachPrivates.class)).intValue(), is(i));
		assertThat(((Long) getPrivateFieldValue("l", instance, OneOfEachPrivates.class)).longValue(), is(l));
		assertThat(((Float) getPrivateFieldValue("f", instance, OneOfEachPrivates.class)).floatValue(),
				is(closeTo(f, 0.001F)));
		assertThat(((Double) getPrivateFieldValue("d", instance, OneOfEachPrivates.class)).doubleValue(),
				is(closeTo(d, 0.001)));
		assertThat(((Boolean) getPrivateFieldValue("t", instance, OneOfEachPrivates.class)).booleanValue(), is(t));
	}

	@Test
	public void testSetParentClassPrivatePrimitiveFieldValue() throws NoSuchFieldException, IllegalAccessException {
		// ExtendsOneOfEachPrivates

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

		ExtendsOneOfEachPrivates instance = (ExtendsOneOfEachPrivates) builder.build();

		assertThat(((Character) getPrivateFieldValue("c", instance, OneOfEachPrivates.class)).charValue(), is(c));
		assertThat(((Byte) getPrivateFieldValue("b", instance, OneOfEachPrivates.class)).byteValue(), is(b));
		assertThat(((Short) getPrivateFieldValue("s", instance, OneOfEachPrivates.class)).shortValue(), is(s));
		assertThat(((Integer) getPrivateFieldValue("i", instance, OneOfEachPrivates.class)).intValue(), is(i));
		assertThat(((Long) getPrivateFieldValue("l", instance, OneOfEachPrivates.class)).longValue(), is(l));
		assertThat(((Float) getPrivateFieldValue("f", instance, OneOfEachPrivates.class)).floatValue(),
				is(closeTo(f, 0.001F)));
		assertThat(((Double) getPrivateFieldValue("d", instance, OneOfEachPrivates.class)).doubleValue(),
				is(closeTo(d, 0.001)));
		assertThat(((Boolean) getPrivateFieldValue("t", instance, OneOfEachPrivates.class)).booleanValue(), is(t));
	}

	/**
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 */
	@Test
	public void testConfirmSetMethodOrderIsMaintained() throws IllegalAccessException, NoSuchFieldException {

		final char c = 'a';
		final byte b = 1;
		final short s = 2;
		final int i = 3;
		final long l = 4;
		final float f = 12.01f;
		final double d = 42.3;
		final boolean t = true;
		final List<AssemblyInstruction> assemblyInstructions = new ArrayList<AssemblyInstruction>();
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

		((EasyBuilder) builder.build()).build();

		boolean inOrder = true;
		int lastId = -1;
		for (int idx = 0; inOrder && idx < assemblyInstructions.size(); idx++) {
			EasyBuilder.AssemblyInstruction instruction = (EasyBuilder.AssemblyInstruction) assemblyInstructions
					.get(idx);
			Integer seqNum = (Integer) getPrivateFieldValue("sequenceId", instruction,
					EasyBuilder.BaseInstruction.class);
			inOrder = seqNum.intValue() > lastId;
			lastId = seqNum.intValue();
		}
		assertThat(inOrder, is(true));
	}

	@Test
	public void testConfirmMethodInvokeOrderIsMaintained() throws IllegalAccessException, NoSuchFieldException {

		final List<AssemblyInstruction> assemblyInstructions = new ArrayList<AssemblyInstruction>();
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

		((EasyBuilder) builder.build()).build();

		boolean inOrder = true;
		int lastId = -1;
		for (int idx = 0; inOrder && idx < assemblyInstructions.size(); idx++) {
			EasyBuilder.AssemblyInstruction instruction = (EasyBuilder.AssemblyInstruction) assemblyInstructions
					.get(idx);
			Integer seqNum = (Integer) getPrivateFieldValue("sequenceId", instruction,
					EasyBuilder.BaseInstruction.class);
			inOrder = seqNum.intValue() > lastId;
			lastId = seqNum.intValue();
		}
		assertThat(inOrder, is(true));
	}

	@Test
	public void testConfirmSetsAndMethodInvoksOrderIsMaintained() throws IllegalAccessException, NoSuchFieldException {
		// MethodsAndMembers

		final char c = 'a';
		final byte b = 1;
		final short s = 2;
		final int i = 3;
		final long l = 4;
		final float f = 12.01f;
		final double d = 42.3;
		final boolean t = true;
		final List<AssemblyInstruction> assemblyInstructions = new ArrayList<AssemblyInstruction>();
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

		((EasyBuilder) builder.build()).build();

		boolean inOrder = true;
		int lastId = -1;
		for (int idx = 0; inOrder && idx < assemblyInstructions.size(); idx++) {
			EasyBuilder.AssemblyInstruction instruction = (EasyBuilder.AssemblyInstruction) assemblyInstructions
					.get(idx);
			Integer seqNum = (Integer) getPrivateFieldValue("sequenceId", instruction,
					EasyBuilder.BaseInstruction.class);
			inOrder = seqNum.intValue() > lastId;
			lastId = seqNum.intValue();
		}
		assertThat(inOrder, is(true));
	}

	@Test
	public void testMethodInvokeCausesException() {

		EasyBuilder builder = new EasyBuilder(ExeceptionalExecution.class) {
			{
				invokeMethod("method", null);
			}
		};

		try {
			builder.build();
			fail("should have gotten a runtime exception");
		} catch (RuntimeException re) {

			assertThat(re.getMessage(), isEmptyString());
			assertThat(re.getCause(), is(notNullValue()));
		}
	}

	@Test
	public void testInvokingNonExistentMethodsIsHarmless() {

		EasyBuilder builder = new EasyBuilder(Parent.class) {
			{
				bypassConstructor();
				invokeMethod("nonExistentMethod", null);
			}
		};

		try {
			builder.build();
		} catch (Throwable t) {

			fail("no exception should be thrown if the named method does not exist");
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

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
	static private void staticMethod() {
		staticCalled = true;
	}

	@SuppressWarnings("unused")
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
	@SuppressWarnings("unused")
	private Object x;
}

class ExtendsSuperType extends SuperType {
	@SuppressWarnings("unused")
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
	@SuppressWarnings("unused")
	private char c;
	@SuppressWarnings("unused")
	private byte b;
	@SuppressWarnings("unused")
	private short s;
	@SuppressWarnings("unused")
	private int i;
	@SuppressWarnings("unused")
	private long l;
	@SuppressWarnings("unused")
	private float f;
	@SuppressWarnings("unused")
	private double d;
	@SuppressWarnings("unused")
	private boolean t;
}

class ExtendsOneOfEachPrivates extends OneOfEachPrivates {
}

class ExeceptionalExecution {
	@SuppressWarnings("unused")
	private void method() {
		throw new RuntimeException();
	}
}
