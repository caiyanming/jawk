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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.metricshub.jawk.frontend.AstNode;
import org.metricshub.jawk.intermediate.AwkTuples;
import org.metricshub.jawk.jrt.AwkRuntimeException;
import org.metricshub.jawk.util.AwkSettings;
import org.metricshub.jawk.util.ScriptFileSource;
import org.metricshub.jawk.util.ScriptSource;

/**
 * Command-line interface for Jawk.
 */
public final class Cli {

	private static final String JAR_NAME;

	static {
		String myName;
		try {
			File me = new File(Cli.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			myName = me.getName();
		} catch (Exception e) {
			myName = "Jawk.jar";
		}
		JAR_NAME = myName;
	}

	private final AwkSettings settings = new AwkSettings();
	private final PrintStream out;

	private final List<ScriptSource> scriptSources = new ArrayList<ScriptSource>();
	private AwkTuples precompiledTuples;

	private boolean dumpSyntaxTree;
	private boolean dumpIntermediateCode;
	private String dumpOutputFilename;
	private File compileOutputFile;
	private boolean printUsage;

	public Cli() {
		this(System.in, System.out, System.err);
	}

	@SuppressFBWarnings("EI_EXPOSE_REP2")
	public Cli(InputStream in, PrintStream out, @SuppressWarnings("unused") PrintStream err) {
		this.out = out;
		settings.setInput(in);
		settings.setOutputStream(out);
	}

	@SuppressFBWarnings("EI_EXPOSE_REP")
	public AwkSettings getSettings() {
		return settings;
	}

	public List<ScriptSource> getScriptSources() {
		return new ArrayList<ScriptSource>(scriptSources);
	}

	@SuppressFBWarnings("EI_EXPOSE_REP")
	public AwkTuples getPrecompiledTuples() {
		return precompiledTuples;
	}

	public void parse(String[] args) {
		int argIdx = 0;
		while (argIdx < args.length) {
			String arg = args[argIdx];
			if (arg.length() == 0) {
				throw new IllegalArgumentException("zero-length argument at position " + (argIdx + 1));
			}
			if (arg.charAt(0) != '-') {
				break;
			} else if (arg.equals("-")) {
				++argIdx;
				break;
			} else if (arg.equals("-v")) {
				checkParameterHasArgument(args, argIdx);
				addVariable(settings, args[++argIdx]);
			} else if (arg.equals("-f")) {
				checkParameterHasArgument(args, argIdx);
				scriptSources.add(new ScriptFileSource(args[++argIdx]));
			} else if (arg.equals("-l")) {
				checkParameterHasArgument(args, argIdx);
				String file = args[++argIdx];
				try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
					precompiledTuples = (AwkTuples) ois.readObject();
				} catch (IOException | ClassNotFoundException ex) {
					throw new IllegalArgumentException("Failed to read tuples '" + file + "': " + ex.getMessage(), ex);
				}
			} else if (arg.equals("-K")) {
				checkParameterHasArgument(args, argIdx);
				compileOutputFile = new File(args[++argIdx]);
			} else if (arg.equals("-o")) {
				checkParameterHasArgument(args, argIdx);
				dumpOutputFilename = args[++argIdx];
			} else if (arg.equals("-S")) {
				dumpSyntaxTree = true;
			} else if (arg.equals("-s")) {
				dumpIntermediateCode = true;
			} else if (arg.equals("-t")) {
				settings.setUseSortedArrayKeys(true);
			} else if (arg.equals("-r")) {
				settings.setCatchIllegalFormatExceptions(false);
			} else if (arg.equals("-F")) {
				checkParameterHasArgument(args, argIdx);
				settings.setFieldSeparator(args[++argIdx]);
			} else if (arg.equals("--locale")) {
				checkParameterHasArgument(args, argIdx);
				settings.setLocale(new Locale(args[++argIdx]));
			} else if (arg.equals("-h") || arg.equals("-?")) {
				if (args.length > 1) {
					throw new IllegalArgumentException("When printing help/usage output, we do not accept other arguments.");
				}
				printUsage = true;
				return;
			} else {
				throw new IllegalArgumentException("Unknown parameter: " + arg);
			}
			++argIdx;
		}

		if (scriptSources.isEmpty() && precompiledTuples == null) {
			if (argIdx >= args.length) {
				throw new IllegalArgumentException("Awk script not provided.");
			}
			String scriptContent = args[argIdx++];
			scriptSources
					.add(
							new ScriptSource(
									ScriptSource.DESCRIPTION_COMMAND_LINE_SCRIPT,
									new StringReader(scriptContent)));
		} else if (!scriptSources.isEmpty()) {
			for (ScriptSource scriptSource : scriptSources) {
				try {
					scriptSource.getReader();
				} catch (IOException ex) {
					throw new IllegalArgumentException(
							"Failed to read script '" + scriptSource.getDescription() + "': " + ex.getMessage(),
							ex);
				}
			}
		}

		while (argIdx < args.length) {
			settings.addNameValueOrFileName(args[argIdx++]);
		}
	}

	private static void checkParameterHasArgument(String[] args, int argIdx) {
		if (argIdx + 1 >= args.length) {
			throw new IllegalArgumentException("Need additional argument for " + args[argIdx]);
		}
	}

	private static final Pattern INITIAL_VAR_PATTERN = Pattern.compile("([_a-zA-Z][_0-9a-zA-Z]*)=(.*)");

	private static void addVariable(AwkSettings settings, String keyValue) {
		Matcher m = INITIAL_VAR_PATTERN.matcher(keyValue);
		if (!m.matches()) {
			throw new IllegalArgumentException(
					"keyValue \"" + keyValue + "\" must be of the form \"name=value\"");
		}
		String name = m.group(1);
		String valueString = m.group(2);
		Object value;
		try {
			value = Integer.parseInt(valueString);
		} catch (NumberFormatException nfe) {
			try {
				value = Double.parseDouble(valueString);
			} catch (NumberFormatException nfe2) {
				value = valueString;
			}
		}
		settings.putVariable(name, value);
	}

	private String outputFilename(String defaultName) {
		return dumpOutputFilename != null ? dumpOutputFilename : defaultName;
	}

	public void run() throws Exception {
		if (printUsage) {
			usage(out);
			return;
		}
		Awk awk = new Awk();
		AwkTuples tuples = precompiledTuples != null ? precompiledTuples : awk.compile(scriptSources);
		if (dumpSyntaxTree) {
			try (
					PrintStream ps = new PrintStream(
							new FileOutputStream(outputFilename("syntax_tree.lst")),
							false,
							StandardCharsets.UTF_8.name())) {
				AstNode ast = awk.getLastAst();
				if (ast != null) {
					ast.dump(ps);
				}
			}
		}
		if (dumpIntermediateCode) {
			try (
					PrintStream ps = new PrintStream(
							new FileOutputStream(outputFilename("avm.lst")),
							false,
							StandardCharsets.UTF_8.name())) {
				tuples.dump(ps);
			}
		}
		if (compileOutputFile != null) {
			try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(compileOutputFile))) {
				oos.writeObject(tuples);
			}
			return;
		}
		if (dumpSyntaxTree || dumpIntermediateCode) {
			return;
		}
		awk.invoke(tuples, settings);
	}

	private static void usage(PrintStream dest) {
		dest.println("Usage:");
		dest
				.println(
						"java -jar " +
								JAR_NAME +
								" [-F fs_val]" +
								" [-f script-filename]" +
								" [-l tuples-filename]" +
								" [-K tuples-filename]" +
								" [-o output-filename]" +
								" [-S]" +
								" [-s]" +
								" [-r]" +
								" [--locale locale]" +
								" [-ext]" +
								" [-t]" +
								" [-v name=val]..." +
								" [script]" +
								" [name=val | input_filename]...");
		dest.println();
		dest.println(" -F fs_val = Use fs_val for FS.");
		dest.println(" -f filename = Use contents of filename for script.");
		dest.println(" -l filename = Load precompiled tuples from filename.");
		dest.println(" -v name=val = Initial awk variable assignments.");
		dest.println();
		dest.println(" -t = (extension) Maintain array keys in sorted order.");
		dest.println(" -K filename = Compile to tuples file and halt.");
		dest.println(" -o = (extension) Specify output file.");
		dest.println(" -S = (extension) Write the syntax tree to file. (default: syntax_tree.lst)");
		dest.println(" -s = (extension) Write the intermediate code to file. (default: avm.lst)");
		dest.println(" -r = (extension) Do NOT hide IllegalFormatExceptions for [s]printf.");
		dest.println(" --locale Locale = (extension) Specify a locale to be used instead of US-English");
		dest.println("-ext= (extension) Enable user-defined extensions. (default: not enabled)");
		dest.println();
		dest.println(" -h or -? = (extension) This help screen.");
	}

	public static Cli parseCommandLineArguments(String[] args) {
		Cli cli = new Cli();
		cli.parse(args);
		return cli;
	}

	public static Cli create(String[] args, InputStream is, PrintStream os, PrintStream es) throws Exception {
		Cli cli = new Cli(is, os, es);
		cli.parse(args);
		cli.run();
		return cli;
	}

	@SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "let PrintStream decide line separator")
	public static void main(String[] args) {
		try {
			Cli cli = new Cli();
			cli.parse(args);
			cli.run();
		} catch (ExitException e) {
			System.exit(e.getCode());
		} catch (AwkRuntimeException e) {
			if (e.getLineNumber() >= 0) {
				System.err.printf("%s (line %d): %s\n", e.getClass().getSimpleName(), e.getLineNumber(), e.getMessage());
			} else {
				System.err.printf("%s: %s\n", e.getClass().getSimpleName(), e.getMessage());
			}
			System.exit(1);
		} catch (IllegalArgumentException e) {
			System.err.println("Failed to parse arguments. Please see the help/usage output (cmd line switch '-h').");
			e.printStackTrace(System.err);
			System.exit(1);
		} catch (Exception e) {
			System.err.printf("%s: %s\n", e.getClass().getSimpleName(), e.getMessage());
			System.exit(1);
		}
	}
}
