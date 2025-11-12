package org.metricshub.jawk;

/*-
 * ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲
 * Jawk
 * ჻჻჻჻჻჻
 * Copyright (C) 2006 - 2025 MetricsHub
 * ჻჻჻჻჻჻
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
 * ╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱
 */

import org.junit.Test;
import org.metricshub.jawk.AwkTestSupport;

/**
 * Tests for the {@link org.metricshub.jawk.jrt.JRT#consumeInput} method.
 */
public class JRTConsumeInputTest {

	/**
	 * Ensures that variable assignments interleaved with filenames in
	 * {@code ARGV} correctly advance {@code NR}.
	 *
	 * @throws Exception if the AWK invocation fails
	 */
	@Test
	public void testVariableAssignmentBetweenFilesIncrementsNR() throws Exception {
		AwkTestSupport
				.awkTest("variable assignments interleaved with filenames advance NR")
				.file("file1", "a\n")
				.file("file2", "b\n")
				.script("{ next } \nEND { print NR }")
				.operand("{{file1}}", "X=1", "{{file2}}")
				.expectLines("3")
				.runAndAssert();
	}
}
