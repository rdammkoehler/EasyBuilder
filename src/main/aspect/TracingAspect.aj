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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public aspect TracingAspect {

	final Logger logger = LoggerFactory.getLogger(EasyBuilder.class);
	
	void emit(String message) {
		logger.info(message);
	}
	
	pointcut traceAssemblyInstruction(EasyBuilder.AssemblyInstruction instruction): target(instruction) && call(void invoke(EasyBuilder));
	
	before(EasyBuilder.AssemblyInstruction instruction):traceAssemblyInstruction(instruction) {
		emit("Invoking Assembly Instruction " + instruction.toString());
	}
	
	pointcut traceAssembleObject(EasyBuilder builder):target(builder) && call(Object assembleObject());
	
	before(EasyBuilder builder):traceAssembleObject(builder) {
		emit("Assembling Object with " + builder.toString());
	}
	
	pointcut tracePreCompileResult(EasyBuilder builder):target(builder) && call(void preCompile());
	
	before(EasyBuilder builder):tracePreCompileResult(builder) {
		emit("Before PreCompile: AssemblyInstructions " + builder.toString());
	}
	
	after(EasyBuilder builder):tracePreCompileResult(builder) {
		emit("After PreCompile: AssemblyInstructions " + builder.toString());
	}
	
}
