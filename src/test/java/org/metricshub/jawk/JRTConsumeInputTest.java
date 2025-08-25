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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;
import org.metricshub.jawk.Awk;
import org.metricshub.jawk.util.AwkSettings;
import org.metricshub.jawk.util.ScriptSource;

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
		File file1 = File.createTempFile("jrt", "1");
		file1.deleteOnExit();
		Files.write(file1.toPath(), "a\n".getBytes(StandardCharsets.UTF_8));

		File file2 = File.createTempFile("jrt", "2");
		file2.deleteOnExit();
		Files.write(file2.toPath(), "b\n".getBytes(StandardCharsets.UTF_8));

		AwkSettings settings = new AwkSettings();
		settings.setDefaultRS("\n");
		settings.setDefaultORS("\n");
		settings.addNameValueOrFileName(file1.getAbsolutePath());
		settings.addNameValueOrFileName("X=1");
		settings.addNameValueOrFileName(file2.getAbsolutePath());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		settings.setOutputStream(new PrintStream(out, true, StandardCharsets.UTF_8.name()));
		settings.addScriptSource(new ScriptSource("Body", new StringReader("{ next } \nEND { print NR }"), false));

		new Awk().invoke(settings);

		assertEquals("3\n", out.toString("UTF-8"));
	}
}
