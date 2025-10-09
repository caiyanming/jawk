package org.metricshub.jawk;

import org.metricshub.jawk.ext.AbstractExtension;
import org.metricshub.jawk.ext.JawkExtension;
import org.metricshub.jawk.ext.annotations.JawkAssocArray;
import org.metricshub.jawk.ext.annotations.JawkFunction;
import org.metricshub.jawk.jrt.AssocArray;

/**
 * Test extension used by the unit tests to exercise the annotation-based
 * extension infrastructure.
 */
public class TestExtension extends AbstractExtension implements JawkExtension {

	private static final String EXTENSION_NAME = "TestExtension";

	/**
	 * Returns the logical name of the test extension.
	 */
	@Override
	public String getExtensionName() {
		return EXTENSION_NAME;
	}

	/**
	 * Concatenates the associative array values {@code count} times.
	 *
	 * @param count number of concatenations to perform
	 * @param array associative array providing the fragments to concatenate
	 * @return concatenated string
	 */
	@JawkFunction("myExtensionFunction")
	public String myExtensionFunction(Number count, @JawkAssocArray AssocArray array) {
		StringBuilder result = new StringBuilder();
		int iterations = count.intValue();
		for (int i = 0; i < iterations; i++) {
			for (Object key : array.keySet()) {
				result.append(array.get(key));
			}
		}
		return result.toString();
	}

	/**
	 * Counts the number of keys present in the provided associative arrays.
	 *
	 * @param arrays associative arrays whose key counts should be aggregated
	 * @return total number of keys across all arrays
	 */
	@JawkFunction("varArgAssoc")
	public int varArgAssoc(@JawkAssocArray AssocArray... arrays) {
		int total = 0;
		for (AssocArray array : arrays) {
			total += array.keySet().size();
		}
		return total;
	}
}
