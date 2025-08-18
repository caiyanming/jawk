package org.metricshub.jawk;

/*-
 * ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲
 * Jawk
 * ႻႻႻႻႻႻ
 * Copyright (C) 2006 - 2025 MetricsHub
 * ႻႻႻႻႻႻ
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * ╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱
 */

import java.io.IOException;
import java.util.Collections;
import org.metricshub.jawk.backend.AVM;
import org.metricshub.jawk.frontend.AwkParser;
import org.metricshub.jawk.intermediate.AwkTuples;
import org.metricshub.jawk.util.AwkSettings;
import org.metricshub.jawk.ExitException;

/**
 * Utility class to evaluate standalone AWK expressions.
 */
public final class ExpressionEvaluator {

	private ExpressionEvaluator() {}

	public static Object eval(String expression, String input, AwkSettings settings)
			throws IOException,
			ExitException {
		AwkParser parser = new AwkParser(
				settings.isAdditionalFunctions(),
				settings.isAdditionalTypeFunctions(),
				Collections.emptyMap());
		AwkTuples tuples = parser.parseExpression(expression);
		return eval(tuples, input, settings);
	}

	public static Object eval(AwkTuples tuples, String input, AwkSettings settings)
			throws ExitException,
			IOException {
		AVM avm = new AVM(settings, Collections.emptyMap());
		return avm.eval(tuples, input);
	}
}
