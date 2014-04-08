/*
 The MIT License

Copyright (c) 2009 easybuilder open source development group

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
package easybuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;

import java.util.*;

/**
 * Please note that this initial version of EasyBuilder is intended to function
 * in the Java JDK 1.4 environment.
 * 
 * @author Nilanjan Raychaundhrui, nraychaudhrui@pillartechnology.com
 * @author Rich Dammkoehler, rdammkoehler@gmail.com (NOrad Ltd.)
 * @noinspection ConstantConditions
 */
public class EasyBuilder {

	/**
	 * The target class to be built.
	 */
	private Class clazz;

	/**
	 * The instance of the class being built. This may be removed later, see
	 * wiki discussion on 'Builder Builder'
	 */
	private Object instance;

	/**
	 * A list of instructions used to assemble the an instance of the class
	 * being built.
	 */
	private List assemblyInstructions = new ArrayList();

	/**
	 * Accumulates the number of instances of Instructions, used to preserve
	 * order of set and invoke commands when building occurs.
	 */
	private int instructionCounter = 0;

	/**
	 * Initializes the EasyBuilder using the given class. No further action
	 * occurs.
	 * 
	 * @param clazz_p
	 *            The class to be built.
	 */
	public EasyBuilder(Class clazz_p) {
		clazz = clazz_p;
		addInstruction(new BasicInstantiateInstruction());
	}

	/**
	 * Adds an assembly instruction to the end of the assembly instruction list.
	 * 
	 * @param instruction
	 *            The instruction to add
	 */
	private void addInstruction(AssemblyInstruction instruction) {
		assemblyInstructions.add(instruction);
	}

	/**
	 * Executes all of the assembly instructions in order.
	 * 
	 * @return The assembled object
	 */
	private Object assembleObject() {
		preCompile();
		Iterator itr = assemblyInstructions.iterator();
		while (itr.hasNext()) {
			AssemblyInstruction instruction = (AssemblyInstruction) itr.next();
			instruction.invoke(this);
		}
		return instance;
	}

	/**
	 * Pre-guard the execution of assembleObject() by ensuring that certain
	 * requisit actions have been taken prior to execution. <p/>
	 * <ul>
	 * <li>The first instruction <u>must</u> be an InstantiateInstruction</li>
	 * <li>An instantiate instruction who's bypass value is true takes
	 * precedence over one who's does not</li>
	 * </ul>
	 */
	private void preCompile() {
		// we must have an assemblyInstruction
		if (assemblyInstructions.isEmpty()) {
			addInstruction(new BasicInstantiateInstruction());
		}
		// ensure execution order
		Collections.sort(assemblyInstructions);
		// make sure we only have one InstantiateInstruction
		while (assemblyInstructions.size() > 1 && assemblyInstructions.get(1) instanceof InstantiateInstruction) {
			assemblyInstructions.remove(1);
		}
	}

	/**
	 * Capture the types of each parameter.
	 * 
	 * @param params
	 *            A list of objects
	 * @return If params is null, zero length Class[], otherwise a Class[]
	 *         representing the types of the objects in the array
	 */
	private Class[] getParamTypes(Object[] params) {
		Class[] types;
		if (params == null) {
			types = new Class[0];
		} else {
			types = new Class[params.length];
			for (int idx = 0; idx < params.length; idx++) {
				if (params[idx] != null) {
					types[idx] = params[idx].getClass();
				}
			}
		}
		return types;
	}

	// @Override java.lang.Object
	public String toString() {
		return new StringBuilder("EasyBuilder[").append("target::").append(clazz.getName()).append(", instructions::").append(assemblyInstructions).append("]")
				.toString();
	}

	//
	// Public API
	//

	/**
	 * Causes the builder to bypass the default constructor of the class to be
	 * built.
	 * 
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder bypassConstructor() {
		// guard against duplication
		if (assemblyInstructions.get(0) instanceof InstantiateInstruction) {
			assemblyInstructions.remove(0);
		}
		assemblyInstructions.add(0, new BypassingInstantiateInstruction());
		return this;
	}

	/**
	 * Creates an initialized instance of the class to be built.
	 * 
	 * @return An initialized instance of the class to be built
	 */
	public Object build() {
		return assembleObject();
	}

	/**
	 * Sets the value of a filed directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, Object value) {
		addInstruction(new SetFieldInstruction(fieldName, value));
		return this;
	}

	/**
	 * Sets the value of a private field directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @param clazz
	 *            The impelementing class of the field
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, Object value, Class clazz) {
		addInstruction(new SetPrivateFieldInstruction(fieldName, value, clazz));
		return this;
	}

	/**
	 * Sets the value of a filed directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, char value) {
		addInstruction(new SetFieldInstruction(fieldName, value));
		return this;
	}

	/**
	 * Sets the value of a private field directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @param clazz
	 *            The impelementing class of the field
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, char value, Class clazz) {
		addInstruction(new SetPrivateFieldInstruction(fieldName, value, clazz));
		return this;
	}

	/**
	 * Sets the value of a filed directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, byte value) {
		addInstruction(new SetFieldInstruction(fieldName, value));
		return this;
	}

	/**
	 * Sets the value of a private field directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @param clazz
	 *            The impelementing class of the field
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, byte value, Class clazz) {
		addInstruction(new SetPrivateFieldInstruction(fieldName, value, clazz));
		return this;
	}

	/**
	 * Sets the value of a filed directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, short value) {
		addInstruction(new SetFieldInstruction(fieldName, value));
		return this;
	}

	/**
	 * Sets the value of a private field directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @param clazz
	 *            The impelementing class of the field
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, short value, Class clazz) {
		addInstruction(new SetPrivateFieldInstruction(fieldName, value, clazz));
		return this;
	}

	/**
	 * Sets the value of a filed directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, int value) {
		addInstruction(new SetFieldInstruction(fieldName, value));
		return this;
	}

	/**
	 * Sets the value of a private field directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @param clazz
	 *            The impelementing class of the field
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, int value, Class clazz) {
		addInstruction(new SetPrivateFieldInstruction(fieldName, value, clazz));
		return this;
	}

	/**
	 * Sets the value of a filed directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, long value) {
		addInstruction(new SetFieldInstruction(fieldName, value));
		return this;
	}

	/**
	 * Sets the value of a private field directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @param clazz
	 *            The impelementing class of the field
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, long value, Class clazz) {
		addInstruction(new SetPrivateFieldInstruction(fieldName, value, clazz));
		return this;
	}

	/**
	 * Sets the value of a filed directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, float value) {
		addInstruction(new SetFieldInstruction(fieldName, value));
		return this;
	}

	/**
	 * Sets the value of a private field directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @param clazz
	 *            The impelementing class of the field
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, float value, Class clazz) {
		addInstruction(new SetPrivateFieldInstruction(fieldName, value, clazz));
		return this;
	}

	/**
	 * Sets the value of a filed directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, double value) {
		addInstruction(new SetFieldInstruction(fieldName, value));
		return this;
	}

	/**
	 * Sets the value of a private field directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @param clazz
	 *            The impelementing class of the field
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, double value, Class clazz) {
		addInstruction(new SetPrivateFieldInstruction(fieldName, value, clazz));
		return this;
	}

	/**
	 * Sets the value of a filed directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, boolean value) {
		addInstruction(new SetFieldInstruction(fieldName, value));
		return this;
	}

	/**
	 * Sets the value of a private field directly through the field.
	 * 
	 * @param fieldName
	 *            The complete name of the field to be set
	 * @param value
	 *            The value to set
	 * @param clazz
	 *            The impelementing class of the field
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setField(String fieldName, boolean value, Class clazz) {
		addInstruction(new SetPrivateFieldInstruction(fieldName, value, clazz));
		return this;
	}

	/**
	 * Sets multiple values directly through the field. The order in which the
	 * sets occurs is arbitrary.
	 * 
	 * @param fieldMap
	 *            A map keyed by field name of values to set
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder setFields(Map fieldMap) {
		Iterator itr = fieldMap.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry entry = (Map.Entry) itr.next();
			addInstruction(new SetFieldInstruction((String) entry.getKey(), entry.getValue()));
		}
		return this;
	}

	/**
	 * Invokes method by name. The results are ???
	 * 
	 * @param methodName
	 *            A declared method on this class or one of it's ancestors
	 * @param args
	 *            the arguments to the method
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder invokeMethod(String methodName, Object[] args) {
		addInstruction(new MethodInvocationInstruction(methodName, args));
		return this;
	}

	/**
	 * Uses a constructor other than the default. Invocation of this will be
	 * ignored if you've used bypassConstructor()
	 * 
	 * @param args
	 *            The arguments for the constructor
	 * @return An instance of the EasyBuilder, this allows chained-calls.
	 */
	public EasyBuilder useAlternateConstructor(Object[] args) {
		// guard against duplication
		if (assemblyInstructions.get(0) instanceof InstantiateInstruction) {
			assemblyInstructions.remove(0);
		}
		assemblyInstructions.add(0, new ParameterizedInstantiateInstruction(args));
		return this;
	}

	//
	// Support Classes
	//

	/**
	 * Represents an assembly instruction in the process of building an instance
	 * of the class to be built.
	 */
	interface AssemblyInstruction extends Comparable {
		void invoke(EasyBuilder builder);
	}

	abstract class BaseInstruction implements AssemblyInstruction {
		final protected int sequenceId = ++instructionCounter;

		/**
		 * Uses the sequenceId to determine order
		 * 
		 * @param that
		 * @return zero
		 */
		public int compareTo(Object that) {
			int rval = 0;
			if ( that instanceof InstantiateInstruction) {
				rval = 1;
			} else if (that instanceof BaseInstruction) {
				rval = sequenceId - ((BaseInstruction) that).sequenceId;
			}
			return rval;
		}

	}

	/**
	 * Instantiates the class to be built.
	 */
	abstract class InstantiateInstruction extends BaseInstruction implements AssemblyInstruction {
		//@Override
		public int compareTo(Object that) {
			int rval = 0;
			if (that instanceof InstantiateInstruction) {
				if (that instanceof BypassingInstantiateInstruction) {
					rval = 1;
				} else {
					rval = 0;
				}
			} else {
				rval = -1;
			}
			return rval;
		}

	}

	class BasicInstantiateInstruction extends InstantiateInstruction implements AssemblyInstruction {

		public BasicInstantiateInstruction() {
		}

		/**
		 * Initialize the class.
		 */
		public void invoke(EasyBuilder builder) {
			if (builder.instance == null) {
				try {
					builder.instance = builder.clazz.newInstance();
				} catch (Exception e) {
					throw new RuntimeException(new StringBuffer("Initalization Exception: EasyBuilder failed to instantiate the class ").append(
							(clazz == null) ? "-unknown-" : clazz.getName()).toString(), e);
				}
			}
		}

		// @java.lang.Override
		public String toString() {
			return new StringBuffer("BasicInstantiate[sequenceId::").append(sequenceId).append("]").toString();
		}
	}

	class BypassingInstantiateInstruction extends InstantiateInstruction implements AssemblyInstruction {

		public BypassingInstantiateInstruction() {
		}

		/**
		 * Initialize the class.
		 */
		public void invoke(EasyBuilder builder) {
			if (builder.instance == null) {
				builder.instance = createInstance(builder.clazz);
			}
		}

		/**
		 * Creates an instance of the class using Google's Objenesis library,
		 * this creation bypasses the default constructor of the class.
		 * 
		 * @param clazz
		 *            The class to be instantiated, the class being built
		 * @return An uninitialized instance of the class being built, even the
		 *         constructor was bypassed.
		 */
		private Object createInstance(Class clazz) {
			Objenesis objenesis = new ObjenesisStd();
			ObjectInstantiator thingyInstantiator = objenesis.getInstantiatorOf(clazz);
			return thingyInstantiator.newInstance();
		}

		//@Override
		public int compareTo(Object that) {
			int rval = -1;
			if (that instanceof BypassingInstantiateInstruction) {
				rval = 0;
			}
			return rval;
		}

		// @java.lang.Override
		public String toString() {
			return new StringBuffer("BypassingInstantiateInstruction[sequenceId::").append(sequenceId).append("]").toString();
		}
	}

	class ParameterizedInstantiateInstruction extends InstantiateInstruction implements AssemblyInstruction {

		/**
		 * Arguments to the constructor.
		 */
		Object[] args = null;

		public ParameterizedInstantiateInstruction(Object[] args_p) {
			args = args_p;
		}

		/**
		 * Initialize the class.
		 */
		public void invoke(EasyBuilder builder) {
			if (builder.instance == null) {
				try {
					builder.instance = constructInstanceWithAlternativeConstructor(builder.clazz);
				} catch (Exception e) {
					throw new RuntimeException(new StringBuffer("Initalization Exception: EasyBuilder failed to instantiate the class ").append(
							(clazz == null) ? "-unknown-" : clazz.getName()).toString(), e);
				}
			}
		}

		/**
		 * Instantiates an instance of the class using a constructor who's
		 * arguments match by data type those provided as arguments to this
		 * instruction.
		 * 
		 * @param clazz
		 *            The class to be instantiated, the class being built.
		 * @return An initialized instance of the class being built, an
		 *         alternate constructor is used.
		 * @throws IllegalAccessException
		 * @throws InstantiationException
		 * @throws InvocationTargetException
		 */
		private Object constructInstanceWithAlternativeConstructor(Class clazz) throws IllegalAccessException, InvocationTargetException,
				InstantiationException {
			Class sourceClass = clazz;
			Class[] paramTypes = getParamTypes(args);
			Constructor constructor = null;
			Object instance = null;
			while (constructor == null && !Object.class.equals(sourceClass)) {
				try {
					constructor = sourceClass.getDeclaredConstructor(paramTypes);
				} catch (NoSuchMethodException nsme) {
					sourceClass = clazz.getSuperclass();
				}
			}
			if (constructor != null) {
				instance = constructor.newInstance(args);
			}
			return instance;
		}

		// @java.lang.Override
		public String toString() {
			// TODO add args to this output
			return new StringBuffer("ParameterizedInstantiateInstruction[sequenceId::").append(sequenceId).append("]").toString();
		}
	}

	/**
	 * Sets a field on the class being built.
	 */
	class SetFieldInstruction extends BaseInstruction implements AssemblyInstruction {
		String fieldName = null;
		Object value = null;
		char c;
		byte b;
		short s;
		int i;
		long l;
		float f;
		double d;
		boolean t;
		Class type = null;

		/**
		 * @param fn
		 *            The complete name of the field to be set
		 * @param v
		 *            The value to set
		 */
		public SetFieldInstruction(String fn, Object v) {
			fieldName = fn;
			value = v;
			type = v.getClass();
		}

		//
		// pre-Java 1.5 support
		//

		public SetFieldInstruction(String fn, boolean v) {
			fieldName = fn;
			t = v;
			type = Boolean.TYPE;
		}

		public SetFieldInstruction(String fn, byte v) {
			fieldName = fn;
			b = v;
			type = Byte.TYPE;
		}

		public SetFieldInstruction(String fn, char v) {
			fieldName = fn;
			c = v;
			type = Character.TYPE;
		}

		public SetFieldInstruction(String fn, short v) {
			fieldName = fn;
			s = v;
			type = Short.TYPE;
		}

		public SetFieldInstruction(String fn, int v) {
			fieldName = fn;
			i = v;
			type = Integer.TYPE;
		}

		public SetFieldInstruction(String fn, long v) {
			fieldName = fn;
			l = v;
			type = Long.TYPE;
		}

		public SetFieldInstruction(String fn, float v) {
			fieldName = fn;
			f = v;
			type = Float.TYPE;
		}

		public SetFieldInstruction(String fn, double v) {
			fieldName = fn;
			d = v;
			type = Double.TYPE;
		}

		/**
		 * 
		 */
		public void invoke(EasyBuilder builder) {
			try {
				Field field = findField(builder.clazz, fieldName);
				field.setAccessible(true);
				if (value == null) {
					setPrimitive(field, builder.instance);
				} else {
					field.set(builder.instance, value);
				}
			} catch (Exception ex) {
				throw new RuntimeException("", ex);
			}
		}

		public void setPrimitive(Field field, Object instance) throws IllegalAccessException {
			if (Boolean.TYPE.equals(type)) {
				field.setBoolean(instance, t);
			} else if (Character.TYPE.equals(type)) {
				field.setChar(instance, c);
			} else if (Byte.TYPE.equals(type)) {
				field.setByte(instance, b);
			} else if (Short.TYPE.equals(type)) {
				field.setShort(instance, s);
			} else if (Integer.TYPE.equals(type)) {
				field.setInt(instance, i);
			} else if (Long.TYPE.equals(type)) {
				field.setLong(instance, l);
			} else if (Float.TYPE.equals(type)) {
				field.setFloat(instance, f);
			} else if (Double.TYPE.equals(type)) {
				field.setDouble(instance, d);
			}
		}

		/**
		 * Locate the field that needs to be set by inspecting the class being
		 * built. If not found look to the parent class. Continue back to
		 * <code>java.lang.Object</code>. If not found throw a
		 * NoSuchFieldException to indicate that the field was not located.
		 * 
		 * @param clazz
		 *            The initial class, the one that's being built
		 * @param fieldName
		 *            The name of the field to be found
		 * @return The Field requested by name
		 * @throws NoSuchFieldException
		 *             Field was not found in this class hierarchy.
		 */
		protected Field findField(Class clazz, String fieldName) throws NoSuchFieldException {
			Class clazz_lcl = clazz;
			Field field = null;
			do {
				try {
					field = clazz_lcl.getDeclaredField(fieldName);
				} catch (Exception ignored) {
				}
				clazz_lcl = clazz_lcl.getSuperclass();
			} while (field == null && !clazz_lcl.equals(Object.class));
			if (field == null) {
				throw new NoSuchFieldException(new StringBuffer("Field not found for name ").append(fieldName).toString());
			}
			return field;
		}

		protected String getValueString() {
			String rval = "null";
			if (Boolean.TYPE.equals(type)) {
				rval = Boolean.toString(t);
			} else if (Character.TYPE.equals(type)) {
				rval = Character.toString(c);
			} else if (Byte.TYPE.equals(type)) {
				rval = Byte.toString(b);
			} else if (Short.TYPE.equals(type)) {
				rval = Short.toString(s);
			} else if (Integer.TYPE.equals(type)) {
				rval = Integer.toString(i);
			} else if (Long.TYPE.equals(type)) {
				rval = Long.toString(l);
			} else if (Float.TYPE.equals(type)) {
				rval = Float.toString(f);
			} else if (Double.TYPE.equals(type)) {
				rval = Double.toString(d);
			} else {
				rval = value.toString();
			}
			return rval;
		}

		// @java.lang.Override
		public String toString() {
			return new StringBuffer("setField[fieldName::").append(fieldName).append("(").append(type.getName()).append("), value::").append(getValueString())
					.append(", sequenceId::").append(sequenceId).append("]").toString();
		}
	}

	/**
	 * Invokes a method regardless of it's accessibility. Return values are not
	 * presently supported
	 */
	class MethodInvocationInstruction extends BaseInstruction implements AssemblyInstruction {
		String methodName = null;
		Object[] args = null;

		public MethodInvocationInstruction(String methodName_p, Object[] args_p) {
			methodName = methodName_p;
			args = args_p;
		}

		public void invoke(EasyBuilder builder) {
			Class clazz = builder.clazz;
			Class[] paramTypes = getParamTypes(args);
			Method method = null;
			while (method == null && !Object.class.equals(clazz)) {
				try {
					method = clazz.getDeclaredMethod(methodName, paramTypes);
					method.setAccessible(true);
					method.invoke(builder.instance, args);
					// TODO what shall we do with the result of this execution?
				} catch (NoSuchMethodException nsme) {
					clazz = clazz.getSuperclass();
				} catch (Exception e) {
					throw new RuntimeException("", e);
				}
			}
		}

		// @java.lang.Override
		public String toString() {
			return new StringBuffer("invokeMethod[methodName::").append(methodName).append(", sequenceId::").append(sequenceId).append("]").toString();
		}
	}

	/**
	 * Sets private fields using a specified declaring class allowing you to set
	 * the private field on a parent class that is masked by the target type or
	 * any interviening class in the hierarchy.
	 */
	class SetPrivateFieldInstruction extends SetFieldInstruction {

		Class targetClass = null;

		public SetPrivateFieldInstruction(String fn, Object v, Class startingClass) {
			super(fn, v);
			targetClass = startingClass;
		}

		public SetPrivateFieldInstruction(String fn, boolean v, Class startingClass) {
			super(fn, v);
			targetClass = startingClass;
		}

		public SetPrivateFieldInstruction(String fn, byte v, Class startingClass) {
			super(fn, v);
			targetClass = startingClass;
		}

		public SetPrivateFieldInstruction(String fn, char v, Class startingClass) {
			super(fn, v);
			targetClass = startingClass;
		}

		public SetPrivateFieldInstruction(String fn, short v, Class startingClass) {
			super(fn, v);
			targetClass = startingClass;
		}

		public SetPrivateFieldInstruction(String fn, int v, Class startingClass) {
			super(fn, v);
			targetClass = startingClass;
		}

		public SetPrivateFieldInstruction(String fn, long v, Class startingClass) {
			super(fn, v);
			targetClass = startingClass;
		}

		public SetPrivateFieldInstruction(String fn, float v, Class startingClass) {
			super(fn, v);
			targetClass = startingClass;
		}

		public SetPrivateFieldInstruction(String fn, double v, Class startingClass) {
			super(fn, v);
			targetClass = startingClass;
		}

		protected Field findField(Class clazz, String fieldName) throws NoSuchFieldException {
			return super.findField(targetClass, fieldName);
		}

		// @java.lang.Override
		public String toString() {
			return new StringBuffer("setPrivateField[fieldName::").append(fieldName).append("(").append(type.getName()).append("), value::").append(
					getValueString()).append(", sequenceId::").append(sequenceId).append("]").toString();
		}
	}
}
