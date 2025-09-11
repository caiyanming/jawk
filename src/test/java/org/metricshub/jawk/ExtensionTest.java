package org.metricshub.jawk;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.metricshub.jawk.backend.AVM;
import org.metricshub.jawk.ext.JawkExtension;
import org.metricshub.jawk.intermediate.AwkTuples;
import org.metricshub.jawk.util.AwkSettings;
import org.metricshub.jawk.util.ScriptSource;

public class ExtensionTest {

	@Test
	public void testExtension() throws Exception {
		JawkExtension myExtension = new TestExtension();
		Map<String, JawkExtension> myExtensionMap = Arrays
				.stream(myExtension.extensionKeywords())
				.collect(Collectors.toMap(k -> k, k -> myExtension));

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

		// Execute the awk script against the specified input
		AVM avm = null;
		try {
			AwkTuples tuples = new Awk(myExtensionMap).compile(Collections.singletonList(script));
			avm = new AVM(settings, myExtensionMap);
			avm.interpret(tuples);
		} catch (ExitException e) {
			if (e.getCode() != 0) {
				throw e;
			}
		} finally {
			if (avm != null) {
				avm.waitForIO();
			}
		}
		String resultString = resultBytesStream.toString("UTF-8");
		assertEquals("ababab", resultString);
	}
}
