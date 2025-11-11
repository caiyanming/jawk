package org.metricshub.jawk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.metricshub.jawk.AwkTestSupport.ConfiguredTest;
import org.metricshub.jawk.AwkTestSupport.TestResult;

@RunWith(Parameterized.class)
public class AwkMan7Test {

	private static final boolean IS_POSIX = !System
			.getProperty("os.name", "")
			.toLowerCase(Locale.ROOT)
			.contains("win");

	private static Locale defaultLocale;

	@BeforeClass
	public static void captureLocale() {
		defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.US);
	}

	@AfterClass
	public static void resetLocale() {
		if (defaultLocale != null) {
			Locale.setDefault(defaultLocale);
		}
	}

	@Parameter(0)
	public String description;

	@Parameter(1)
	public ConfiguredTest testCase;

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> parameters() throws Exception {
		List<ConfiguredTest> cases = new ArrayList<>();
		// 1) Options, operands, assignment timing
		cases
				.add(
						test("1. -v visible in BEGIN")
								.script("BEGIN{print X}")
								.preassign("X", "42")
								.expectLines("42")
								.build());

		cases
				.add(
						test("2. Operand assignment after BEGIN")
								.script("BEGIN{print (X==\"\"?\"unset\":\"set\")} {print X \":\" $0}")
								.file("a.txt", "one\n")
								.operand("X=abc", "{{a.txt}}")
								.expectLines("unset", "abc:one")
								.build());

		cases
				.add(
						test("3. Operand assignment per file")
								.script("{print FILENAME \":\" X \":\" $0}")
								.file("A", "a1\na2\n")
								.file("B", "b1\nb2\n")
								.operand("X=AA", "{{A}}", "X=BB", "{{B}}")
								.expectLines("{{A}}:AA:a1", "{{A}}:AA:a2", "{{B}}:BB:b1", "{{B}}:BB:b2")
								.build());

		cases
				.add(
						test("4. BEGIN-only does not read files")
								.script("BEGIN{print \"hi\"}")
								.file("a.txt", "anything\n")
								.operand("{{a.txt}}")
								.expectLines("hi")
								.build());

		cases
				.add(
						test("5. END reads input")
								.script("END{print NR}")
								.file("a.txt", "x\ny\n")
								.operand("{{a.txt}}")
								.expectLines("2")
								.build());

		// 2) Records and fields
		cases
				.add(
						test("6. Default RS is newline")
								.script("{print $0}")
								.stdin("a\nb\nc\n")
								.expectLines("a", "b", "c")
								.build());

		cases
				.add(
						test("7. RS single character")
								.script("BEGIN{RS=\",\"}{print $0}")
								.stdin("a,b,c")
								.expectLines("a", "b", "c")
								.build());

		cases
				.add(
						test("8. RS empty paragraph mode")
								.script(
										"BEGIN{RS=\"\"}{n=split($0, lines, \"\\n\"); count=0; for(i=1;i<=n;i++){if(lines[i]!=\"\"){count++}}; print \"REC:\" NR \":\" count \"lines\"}")
								.stdin("p1-line1\n\n\np2-line1\np2-line2\n\np3\n")
								.expectLines("REC:1:4lines")
								.build());

		cases
				.add(
						test("9. Default FS collapses blanks")
								.script("{print NF \":\" $1 \":\" $2 \":\" $3}")
								.stdin(" a\tb   c")
								.expectLines("3:a:b:c")
								.build());

		cases
				.add(
						test("10. FS comma preserves empties")
								.script("BEGIN{FS=\",\"}{print NF \":\" $1 \":\" $2 \":\" $3 \":\" $4}")
								.stdin("a,,b,c,")
								.expectLines("5:a::b:c")
								.build());

		cases
				.add(
						test("11. FS as ERE")
								.script("BEGIN{FS=\"[ ,:]+\"}{print NF \":\" $1 \":\" $2 \":\" $3}")
								.stdin("a,, b:::c")
								.expectLines("3:a:b:c")
								.build());

		cases
				.add(
						test("12. FS change affects future records")
								.script("NR==1{print NF \":\" $1 \":\" $2; FS=\",\"} NR==2{print NF \":\" $1 \":\" $2}")
								.stdin("a b\nc,d\n")
								.expectLines("2:a:b", "2:c:d")
								.build());

		cases
				.add(
						test("13. Assigning to $0 recomputes fields")
								.script("{$0=\"p q r\"; print NF \":\" $2}")
								.stdin("x y\n")
								.expectLines("3:q")
								.build());

		cases
				.add(
						test("14. Assigning to field rebuilds $0")
								.script("{$2=\"X\"; print $0}")
								.stdin("a b c\n")
								.expectLines("a X c")
								.build());

		cases
				.add(
						test("15. Referencing field past NF")
								.script("{print \"(\" $(NF+1) \"):\" NF}")
								.stdin("a b\n")
								.expectLines("():2")
								.build());

		cases
				.add(
						test("16. Assigning beyond NF grows record")
								.script("BEGIN{OFS=\",\"} {$(NF+2)=5; print NF \":\" $0}")
								.stdin("a b\n")
								.expectLines("4:a,b,,5")
								.build());

		// 3) Pattern evaluation & range patterns
		cases
				.add(
						test("17. Missing action prints record")
								.script("1")
								.stdin("x\n")
								.expectLines("x")
								.build());

		cases
				.add(
						test("18. Regex pattern matches records")
								.script("/ar/")
								.stdin("foo\nbar\n")
								.expectLines("bar")
								.build());

		cases
				.add(
						test("19. Pattern order evaluation")
								.script("{flag=1} flag==1 {print \"seen\"}")
								.stdin("x\n")
								.expectLines("seen")
								.build());

		cases
				.add(
						test("20. Range pattern within file")
								.script("/b/,/d/")
								.stdin("a\nb\nc\nd\ne\n")
								.expectLines("b", "c", "d")
								.build());

		cases
				.add(
						test("21. Range pattern repeats")
								.script("/start/,/end/")
								.stdin("start\nx\nend\nx\nend\n")
								.expectLines("start", "x", "end")
								.build());

		// 4) Regular expressions
		cases
				.add(
						test("22. Unanchored regex matches substring")
								.script("$0 ~ /b/")
								.stdin("abc\n")
								.expectLines("abc")
								.build());

		cases
				.add(
						test("23. Anchored regex")
								.script("$0 ~ /^abc$/")
								.stdin("abc\nabcx\n")
								.expectLines("abc")
								.build());

		cases
				.add(
						test("24. Literal slash in regex")
								.script("$0 ~ /a\\/b/")
								.stdin("a/b\n")
								.expectLines("a/b")
								.build());

		cases
				.add(
						test("25. Regex from variable")
								.script("BEGIN{re=\"a\\/b\"} $0 ~ re {print $0}")
								.stdin("path a/b\n")
								.expectLines("path a/b")
								.build());

		cases
				.add(
						test("26. Regex standalone pattern")
								.script("/A/")
								.stdin("xxAyy\n")
								.expectLines("xxAyy")
								.build());

		// 5) BEGIN / END
		cases
				.add(
						test("27. Multiple BEGIN order")
								.script("BEGIN{print \"1\"} BEGIN{print \"2\"}")
								.expectLines("1", "2")
								.build());

		cases
				.add(
						test("28. Multiple END order")
								.script("END{print \"E1\"} END{print \"E2\"}")
								.stdin("x\ny\n")
								.expectLines("E1", "E2")
								.build());

		cases
				.add(
						test("29. getline in BEGIN consumes record")
								.script("BEGIN{getline; print \"got:\" $0} {print \"line:\" $0}")
								.stdin("L1\nL2\n")
								.expectLines("got:L1", "line:L2")
								.build());

		// 6) Built-in variables & conversions
		cases
				.add(
						test("30. FILENAME visibility")
								.script("BEGIN{print (FILENAME==\"\"?\"undef\":\"bad\")} {last=FILENAME} END{print last}")
								.file("A", "a\n")
								.file("B", "b\n")
								.operand("{{A}}", "{{B}}")
								.expectLines("undef", "{{B}}")
								.build());

		cases
				.add(
						test("31. NR vs FNR")
								.script("{print FILENAME \":\" FNR \":\" NR}")
								.file("A", "x\ny\n")
								.file("B", "z\n")
								.operand("{{A}}", "{{B}}")
								.expectLines("{{A}}:1:1", "{{A}}:2:2", "{{B}}:1:3")
								.build());

		cases
				.add(
						test("32. ARGV manipulation skips file")
								.script("BEGIN{ARGV[1]=\"\"} {print FILENAME \":\" $0}")
								.file("A", "a\n")
								.file("B", "b\n")
								.operand("{{A}}", "{{B}}")
								.expectLines("{{B}}:b")
								.build());

		cases
				.add(
						test("33. OFMT vs CONVFMT")
								.script("BEGIN{OFMT=\"%.2f\"; CONVFMT=\"%.3f\"; x=1.2345; print x; s=x \"\"; print s}")
								.expectLines("1.23", "1.24")
								.build());

		cases
				.add(
						test("34. Integers stringify with %d")
								.script("BEGIN{CONVFMT=\"%.3f\"; x=12; s=x \"\"; print s}")
								.expectLines("12")
								.build());

		cases
				.add(
						test("35. Numeric vs string comparison")
								.script("{print ($0<10)?\"Y\":\"N\"}")
								.stdin("2\n2a\n")
								.expectLines("Y", "N")
								.build());

		// 7) Expressions and operators
		cases
				.add(
						test("36. Exponentiation is right-associative")
								.script("BEGIN{print 2^3^2}")
								.expectLines("512")
								.build());

		cases
				.add(
						test("37. Modulus uses fmod semantics")
								.script("BEGIN{print (-5)%2}")
								.expectLines("-1")
								.build());

		cases
				.add(
						test("38. Concatenation precedence")
								.script("BEGIN{print 1 2+3}")
								.expectLines("15")
								.build());

		cases
				.add(
						test("39. Ternary right associativity")
								.script("BEGIN{print (0?1:0?2:3)}")
								.expectLines("3")
								.build());

		cases
				.add(
						test("40. Pre vs post increment")
								.script("BEGIN{i=0; print i++; print i; j=0; print ++j; print j}")
								.expectLines("0", "1", "1", "1")
								.build());

		cases
				.add(
						test("41. Field expression index")
								.script("{i=2; print $(i)}")
								.stdin("a b c\n")
								.expectLines("b")
								.build());

		// 8) print and printf
		cases
				.add(
						test("42. Print with empty expr list")
								.script("{print; print $0}")
								.stdin("hello\n")
								.expectLines("hello", "hello")
								.build());

		cases
				.add(
						test("43. OFS and ORS")
								.script("BEGIN{OFS=\",\"; ORS=\"|\"} {print $1,$2; print $2,$1}")
								.stdin("a b\n")
								.expect("a,b|b,a|")
								.build());

		cases
				.add(
						test("44. printf width and precision")
								.script("BEGIN{printf \"%.3f\\n\", 1.23456; printf \"%5s\\n\", \"x\"}")
								.expectLines("1.235", "    x")
								.build());

		cases
				.add(
						test("45. printf does not unescape variable format")
								.script("BEGIN{fmt=\"\\\\n\"; printf fmt; printf \"\\n\"}")
								.expect("\\n\n")
								.build());

		// 9) I/O redirection and system
		cases
				.add(
						test("46. Write to file and close")
								.path("tOut")
								.script(
										"BEGIN{f=\"{{tOut}}\"; print \"X\" > f; rc=close(f); print rc; while ((getline line < f)>0) print line; close(f)}")
								.expectLines("0", "X")
								.build());

		cases
				.add(
						test("47. Append with redirect")
								.path("tAppend")
								.script(
										"BEGIN{f=\"{{tAppend}}\"; print \"A\" >> f; print \"B\" >> f; close(f); while ((getline line < f)>0) print line; close(f)}")
								.expectLines("A", "B")
								.build());

		cases
				.add(
						test("48. Pipe to command produces output")
								.script("{print $0 | \"sed s/a/A/\"} END{close(\"sed s/a/A/\")}")
								.stdin("a\nb\n")
								.posixOnly()
								.expectLines("A", "b")
								.build());

		cases
				.add(
						test("49. Command pipe getline")
								.script("BEGIN{cmd=\"printf abc\\n\"; n=(cmd | getline x); print n \":\" x; close(cmd)}")
								.posixOnly()
								.expectLines("1:abc")
								.build());

		cases
				.add(
						test("50. getline from file returns counts")
								.file("fileX", "L1\nL2\n")
								.script("BEGIN{f=\"{{fileX}}\"; n=0; while ((rc=(getline ln < f))>0){n++} print n \":\" rc; close(f)}")
								.expectLines("2:0")
								.build());

		cases
				.add(
						test("51. system() exit status")
								.script("BEGIN{print system(\"sh -c true\"); print (system(\"sh -c false\")!=0?\"NZ\":\"Z\")}")
								.posixOnly()
								.expectLines("0", "NZ")
								.build());

		// 10) next, nextfile, exit
		cases
				.add(
						test("52. next skips remaining rules")
								.script("{print \"A:\" $0; next; print \"B:\" $0}")
								.stdin("a\nb\n")
								.expectLines("A:a", "A:b")
								.build());

		cases
				.add(
						test("53. emulate nextfile skips to next file")
								.script(
										"FNR==1{print $0; fname=FILENAME; while (getline > 0) { if (FILENAME!=fname) {print $0; break} } next} {print \"NEVER\"}")
								.file("A", "a1\na2\n")
								.file("B", "b1\n")
								.operand("{{A}}", "{{B}}")
								.expectLines("a1", "b1")
								.build());

		cases
				.add(
						test("54. exit still runs END")
								.script("{print; exit} END{print \"E\"}")
								.stdin("x\ny\n")
								.expectLines("x", "E")
								.build());

		cases
				.add(
						test("55. BEGIN exit code")
								.script("BEGIN{exit 3}")
								.expect("")
								.expectExit(3)
								.build());

		// 11) String functions
		cases
				.add(
						test("56. length() default argument")
								.script("{print length()}")
								.stdin("abcd\n")
								.expectLines("4")
								.build());

		cases
				.add(
						test("57. index function")
								.script("BEGIN{print index(\"banana\",\"na\"); print index(\"banana\",\"x\")}")
								.expectLines("3", "0")
								.build());

		cases
				.add(
						test("58. substr variations")
								.script("BEGIN{print substr(\"hello\",2,3); print substr(\"hello\",4)}")
								.expectLines("ell", "lo")
								.build());

		cases
				.add(
						test("59. match updates RSTART and RLENGTH")
								.script(
										"BEGIN{print match(\"abc\",\"abc\"), RSTART, RLENGTH; print match(\"xyz\",\"a\"), RSTART, RLENGTH}")
								.expectLines("1 1 3", "0 0 -1")
								.build());

		cases
				.add(
						test("60. sub replaces first occurrence")
								.script("BEGIN{s=\"foo\"; n=sub(/f/, \"X\", s); print n \":\" s}")
								.expectLines("1:Xoo")
								.build());

		cases
				.add(
						test("61. gsub escapes ampersand")
								.script("BEGIN{s=\"aba\"; n=gsub(/a/,\"\\\\&X\",s); print n \":\" s}")
								.expectLines("2:&Xb&X")
								.build());

		cases
				.add(
						test("62. split clears array and counts")
								.script("BEGIN{delete a; n=split(\"a::b:c\", a, \"[:]+\"); print n \":\" a[1] \":\" a[2] \":\" a[3]}")
								.expectLines("3:a:b:c")
								.build());

		cases
				.add(
						test("63. sprintf formatting")
								.script("BEGIN{print sprintf(\"<%6.2f>\", 1.234)}")
								.expectLines("<  1.23>")
								.build());

		// 12) Numeric functions
		cases
				.add(
						test("64. int truncates toward zero")
								.script("BEGIN{print int(-1.7)}")
								.expectLines("-1")
								.build());

		cases
				.add(
						test("65. srand produces repeatable sequence")
								.script("BEGIN{srand(1); r1=rand(); srand(1); r2=rand(); print (r1==r2)?1:0}")
								.expectLines("1")
								.build());

		cases
				.add(
						test("66. sqrt positive")
								.script("BEGIN{printf \"%.5f\\n\", sqrt(9)}")
								.expectLines("3.00000")
								.build());

		// 13) Arrays
		cases
				.add(
						test("67. in operator for arrays")
								.script("BEGIN{print ((1 in a)?1:0); a[1]=0; print ((1 in a)?1:0)}")
								.expectLines("0", "1")
								.build());

		cases
				.add(
						test("68. Multidimensional arrays via SUBSEP")
								.script("BEGIN{a[1,2]=42; print a[1 SUBSEP 2]}")
								.expectLines("42")
								.build());

		cases
				.add(
						test("69. delete element and array")
								.script(
										"BEGIN{a[1]=10; a[2]=20; delete a[1]; c=0; for(i in a)c++; print c; delete a; c=0; for(i in a)c++; print c}")
								.expectLines("1", "0")
								.build());

		cases
				.add(
						test("70. split yields numeric strings")
								.script("BEGIN{n=split(\"10 20\",a,\" \" ); print (a[1]+0)+(a[2]+0)}")
								.expectLines("30")
								.build());

		// 14) User-defined functions
		cases
				.add(
						test("71. Scalars passed by value")
								.script("function f(x){x=5} BEGIN{y=3; f(y); print y}")
								.expectLines("3")
								.build());

		cases
				.add(
						test("72. Arrays passed by reference")
								.script("function g(arr){arr[1]=\"X\"} BEGIN{a[1]=\"A\"; g(a); print a[1]}")
								.expectLines("X")
								.build());

		cases
				.add(
						test("73. Missing actual args default to empty")
								.script("function f(u,v){print (u==\"\"?\"E\":u) \":\" (v==\"\"?\"E\":v)} BEGIN{f(1)}")
								.expectLines("1:E")
								.build());

		cases
				.add(
						test("74. Function call requires no whitespace")
								.script("function f(x){return x} BEGIN{print f(7)}")
								.expectLines("7")
								.build());

		cases
				.add(
						test("75. Recursive function")
								.script("function fact(n){return n? n*fact(n-1):1} BEGIN{print fact(5)}")
								.expectLines("120")
								.build());

		// 15) getline variants
		cases
				.add(
						test("76. Bare getline updates record state")
								.file("A", "a\nb\n")
								.script("BEGIN{print NR \":\" FNR} {if (NR==1){getline; print $0 \":\" NF \":\" NR \":\" FNR; exit}}")
								.operand("{{A}}")
								.expectLines("0:0", "b:1:2:2")
								.build());

		cases
				.add(
						test("77. getline var leaves $0 unchanged")
								.file("A", "a\nb\n")
								.script("{ if (FNR==1) { getline line; print line \":\" $0 \":\" NF \":\" NR \":\" FNR; exit } }")
								.operand("{{A}}")
								.expectLines("b:a:1:2:2")
								.build());

		cases
				.add(
						test("78. getline from named file and close")
								.file("f1", "L1\nL2\n")
								.script("BEGIN{f=\"{{f1}}\"; getline x < f; getline y < f; print x \"-\" y; print (close(f)==0)}")
								.expectLines("L1-L2", "1")
								.build());

		// 16) Field/record counters in BEGIN/END
		cases
				.add(
						test("79. BEGIN NR/FNR zero and getline sets NF")
								.script("BEGIN{print NR \":\" FNR; getline; print NF \":\" $0; exit}")
								.stdin("x\n")
								.expectLines("0:0", "1:x")
								.build());

		cases
				.add(
						test("80. END sees total NR")
								.script("END{print NR}")
								.stdin("a\nb\nc\n")
								.expectLines("3")
								.build());

		// 17) Comparisons and locale
		cases
				.add(
						test("81. String comparison in C locale")
								.script("BEGIN{print (\"abc\"<\"abd\")?\"Y\":\"N\"}")
								.expectLines("Y")
								.build());

		cases
				.add(
						test("82. Numeric comparison with numeric strings")
								.script("BEGIN{print (10<\"2\")?\"Y\":\"N\"; print (10<(\" 2\"))?\"Y\":\"N\"}")
								.expectLines("N", "N")
								.build());

		// 18) ARGC / ARGV processing
		cases
				.add(
						test("83. Assignments in ARGV")
								.script("{print X \":\" $0}")
								.file("A", "a\n")
								.operand("X=Q", "{{A}}")
								.expectLines("Q:a")
								.build());

		cases
				.add(
						test("84. Append file via ARGV in BEGIN")
								.script("BEGIN{ARGC=ARGC+1; ARGV[ARGC-1]=\"{{A}}\"} {print $0}")
								.file("A", "a\n")
								.operand("{{A}}")
								.expectLines("a", "a")
								.build());

		// 19) Grammar & misc
		cases
				.add(
						test("85. Semicolon statement separators")
								.script("{a=1; b=2; print a+b}")
								.stdin("x\n")
								.expectLines("3")
								.build());

		cases
				.add(
						test("86. for-in iteration count")
								.script("BEGIN{a[\"x\"]=1; a[\"y\"]=2; c=0; for (i in a) c++; print c}")
								.expectLines("2")
								.build());

		// 20) Field rebuild edge cases
		cases
				.add(
						test("87. Rebuild $0 with custom OFS")
								.script("BEGIN{OFS=\"|\"} {$2=\"\"; print $0 \":\" NF}")
								.stdin("a b\n")
								.expectLines("a|:2")
								.build());

		// 21) split vs FS semantics
		cases
				.add(
						test("88. split with literal space separator")
								.script("BEGIN{n=split(\" a\\t b  c \", a, \" \" ); print n \":\" a[1] \":\" a[2] \":\" a[3]}")
								.expectLines("3:a:b:c")
								.build());

		// 22) Regex-driven FS
		cases
				.add(
						test("89. FS as class repetition")
								.script("BEGIN{FS=\"[ ,:]+\"}{print NF \":\" $1 \":\" $2}")
								.stdin("a  , , : :b\n")
								.expectLines("2:a:b")
								.build());

		// 23) Paragraph mode specifics
		cases
				.add(
						test("90. RS empty ignores leading blanks")
								.script(
										"BEGIN{RS=\"\"}{gsub(\"\\n+\",\" \",$0); sub(\"^ +\",\"\",$0); sub(\" +$\",\"\",$0); print \"REC-\" NR \":\" $0}")
								.stdin("\n\npara1\n\n\npara2\n\n")
								.expectLines("REC-1:para1 para2")
								.build());

		// 24) Range across files
		cases
				.add(
						test("91. Range resets per file")
								.script("/1/,/2/ {print FILENAME \":\" $0}")
								.file("A", "1\nX\n2\n")
								.file("B", "1\n2\n")
								.operand("{{A}}", "{{B}}")
								.expectLines("{{A}}:1", "{{A}}:X", "{{A}}:2", "{{B}}:1", "{{B}}:2")
								.build());

		// 25) ENVIRON
		cases
				.add(
						test("92. ENVIRON exposes environment variable")
								.script("BEGIN{print ENVIRON[\"AWK_TEST\"]}")
								.expectLines(System.getenv().getOrDefault("AWK_TEST", ""))
								.build());

		// 26) Regex argument via variable
		cases
				.add(
						test("93. match uses regex variable")
								.script("BEGIN{re=\"fo+\"; print match(\"foo\",re)}")
								.expectLines("1")
								.build());

		// 29) Grammar sanity
		cases
				.add(
						test("94. Action-only prints records")
								.script("{print NR \":\" $0}")
								.stdin("x\ny\n")
								.expectLines("1:x", "2:y")
								.build());

		cases
				.add(
						test("95. Pattern-only expression")
								.script("($0+0) {print NR \":T\"}")
								.stdin("1\n0\n")
								.expectLines("1:T")
								.build());

		return cases.stream().map(tc -> new Object[] { tc.description(), tc }).collect(Collectors.toList());
	}

	@Test
	public void runSpec() throws Exception {
		testCase.assumeSupported();
		TestResult result = testCase.run();
		result.assertExpected();
	}

	private static AwkTestSupport.AwkTestBuilder test(String description) {
		return AwkTestSupport.awkTest(description);
	}
}
