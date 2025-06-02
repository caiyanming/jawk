package org.metricshub.jawk.backend;

/*-
 * ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲
 * Jawk
 * ჻჻჻჻჻჻
 * Copyright (C) 2006 - 2025 MetricsHub
 * ჻჻჻჻჻჻
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱
 */

import java.util.Arrays;

import org.metricshub.jawk.jrt.AssocArray;
import org.metricshub.jawk.intermediate.UninitializedObject;
import org.metricshub.jawk.util.ArrayStackImpl;
import org.metricshub.jawk.util.AwkLogger;
import org.metricshub.jawk.util.LinkedListStackImpl;
import org.metricshub.jawk.util.MyStack;
import org.slf4j.Logger;

/**
 * Runtime stack used by the AVM interpreter.
 */
class RuntimeStack {

	private static final Logger LOG = AwkLogger.getLogger(RuntimeStack.class);
	static final UninitializedObject BLANK = new UninitializedObject();

	private Object[] globals = null;
	private Object[] locals = null;
	private MyStack<Object[]> localsStack = new ArrayStackImpl<Object[]>();
	private MyStack<Integer> returnIndexes = new LinkedListStackImpl<Integer>();

	@SuppressWarnings("unused")
	public void dump() {
		LOG.info("globals = {}", Arrays.toString(globals));
		LOG.info("locals = {}", Arrays.toString(locals));
		LOG.info("localsStack = {}", localsStack);
		LOG.info("returnIndexes = {}", returnIndexes);
	}

	Object[] getNumGlobals() {
		return globals;
	}

	/** Must be one of the first methods executed. */
	void setNumGlobals(long l) {
		assert l >= 0;
		assert globals == null;
		globals = new Object[(int) l];
		for (int i = 0; i < l; i++) {
			globals[i] = null;
		}
		// must accept multiple executions
		// expandFrameIfNecessary(num_globals);
	}

	/*
	 * // this assumes globals = Object[0] upon initialization
	 * private void expandFrameIfNecessary(int num_globals) {
	 * if (num_globals == globals.length)
	 * // no need for expansion;
	 * // do nothing
	 * return;
	 * Object[] new_frame = new Object[num_globals];
	 * for (int i=0;i<globals.length;++i)
	 * new_frame[i] = globals[i];
	 * globals = new_frame;
	 * }
	 */

	Object getVariable(long offset, boolean isGlobal) {
		assert globals != null;
		assert offset != AVM.NULL_OFFSET;
		if (isGlobal) {
			return globals[(int) offset];
		} else {
			return locals[(int) offset];
		}
	}

	Object setVariable(long offset, Object val, boolean isGlobal) {
		assert globals != null;
		assert offset != AVM.NULL_OFFSET;
		if (isGlobal) {
			return globals[(int) offset] = val;
		} else {
			return locals[(int) offset] = val;
		}
	}

	// for _DELETE_ARRAY_
	void removeVariable(long offset, boolean isGlobal) {
		assert globals != null;
		assert offset != AVM.NULL_OFFSET;
		if (isGlobal) {
			assert globals[(int) offset] == null || globals[(int) offset] instanceof AssocArray;
			globals[(int) offset] = null;
		} else {
			assert locals[(int) offset] == null || locals[(int) offset] instanceof AssocArray;
			locals[(int) offset] = null;
		}
	}

	void setFilelistVariable(int offset, Object value) {
		assert globals != null;
		assert offset != AVM.NULL_OFFSET;
		globals[offset] = value;
	}

	void pushFrame(long numFormalParams, int positionIdx) {
		localsStack.push(locals);
		locals = new Object[(int) numFormalParams];
		returnIndexes.push(positionIdx);
	}

	/** returns the position index */
	int popFrame() {
		locals = localsStack.pop();
		return returnIndexes.pop();
	}

	void popAllFrames() {
		for (int i = localsStack.size(); i > 0; i--) {
			locals = localsStack.pop();
			returnIndexes.pop();
		}
	}

	private Object returnValue;

	void setReturnValue(Object obj) {
		assert returnValue == null;
		returnValue = obj;
	}

	Object getReturnValue() {
		Object retval;
		if (returnValue == null) {
			retval = BLANK;
		} else {
			retval = returnValue;
		}
		returnValue = null;
		return retval;
	}
}
