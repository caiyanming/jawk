package org.metricshub.jawk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.metricshub.jawk.ext.JawkExtension;
import org.metricshub.jawk.util.AwkSettings;
import org.metricshub.jawk.util.ScriptSource;

/**
 * Reusable helpers for building and executing Jawk tests. This consolidates the
 * logic that was historically duplicated across different test suites, so that
 * all tests share the same approach for managing temporary files, providing
 * input, and capturing the results of either {@link Awk} or {@link Cli}
 * executions.
 */
public final class AwkTestSupport {

	private static final boolean IS_POSIX = !System
			.getProperty("os.name", "")
			.toLowerCase(Locale.ROOT)
			.contains("win");

	private static final Path SHARED_TEMP_DIR;

	static {
		try {
			SHARED_TEMP_DIR = Files.createTempDirectory("jawk-shared");
			SHARED_TEMP_DIR.toFile().deleteOnExit();
		} catch (IOException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	private AwkTestSupport() {}

	public static AwkTestBuilder awkTest(String description) {
		return new AwkTestBuilder(description);
	}

	public static CliTestBuilder cliTest(String description) {
		return new CliTestBuilder(description);
	}

	public static Path sharedTempDirectory() {
		return SHARED_TEMP_DIR;
	}

	public interface ConfiguredTest {
		String description();

		void assumeSupported();

		TestResult run() throws Exception;

		default void runAndAssert() throws Exception {
			assumeSupported();
			run().assertExpected();
		}
	}

	public static final class TestResult {
		private final String description;
		private final String output;
		private final int exitCode;
		private final String expectedOutput;
		private final Integer expectedExitCode;
		private final Class<? extends Throwable> expectedException;
		private final Throwable thrownException;

		TestResult(
				String description,
				String output,
				int exitCode,
				String expectedOutput,
				Integer expectedExitCode,
				Class<? extends Throwable> expectedException,
				Throwable thrownException) {
			this.description = description;
			this.output = output;
			this.exitCode = exitCode;
			this.expectedOutput = expectedOutput;
			this.expectedExitCode = expectedExitCode;
			this.expectedException = expectedException;
			this.thrownException = thrownException;
		}

		public String description() {
			return description;
		}

		public String output() {
			return output;
		}

		public int exitCode() {
			return exitCode;
		}

		public String[] lines() {
			if (output.isEmpty()) {
				return new String[0];
			}
			String trimmed = output.endsWith("\n") ? output.substring(0, output.length() - 1) : output;
			String[] pieces = trimmed.split("\n", -1);
			for (int i = 0; i < pieces.length; ++i) {
				if (pieces[i].endsWith("\r")) {
					pieces[i] = pieces[i].substring(0, pieces[i].length() - 1);
				}
			}
			return pieces;
		}

		public void assertExpected() {
			if (expectedException != null) {
				if (thrownException == null) {
					throw new AssertionError(
							"Expected exception "
									+ expectedException.getName()
									+ " for "
									+ description
									+ " but execution completed successfully");
				}
				if (!expectedException.isInstance(thrownException)) {
					throw new AssertionError(
							"Expected exception "
									+ expectedException.getName()
									+ " for "
									+ description
									+ " but got "
									+ thrownException.getClass().getName());
				}
				return;
			}
			if (expectedOutput != null) {
				assertEquals("Unexpected output for " + description, expectedOutput, output);
			}
			if (expectedExitCode != null) {
				assertEquals("Unexpected exit code for " + description, expectedExitCode.intValue(), exitCode);
			} else {
				assertEquals("Unexpected exit code for " + description, 0, exitCode);
			}
		}

		public String expectedOutput() {
			return expectedOutput;
		}

		public Integer expectedExitCode() {
			return expectedExitCode;
		}

		public Throwable thrownException() {
			return thrownException;
		}

		public Class<? extends Throwable> expectedException() {
			return expectedException;
		}
	}

	public static final class AwkTestBuilder extends BaseTestBuilder<AwkTestBuilder> {
		private final Map<String, Object> preAssignments = new LinkedHashMap<>();
		private Awk customAwk;
		private final List<JawkExtension> extensions = new ArrayList<>();

		private AwkTestBuilder(String description) {
			super(description);
		}

		public AwkTestBuilder preassign(String name, Object value) {
			preAssignments.put(name, value);
			return this;
		}

		public AwkTestBuilder withAwk(Awk awkEngine) {
			if (awkEngine == null) {
				throw new IllegalArgumentException("Awk instance must not be null");
			}
			this.customAwk = awkEngine;
			return this;
		}

		public AwkTestBuilder withExtensions(JawkExtension... extensionsParam) {
			if (extensionsParam != null) {
				extensions.addAll(Arrays.asList(extensionsParam));
			}
			return this;
		}

		public AwkTestBuilder withExtensions(Collection<? extends JawkExtension> extensionsParam) {
			if (extensionsParam != null) {
				extensions.addAll(extensionsParam);
			}
			return this;
		}

		@Override
		protected AwkTestCase buildTestCase(
				TestLayout layout,
				Map<String, String> files,
				List<String> operands,
				List<String> placeholders) {
			if (useTempDir && !preAssignments.containsKey("TEMPDIR")) {
				preAssignments.put("TEMPDIR", SHARED_TEMP_DIR.toString());
			}
			return new AwkTestCase(
					layout,
					files,
					operands,
					placeholders,
					requiresPosix,
					preAssignments,
					customAwk,
					extensions);
		}
	}

	public static final class CliTestBuilder extends BaseTestBuilder<CliTestBuilder> {
		private final List<String> argumentSpecs = new ArrayList<>();
		private final Map<String, Object> assignments = new LinkedHashMap<>();

		private CliTestBuilder(String description) {
			super(description);
		}

		public CliTestBuilder argument(String... args) {
			argumentSpecs.addAll(Arrays.asList(args));
			return this;
		}

		public CliTestBuilder preassign(String name, Object value) {
			assignments.put(name, value);
			return this;
		}

		@Override
		protected CliTestCase buildTestCase(
				TestLayout layout,
				Map<String, String> files,
				List<String> operands,
				List<String> placeholders) {
			if (useTempDir && !assignments.containsKey("TEMPDIR")) {
				assignments.put("TEMPDIR", SHARED_TEMP_DIR.toString());
			}
			return new CliTestCase(layout, files, operands, placeholders, requiresPosix, argumentSpecs, assignments);
		}
	}

	private abstract static class BaseTestBuilder<B extends BaseTestBuilder<B>> {
		protected final String description;
		protected String script;
		protected String stdin;
		protected final Map<String, String> fileContents = new LinkedHashMap<>();
		protected final List<String> operandSpecs = new ArrayList<>();
		protected final List<String> pathPlaceholders = new ArrayList<>();
		protected String expectedOutput;
		protected Integer expectedExitCode;
		protected Class<? extends Throwable> expectedException;
		protected boolean requiresPosix;
		protected boolean useTempDir;

		BaseTestBuilder(String description) {
			this.description = description;
		}

		@SuppressWarnings("unchecked")
		public B script(String script) {
			this.script = script;
			return (B) this;
		}

		@SuppressWarnings("unchecked")
		public B script(InputStream scriptStream) {
			if (scriptStream == null) {
				throw new IllegalArgumentException("scriptStream must not be null");
			}
			try (InputStream in = scriptStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				byte[] buffer = new byte[8192];
				int read;
				while ((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
				}
				return script(new String(out.toByteArray(), StandardCharsets.UTF_8));
			} catch (IOException ex) {
				throw new UncheckedIOException("Failed to read script stream", ex);
			}
		}

		@SuppressWarnings("unchecked")
		public B stdin(String stdin) {
			this.stdin = stdin;
			return (B) this;
		}

		@SuppressWarnings("unchecked")
		public B file(String name, String contents) {
			fileContents.put(name, contents);
			return (B) this;
		}

		@SuppressWarnings("unchecked")
		public B operand(String... operands) {
			operandSpecs.addAll(Arrays.asList(operands));
			return (B) this;
		}

		@SuppressWarnings("unchecked")
		public B path(String placeholder) {
			pathPlaceholders.add(placeholder);
			return (B) this;
		}

		@SuppressWarnings("unchecked")
		public B expect(String expected) {
			this.expectedOutput = expected;
			return (B) this;
		}

		@SuppressWarnings("unchecked")
		public B expectLines(String... lines) {
			if (lines.length == 0) {
				this.expectedOutput = "";
			} else {
				this.expectedOutput = Arrays.stream(lines).collect(Collectors.joining("\n", "", "\n"));
			}
			return (B) this;
		}

		@SuppressWarnings("unchecked")
		public B expectExit(int code) {
			this.expectedExitCode = code;
			return (B) this;
		}

		@SuppressWarnings("unchecked")
		public B expectThrow(Class<? extends Throwable> exceptionClass) {
			this.expectedException = exceptionClass;
			return (B) this;
		}

		@SuppressWarnings("unchecked")
		public B posixOnly() {
			this.requiresPosix = true;
			return (B) this;
		}

		@SuppressWarnings("unchecked")
		public B withTempDir() {
			this.useTempDir = true;
			return (B) this;
		}

		public ConfiguredTest build() {
			TestLayout layout = new TestLayout(
					description,
					script,
					stdin,
					expectedOutput,
					expectedExitCode,
					expectedException);
			Map<String, String> files = new LinkedHashMap<>(fileContents);
			List<String> operands = new ArrayList<>(operandSpecs);
			List<String> placeholders = new ArrayList<>(pathPlaceholders);
			return buildTestCase(layout, files, operands, placeholders);
		}

		public TestResult run() throws Exception {
			return build().run();
		}

		public void runAndAssert() throws Exception {
			build().runAndAssert();
		}

		protected abstract BaseTestCase buildTestCase(
				TestLayout layout,
				Map<String, String> fileContents,
				List<String> operandSpecs,
				List<String> pathPlaceholders);
	}

	private abstract static class BaseTestCase implements ConfiguredTest {
		private final TestLayout layout;
		private final Map<String, String> fileContents;
		private final List<String> operandSpecs;
		private final List<String> pathPlaceholders;
		private final boolean requiresPosix;

		BaseTestCase(
				TestLayout layout,
				Map<String, String> fileContents,
				List<String> operandSpecs,
				List<String> pathPlaceholders,
				boolean requiresPosix) {
			this.layout = layout;
			this.fileContents = fileContents;
			this.operandSpecs = operandSpecs;
			this.pathPlaceholders = pathPlaceholders;
			this.requiresPosix = requiresPosix;
		}

		@Override
		public String description() {
			return layout.description;
		}

		@Override
		public void assumeSupported() {
			if (requiresPosix) {
				assumeTrue("POSIX-like environment required for " + layout.description, IS_POSIX);
			}
		}

		@Override
		public final TestResult run() throws Exception {
			assumeSupported();
			ExecutionEnvironment env = prepareEnvironment();
			try {
				return executeAndCapture(env);
			} finally {
				deleteRecursively(env.tempDir);
			}
		}

		private TestResult executeAndCapture(ExecutionEnvironment env) throws Exception {
			try {
				ActualResult result = execute(env);
				String expected = layout.expectedOutput != null ? env.resolve(layout.expectedOutput) : null;
				return new TestResult(
						layout.description,
						result.output,
						result.exitCode,
						expected,
						layout.expectedExitCode,
						layout.expectedException,
						null);
			} catch (Throwable ex) {
				if (layout.expectedException != null && layout.expectedException.isInstance(ex)) {
					return new TestResult(
							layout.description,
							"",
							0,
							null,
							layout.expectedExitCode,
							layout.expectedException,
							ex);
				}
				if (ex instanceof Exception) {
					throw (Exception) ex;
				}
				throw (Error) ex;
			}
		}

		protected abstract ActualResult execute(ExecutionEnvironment env) throws Exception;

		protected ExecutionEnvironment prepareEnvironment() throws IOException {
			Path tempDir = Files.createTempDirectory("jawk-test");
			Map<String, Path> placeholders = new LinkedHashMap<>();
			for (Map.Entry<String, String> entry : fileContents.entrySet()) {
				Path path = tempDir.resolve(entry.getKey());
				Path parent = path.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
				}
				Files.write(path, entry.getValue().getBytes(StandardCharsets.UTF_8));
				placeholders.put(entry.getKey(), path);
			}
			for (String placeholder : pathPlaceholders) {
				Path path = tempDir.resolve(placeholder);
				Path parent = path.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
				}
				placeholders.put(placeholder, path);
			}
			return new ExecutionEnvironment(tempDir, placeholders);
		}

		protected List<String> resolvedOperands(ExecutionEnvironment env) {
			return operandSpecs
					.stream()
					.map(env::resolve)
					.collect(Collectors.toList());
		}

		protected String resolvedScript(ExecutionEnvironment env) {
			return layout.script != null ? env.resolve(layout.script) : null;
		}

		protected String resolvedStdin(ExecutionEnvironment env) {
			return layout.stdin != null ? env.resolve(layout.stdin) : null;
		}
	}

	private static final class AwkTestCase extends BaseTestCase {
		private final Map<String, Object> preAssignments;
		private final Awk customAwk;
		private final List<JawkExtension> extensions;

		AwkTestCase(
				TestLayout layout,
				Map<String, String> fileContents,
				List<String> operandSpecs,
				List<String> pathPlaceholders,
				boolean requiresPosix,
				Map<String, Object> preAssignments,
				Awk customAwk,
				List<JawkExtension> extensions) {
			super(layout, fileContents, operandSpecs, pathPlaceholders, requiresPosix);
			this.preAssignments = new LinkedHashMap<>(preAssignments);
			this.customAwk = customAwk;
			this.extensions = new ArrayList<>(extensions);
		}

		@Override
		protected ActualResult execute(ExecutionEnvironment env) throws Exception {
			AwkSettings settings = new AwkSettings();
			settings.setDefaultRS("\n");
			settings.setDefaultORS("\n");
			for (Map.Entry<String, Object> entry : preAssignments.entrySet()) {
				settings.putVariable(entry.getKey(), entry.getValue());
			}
			String stdin = resolvedStdin(env);
			if (stdin != null) {
				settings.setInput(new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)));
			} else {
				settings.setInput(new ByteArrayInputStream(new byte[0]));
			}
			ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
			settings.setOutputStream(new PrintStream(outBytes, true, StandardCharsets.UTF_8.name()));
			for (String operand : resolvedOperands(env)) {
				settings.addNameValueOrFileName(operand);
			}
			Awk awk;
			if (customAwk != null) {
				awk = customAwk;
			} else if (!extensions.isEmpty()) {
				awk = new Awk(extensions);
			} else {
				awk = new Awk();
			}
			int exitCode = 0;
			try {
				String resolvedScript = resolvedScript(env);
				awk.invoke(new ScriptSource("test", new StringReader(resolvedScript)), settings);
			} catch (ExitException ex) {
				exitCode = ex.getCode();
			}
			return new ActualResult(outBytes.toString(StandardCharsets.UTF_8.name()), exitCode);
		}
	}

	private static final class CliTestCase extends BaseTestCase {
		private final List<String> argumentSpecs;
		private final Map<String, Object> assignments;

		CliTestCase(
				TestLayout layout,
				Map<String, String> fileContents,
				List<String> operandSpecs,
				List<String> pathPlaceholders,
				boolean requiresPosix,
				List<String> argumentSpecs,
				Map<String, Object> assignments) {
			super(layout, fileContents, operandSpecs, pathPlaceholders, requiresPosix);
			this.argumentSpecs = new ArrayList<>(argumentSpecs);
			this.assignments = new LinkedHashMap<>(assignments);
		}

		@Override
		protected ActualResult execute(ExecutionEnvironment env) throws Exception {
			String stdin = resolvedStdin(env);
			InputStream in = stdin != null ?
					new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)) : new ByteArrayInputStream(new byte[0]);
			ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
			ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
			Cli cli = new Cli(
					in,
					new PrintStream(outBytes, true, StandardCharsets.UTF_8.name()),
					new PrintStream(errBytes, true, StandardCharsets.UTF_8.name()));

			List<String> args = new ArrayList<>();
			for (Map.Entry<String, Object> entry : assignments.entrySet()) {
				args.add("-v");
				args.add(entry.getKey() + "=" + String.valueOf(entry.getValue()));
			}
			for (String spec : argumentSpecs) {
				args.add(env.resolve(spec));
			}
			String resolvedScript = resolvedScript(env);
			if (resolvedScript != null) {
				args.add(resolvedScript);
			}
			args.addAll(resolvedOperands(env));

			int exitCode = 0;
			try {
				cli.parse(args.toArray(new String[0]));
				cli.run();
			} catch (ExitException ex) {
				exitCode = ex.getCode();
			}
			return new ActualResult(outBytes.toString(StandardCharsets.UTF_8.name()), exitCode);
		}
	}

	private static final class ExecutionEnvironment {
		private final Path tempDir;
		private final Map<String, Path> placeholders;

		ExecutionEnvironment(Path tempDir, Map<String, Path> placeholders) {
			this.tempDir = tempDir;
			this.placeholders = placeholders;
		}

		String resolve(String value) {
			if (value == null) {
				return null;
			}
			String result = value;
			for (Map.Entry<String, Path> entry : placeholders.entrySet()) {
				result = result.replace("{{" + entry.getKey() + "}}", entry.getValue().toString());
			}
			return result;
		}
	}

	private static final class ActualResult {
		final String output;
		final int exitCode;

		ActualResult(String output, int exitCode) {
			this.output = output;
			this.exitCode = exitCode;
		}
	}

	private static final class TestLayout {
		final String description;
		final String script;
		final String stdin;
		final String expectedOutput;
		final Integer expectedExitCode;
		final Class<? extends Throwable> expectedException;

		TestLayout(
				String description,
				String script,
				String stdin,
				String expectedOutput,
				Integer expectedExitCode,
				Class<? extends Throwable> expectedException) {
			this.description = description;
			this.script = script;
			this.stdin = stdin;
			this.expectedOutput = expectedOutput;
			this.expectedExitCode = expectedExitCode;
			this.expectedException = expectedException;
		}
	}

	private static void deleteRecursively(Path root) throws IOException {
		if (root == null || !Files.exists(root)) {
			return;
		}
		try (Stream<Path> walk = Files.walk(root)) {
			walk.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException ignored) {
					// best effort cleanup
				}
			});
		}
	}
}
