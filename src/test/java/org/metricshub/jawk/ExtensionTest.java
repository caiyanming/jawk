package org.metricshub.jawk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Collections;
import org.junit.Test;
import org.metricshub.jawk.ExitException;
import org.metricshub.jawk.intermediate.AwkTuples;
import org.metricshub.jawk.jrt.AwkRuntimeException;
import org.metricshub.jawk.jrt.IllegalAwkArgumentException;
import org.metricshub.jawk.util.AwkSettings;
import org.metricshub.jawk.util.ScriptSource;

/**
 * Tests the integration of a custom {@link JawkExtension} implementation with
 * the interpreter.
 */
public class ExtensionTest {

	/**
	 * Verifies that an extension can be registered and invoked from an AWK
	 * script.
	 */
	@Test
	public void testExtension() throws Exception {
		Awk awk = new Awk(new TestExtension());
		AwkSettings settings = new AwkSettings();

		// We force \n as the Record Separator (RS) because even if running on Windows
		// we're passing Java strings, where end of lines are simple \n
		settings.setDefaultRS("\n");
		settings.setDefaultORS("\n");

		// Create the OutputStream, to collect the result as a String
		ByteArrayOutputStream resultBytesStream = new ByteArrayOutputStream();
		settings.setOutputStream(new PrintStream(resultBytesStream));

		ScriptSource script = new ScriptSource(
				"Body",
				new StringReader(
						"BEGIN { ab[1] = \"a\"; ab[2] = \"b\"; printf myExtensionFunction(3, ab) }"));

		AwkTuples tuples = awk.compile(Collections.singletonList(script));
		try {
			awk.invoke(tuples, settings);
		} catch (ExitException e) {
			if (e.getCode() != 0) {
				throw e;
			}
		}
		String resultString = resultBytesStream.toString("UTF-8");
		assertEquals("ababab", resultString);
	}

	@Test(expected = IllegalAwkArgumentException.class)
	public void testAnnotatedExtensionArgumentValidation() throws Exception {
		Awk awk = new Awk(new TestExtension());
		AwkSettings settings = new AwkSettings();

		settings.setDefaultRS("\n");
		settings.setDefaultORS("\n");

		ScriptSource script = new ScriptSource(
				"Body",
				new StringReader("BEGIN { myExtensionFunction() }"));

		awk.compile(Collections.singletonList(script));
	}

	@Test
	public void testVarArgAssocArrayInvocation() throws Exception {
		Awk awk = new Awk(new TestExtension());
		AwkSettings settings = new AwkSettings();

		settings.setDefaultRS("\n");
		settings.setDefaultORS("\n");

		ByteArrayOutputStream resultBytesStream = new ByteArrayOutputStream();
		settings.setOutputStream(new PrintStream(resultBytesStream));

		ScriptSource script = new ScriptSource(
				"Body",
				new StringReader(
						"BEGIN { first[1] = 1; second[1] = 2; print varArgAssoc(first, second) }"));

		AwkTuples tuples = awk.compile(Collections.singletonList(script));
		try {
			awk.invoke(tuples, settings);
		} catch (ExitException e) {
			if (e.getCode() != 0) {
				throw e;
			}
		}
		assertEquals("2\n", resultBytesStream.toString("UTF-8"));
	}

	@Test
	public void testVarArgAssocArraySemanticValidation() throws Exception {
		Awk awk = new Awk(new TestExtension());
		AwkSettings settings = new AwkSettings();

		settings.setDefaultRS("\n");
		settings.setDefaultORS("\n");

		ScriptSource script = new ScriptSource(
				"Body",
				new StringReader(
						"BEGIN { array[1] = 1; scalar = 1; print varArgAssoc(array, scalar) }"));

		try {
			awk.compile(Collections.singletonList(script));
			fail("Expected a compile-time failure for scalar argument");
		} catch (RuntimeException ex) {
			assertTrue(ex.getMessage().contains("associative array"));
		}
	}

	@Test
	public void testVarArgAssocArrayRuntimeValidation() throws Exception {
		Awk awk = new Awk(new TestExtension());
		AwkSettings settings = new AwkSettings();

		settings.setDefaultRS("\n");
		settings.setDefaultORS("\n");

		ScriptSource script = new ScriptSource(
				"Body",
				new StringReader("BEGIN { array[1] = 1; print varArgAssoc(array, 1) }"));

		AwkTuples tuples = awk.compile(Collections.singletonList(script));
		try {
			awk.invoke(tuples, settings);
			fail("Expected IllegalAwkArgumentException for non-array argument");
		} catch (IllegalAwkArgumentException ex) {
// expected
		} catch (AwkRuntimeException ex) {
			assertTrue(ex.getCause() instanceof IllegalAwkArgumentException);
		}
	}
}
