package org.metricshub.jawk;

import static org.junit.Assert.*;

import org.junit.Test;
import org.metricshub.jawk.util.AwkSettings;

public class ExpressionEvaluatorTest {

	@Test
	public void testNumericExpression() throws Exception {
		AwkSettings settings = new AwkSettings();
		Object result = ExpressionEvaluator.eval("1 + 2", null, settings);
		assertTrue(result instanceof Number);
		assertEquals(3, ((Number) result).intValue());
	}

	@Test
	public void testFieldExtraction() throws Exception {
		AwkSettings settings = new AwkSettings();
		Object result = ExpressionEvaluator.eval("$2", "my text input", settings);
		assertEquals("text", result);
	}

	@Test
	public void testNF() throws Exception {
		AwkSettings settings = new AwkSettings();
		Object result = ExpressionEvaluator.eval("NF", "a b c", settings);
		assertEquals(3, ((Number) result).intValue());
	}
}
