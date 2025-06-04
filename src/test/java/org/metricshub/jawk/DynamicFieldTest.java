package org.metricshub.jawk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.metricshub.jawk.AwkTestHelper.runAwk;

import org.junit.Test;
import org.metricshub.jawk.jrt.AwkRuntimeException;

public class DynamicFieldTest {

	@Test
	public void testEmptyStringFieldIndex() throws Exception {
		String result = runAwk("BEGIN{idx=\"\"}{print $(idx)}", "a b c");
		assertEquals("a b c\n", result);
	}

	@Test
	public void testNonNumericFieldIndex() throws Exception {
		String result = runAwk("BEGIN{idx=\"foo\"}{print $(idx)}", "a b");
		assertEquals("a b\n", result);
	}

	@Test
	public void testUninitializedVariableFieldIndex() throws Exception {
		String result = runAwk("{print $(idx)}", "a b");
		assertEquals("a b\n", result);
	}

	@Test
	public void testNumericStringFieldIndex() throws Exception {
		String result = runAwk("BEGIN{idx=\"2\"}{print $(idx)}", "a b c");
		assertEquals("b\n", result);
	}

	@Test
	public void testFloatStringFieldIndex() throws Exception {
		String result = runAwk("BEGIN{idx=\"2.7\"}{print $(idx)}", "a b c");
		assertEquals("b\n", result);
	}

	@Test
	public void testFloatVariableFieldIndex() throws Exception {
		String result = runAwk("BEGIN{idx=2.3}{print $(idx)}", "a b c");
		assertEquals("b\n", result);
	}

	@Test
	public void testExponentStringFieldIndex() throws Exception {
		String result = runAwk("BEGIN{idx=\"3e0\"}{print $(idx)}", "1 2 3 4");
		assertEquals("3\n", result);
	}

	@Test
	public void testNegativeFieldIndex() {
		assertThrows(
				AwkRuntimeException.class,
				() -> runAwk("BEGIN{idx=-1}{print $(idx)}", "a b"));
	}
}
