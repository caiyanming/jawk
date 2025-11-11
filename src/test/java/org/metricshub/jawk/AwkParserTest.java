package org.metricshub.jawk;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.metricshub.jawk.frontend.ast.LexerException;

public class AwkParserTest {

	private static final Awk AWK = new Awk();

	private static String runAwk(String script, String input) throws Exception {
		AwkTestSupport.AwkTestBuilder builder = AwkTestSupport
				.awkTest("AwkParserTest" + script.hashCode())
				.script(script);
		if (input != null) {
			builder.stdin(input);
		}
		return builder.build().run().output();
	}

	private static String readResource(String resource) throws IOException {
		try (
				InputStream in = AwkParserTest.class.getResourceAsStream(resource);
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			if (in == null) {
				throw new IOException("Resource not found: " + resource);
			}
			byte[] buffer = new byte[4096];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			return out.toString(StandardCharsets.UTF_8.name());
		}
	}

	@Test
	public void testStringParsing() throws Exception {
		assertEquals("'\\\\' must become \\", "\\", AWK.eval("\"\\\\\" "));
		assertEquals("'\\a' must become BEL", "\u0007", AWK.eval("\"\\a\" "));
		assertEquals("'\\b' must become BS", "\u0008", AWK.eval("\"\\b\" "));
		assertEquals("'\\f' must become FF", "\014", AWK.eval("\"\\f\" "));
		assertEquals("'\\n' must become LF", "\n", AWK.eval("\"\\n\" "));
		assertEquals("'\\r' must become CR", "\r", AWK.eval("\"\\r\" "));
		assertEquals("'\\t' must become TAB", "\t", AWK.eval("\"\\t\" "));
		assertEquals("'\\v' must become VT", "\u000B", AWK.eval("\"\\v\" "));
		assertEquals("'\\33' must become ESC", "\u001B", AWK.eval("\"\\33\" "));
		assertEquals("'\\1!' must become {0x01, 0x21}", "\u0001!", AWK.eval("\"\\1!\" "));
		assertEquals("'\\19' must become {0x01, 0x39}", "\u00019", AWK.eval("\"\\19\" "));
		assertEquals("'\\38' must become {0x03, 0x38}", "\u00038", AWK.eval("\"\\38\" "));
		assertEquals("'\\132' must become Z", "Z", AWK.eval("\"\\132\" "));
		assertEquals("'\\1320' must become Z0", "Z0", AWK.eval("\"\\1320\" "));
		assertEquals("'\\\"' must become \"", "\"", AWK.eval("\"\\\"\" "));
		assertEquals("'\\x1B' must become ESC", "\u001B", AWK.eval("\"\\x1B\" "));
		assertEquals("'\\x1b' must become ESC", "\u001B", AWK.eval("\"\\x1b\" "));
		assertEquals("'\\x1!' must become {0x01, 0x21}", "\u0001!", AWK.eval("\"\\x1!\" "));
		assertEquals("'\\x1G' must become {0x01, 0x47}", "\u0001G", AWK.eval("\"\\x1G\" "));
		assertEquals("'\\x21A' must become !A", "!A", AWK.eval("\"\\x21A\" "));
		assertEquals("'\\x!' must become x!", "x!", AWK.eval("\"\\x!\" "));
		assertThrows(
				"Unfinished string by EOF must throw",
				LexerException.class,
				() -> runAwk("BEGIN { printf \"unfinished", null));
		assertThrows(
				"Unfinished string by EOL must throw",
				LexerException.class,
				() -> AWK.eval("\"unfinished\n\""));
		assertThrows(
				"Interrupted octal number in string by EOF must throw",
				LexerException.class,
				() -> runAwk("BEGIN { printf \"unfinished\\0", null));
		assertThrows(
				"Interrupted octal number in string by EOL must throw",
				LexerException.class,
				() -> AWK.eval("\"unfinished\\0\n\""));
		assertThrows(
				"Interrupted hex number in string by EOF must throw",
				LexerException.class,
				() -> runAwk("BEGIN { printf \"unfinished\\xF", null));
		assertThrows(
				"Interrupted hex number in string by EOL must throw",
				LexerException.class,
				() -> AWK.eval("\"unfinished\\xf\n\""));
	}

	@Test
	public void testMultiLineStatement() throws Exception {
		assertEquals("|| must allow eol", "success", runAwk("BEGIN { if (0 || \n    1) { printf \"success\" } }", null));
		assertEquals("&& must allow eol", "success", runAwk("BEGIN { if (1 && \n    1) { printf \"success\" } }", null));
		assertEquals("? must allow eol", "success", AWK.eval("1 ?\n\"success\" : \"failed\" "));
		assertEquals(": must allow eol", "success", AWK.eval("1 ? \"success\" :\n\"failed\" "));
		assertEquals(", must allow eol", "success", runAwk("BEGIN { printf(\"%s\", \n\"success\") }", null));
		assertEquals("do must allow eol", "success", runAwk("BEGIN { do\n printf \"success\"; while (0) }", null));
		assertEquals(
				"else must allow eol",
				"success",
				runAwk("BEGIN { if (0) { printf \"failure\" } else \n printf \"success\" }", null));
	}

	@Test
	public void testUnaryPlus() throws Exception {
		assertEquals("+a must convert a to number", 0L, AWK.eval("+a "));
	}

	@Test
	public void testTernaryExpression() throws Exception {
		assertEquals(
				"Ternary expression must allow string concatenations",
				"success",
				runAwk("BEGIN { printf( a \"1\" b ? \"suc\" \"cess\" : \"failure\" ) }", null));
	}

	@Test
	public void testNestedTernaryExpression() throws Exception {
		assertEquals(
				"Nested ternary must parse correctly",
				2L,
				AWK.eval("1 ? 2 : 3 ? 4 : 5 "));
	}

	@Test
	public void testTernaryAfterPrintParentheses() throws Exception {
		assertEquals(
				"Ternary after print parentheses must parse",
				"20\n",
				runAwk("BEGIN { print (1>2) ? 10 : 20 }", null));
	}

	@Test
	public void testGron() throws Exception {
		String gron = readResource("/xonixx/gron.awk");
		assertEquals("gron.awk must not trigger any parser exception", "json=[]\n", runAwk(gron, "[]"));
		assertEquals(
				"gron.awk must work",
				"json=[]\njson[0]={}\njson[0].a=1\njson[1]={}\njson[1].b=\"2\"\n",
				runAwk(gron, "[{\"a\": 1},\n{\"b\": \"2\"}]"));
	}

	@Test
	public void testPow() throws Exception {
		assertEquals("^ (pow) operator must be supported", 256L, AWK.eval("2^8 "));
		assertEquals("** (pow) operator must be supported", 256L, AWK.eval("2**8 "));
	}

	@Test
	public void testPowAssignment() throws Exception {
		assertEquals("^= must be supported", "4\n", runAwk("BEGIN { n = 2; n ^= 2; print n }", null));
		assertEquals("**= must be supported", "4\n", runAwk("BEGIN { n = 2; n **= 2; print n }", null));
	}

	@Test
	public void testOperatorPrecedence() throws Exception {
		assertEquals(
				"$a precedes a++",
				"1122",
				runAwk("{ a = 1; printf $a++ ; printf a ; printf $(a++) ; printf a }", "1 2 3"));
		assertEquals(
				"$a precedes ++a",
				"2233",
				runAwk("{ a = 1; printf $++a ; printf a ; printf $(++a) ; printf a }", "1 2 3"));
		assertEquals(
				"$a precedes a--",
				"3322",
				runAwk("{ a = 3; printf $a-- ; printf a ; printf $(a--) ; printf a }", "1 2 3"));
		assertEquals(
				"$a precedes --a",
				"2211",
				runAwk("{ a = 3; printf $--a ; printf a ; printf $(--a) ; printf a }", "1 2 3"));
		assertEquals("++ precedes ^", "22", runAwk("BEGIN { a = 1; printf(2^a++); printf a }", null));
		assertEquals("^ precedes unary -", -1L, AWK.eval("-1^2"));
		assertEquals("^ precedes unary !", 1, AWK.eval("!0^2"));
		assertEquals("Unary - precedes *", -2L, AWK.eval("0 + -1 * 2"));
		assertEquals("* precedes +", 5L, AWK.eval("1 + 2 * 2"));
		assertEquals("+ precedes string concat", "33", AWK.eval("1 + 2 3"));
	}

	@Test
	public void testRegExpConstant() throws Exception {
		assertEquals("/\\\\/ must be supported", "success", runAwk("/\\\\/ { printf \"success\" }", "a\\b"));
		assertEquals("/\\// must be supported", "success", runAwk("/\\// { printf \"success\" }", "a/b"));
		assertEquals("/=1/ must be supported", "success", runAwk("/=1/ { printf \"success\" }", "a=1\n1\n="));
		assertEquals("/\\057/ must be supported", "success", runAwk("/\\057/ { printf \"success\" }", "a/b"));
		assertThrows(
				"Unfinished regexp by EOF must throw",
				LexerException.class,
				() -> runAwk("/unfinished { print $0 }", null));
		assertThrows(
				"Unfinished regexp by EOL must throw",
				LexerException.class,
				() -> AWK.eval("/unfinished\n/"));
	}
}
