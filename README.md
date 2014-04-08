EasyBuilder
===========

A dynamic builder pattern tool for Java.

README - EasyBuilder 1.0

EasyBuilder is licensed under the MIT License, see LICENSE file.

EasyBuilder is a utility package that allows you to quickly create Builders 
for your applications objects. EasyBuilder has been built and tested with 
JDK 1.4 and JDK 1.5. It relies on the Objenesis 1.1 library. If you have 
Aspect-J (1.5.4) you can also use the TracingAspect to generate logging 
information. Logging is supported through SLF4J 1.5.8 and uses the 
'Simple Logger' by default. 

Installation:
===============================
Add the following jars to your classpath;
-	easybuilder-1.0.jar
-	objenesis-1.1.jar

Optionally you may include
-	aspectjrt-1.5.4.jar
-	slf4j-api-1.5.8.jar
-	slf4j-simple-1.5.8.jar

Usage:
===============================
EasyBuilder establishes a simple DSL for the builder pattern. 

The first step is to create the EasyBuilder instance. To do this, use the
constructor to specify the target type to be built, e.g.

'''java
	new EasyBuilder(SomeClass.class);
'''
	
Once you've created the builder, apply the builder DSL methods to establish the 
assembly instructions for the target class. Once all of the instructions have 
been created, call the build() method to assemble the object. 

The DSL methods are available;
===============================

build() 					Execute all the instructions provided. This should 
							be the last thing you call.
bypassConstructor()			Use Objenesis to skip the class constructor, handy 
							when the constructor has some undesirable 
							side-effects.
setField(<field>,<value>)	Sets the value of a field on the target class
setField(<field>,<value>,<implementing class>)
							Sets the value of a field on the target class based 
							on it's defining class
setFields(<map>)			Sets the values of fields named in the map. The map 
							is keyed by field name, and the values in the map 
							are the values to use.
useAlternateConstructor(Object[])
							Construct the object using some complex constructor 
							that accepts the arguments provided. The arguments 
							should be presented in the same order as the 
							constructor expects them using the specific types 
							of the constructors arguments. EasyBuilder will do 
							the rest.
							
					 
Example Usage:
===============================
Your best examples are in the EasyBuilderTest.java file, however, the simplest 
usage pattern is as follows;

JDK 1.4
===============================
'''java
		EasyBuilder builder = new EasyBuilder(SomeNonJavaBean.class);
		builder.builder.bypassConstructor();
		builder.setField("param1", "someValue");
		builder.setField("param2", 42);
		SomeNonJavaBean anotherNonBean = (SomeNonJavaBean) builder.build();
'''

JDK 1.5
===============================
'''java
		EasyBuilder builder = new EasyBuilder(SomeNonJavaBean.class) {
			{
				bypassConstructor();
				setField("param1", "someValue");
				setField("param2", 42);
			}
		};
		SomeNonJavaBean anotherNonBean = (SomeNonJavaBean) builder.build();
'''
		
Tracing EasyBuilders Internals:
===============================
Using AspectJ you can trace the internal activity of EasyBuilder. 