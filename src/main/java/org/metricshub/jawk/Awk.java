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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.metricshub.jawk.backend.AVM;
import org.metricshub.jawk.ext.JawkExtension;
import org.metricshub.jawk.frontend.AwkParser;
import org.metricshub.jawk.frontend.AwkSyntaxTree;
import org.metricshub.jawk.intermediate.AwkTuples;
import org.metricshub.jawk.util.AwkLogger;
import org.metricshub.jawk.util.AwkSettings;
import org.metricshub.jawk.util.ScriptSource;
import org.slf4j.Logger;

/**
 * Entry point into the parsing, analysis, and execution
 * of a Jawk script.
 * This entry point is used when Jawk is executed as a library.
 * If you want to use Jawk as a stand-alone application, please use {@see Main}.
 * <p>
 * The overall process to execute a Jawk script is as follows:
 * <ul>
 * <li>Parse the Jawk script, producing an abstract syntax tree.
 * <li>Traverse the abstract syntax tree, producing a list of
 * instruction tuples for the interpreter.
 * <li>Traverse the list of tuples, providing a runtime which
 * ultimately executes the Jawk script, <strong>or</strong>
 * Command-line parameters dictate which action is to take place.
 * </ul>
 * Two additional semantic checks on the syntax tree are employed
 * (both to resolve function calls for defined functions).
 * As a result, the syntax tree is traversed three times.
 * And the number of times tuples are traversed is depends
 * on whether interpretation or compilation takes place.
 * <p>
 * By default a minimal set of extensions are automatically
 * included. Please refer to the EXTENSION_PREFIX static field
 * contents for an up-to-date list. As of the initial release
 * of the extension system, the prefix defines the following
 * extensions:
 * <ul>
 * <li>CoreExtension
 * <li>SocketExtension
 * <li>StdinExtension
 * </ul>
 *
 * @see org.metricshub.jawk.backend.AVM
 * @author Danny Daglas
 */
public class Awk {

	private static final String DEFAULT_EXTENSIONS = org.metricshub.jawk.ext.CoreExtension.class.getName()
			+ "#"
			+ org.metricshub.jawk.ext.StdinExtension.class.getName();

	private static final Logger LOG = AwkLogger.getLogger(Awk.class);

	/**
	 * Create a new instance of Awk
	 */
	public Awk() {}

	/**
	 * <p>
	 * invoke.
	 * </p>
	 *
	 * @param settings This tells AWK what to do
	 *        (where to get input from, where to write it to, in what mode to run,
	 *        ...)
	 * @throws java.io.IOException upon an IO error.
	 * @throws java.lang.ClassNotFoundException if intermediate code is specified
	 *         but deserialization fails to load in the JVM
	 * @throws org.metricshub.jawk.ExitException if interpretation is requested,
	 *         and a specific exit code is requested.
	 */
	public void invoke(AwkSettings settings) throws IOException, ClassNotFoundException, ExitException {
		AVM avm = null;
		try {
			// key = Keyword, value = JawkExtension
			Map<String, JawkExtension> extensions;
			if (settings.isUserExtensions()) {
				extensions = getJawkExtensions();
				LOG.trace("user extensions = {}", extensions.keySet());
			} else {
				extensions = Collections.emptyMap();
				LOG.trace("user extensions not enabled");
			}

			AwkTuples tuples = new AwkTuples();
			// to be defined below

			List<ScriptSource> notIntermediateScriptSources = new ArrayList<ScriptSource>(settings.getScriptSources().size());
			for (ScriptSource scriptSource : settings.getScriptSources()) {
				if (scriptSource.isIntermediate()) {
					// read the intermediate file, bypassing frontend processing
					// if several intermediate files are supplied, the most recent one
					// encountered in the list takes precedence
					tuples = (AwkTuples) readObjectFromInputStream(scriptSource.getInputStream());
				} else {
					notIntermediateScriptSources.add(scriptSource);
				}
			}
			if (!notIntermediateScriptSources.isEmpty()) {
				AwkParser parser = new AwkParser(
						settings.isAdditionalFunctions(),
						settings.isAdditionalTypeFunctions(),
						extensions);
				// parse the script
				AwkSyntaxTree ast = parser.parse(notIntermediateScriptSources);

				if (settings.isDumpSyntaxTree()) {
					// dump the syntax tree of the script to a file
					String filename = settings.getOutputFilename("syntax_tree.lst");
					LOG.info("writing to '{}'", filename);
					PrintStream ps = new PrintStream(new FileOutputStream(filename), false, StandardCharsets.UTF_8.name());
					if (ast != null) {
						ast.dump(ps);
					}
					ps.close();
					return;
				}
				// otherwise, attempt to traverse the syntax tree and build
				// the intermediate code
				if (ast != null) {
					// 1st pass to tie actual parameters to back-referenced formal parameters
					ast.semanticAnalysis();
					// 2nd pass to tie actual parameters to forward-referenced formal parameters
					ast.semanticAnalysis();
					// build tuples
					int result = ast.populateTuples(tuples);
					// ASSERTION: NOTHING should be left on the operand stack ...
					assert result == 0;
					// Assign queue.next to the next element in the queue.
					// Calls touch(...) per Tuple so that addresses can be normalized/assigned/allocated
					tuples.postProcess();
					// record global_var -> offset mapping into the tuples
					// so that the interpreter/compiler can assign variables
					// on the "file list input" command line
					parser.populateGlobalVariableNameToOffsetMappings(tuples);
				}
				if (settings.isWriteIntermediateFile()) {
					// dump the intermediate code to an intermediate code file
					String filename = settings.getOutputFilename("a.ai");
					LOG.info("writing to '{}'", filename);
					writeObjectToFile(tuples, filename);
					return;
				}
			}
			if (settings.isDumpIntermediateCode()) {
				// dump the intermediate code to a human-readable text file
				String filename = settings.getOutputFilename("avm.lst");
				LOG.info("writing to '{}'", filename);
				PrintStream ps = new PrintStream(new FileOutputStream(filename), false, StandardCharsets.UTF_8.name());
				tuples.dump(ps);
				ps.close();
				return;
			}

			// interpret!
			avm = new AVM(settings, extensions);
			avm.interpret(tuples);
		} finally {
			if (avm != null) {
				avm.waitForIO();
			}
		}
	}

	/**
	 * Executes the specified AWK script against the given input and returns the
	 * printed output as a {@link String}.
	 *
	 * @param script AWK script to execute
	 * @param input text to process
	 * @return result of the execution as a String
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static String run(String script, String input)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		run(script, input, out);
		return out.toString(StandardCharsets.UTF_8.name());
	}

	/**
	 * Executes the specified AWK script against the given input and writes the
	 * result to the provided {@link OutputStream}.
	 *
	 * @param script AWK script to execute
	 * @param input text to process
	 * @param output destination for the printed output
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static void run(String script, String input, OutputStream output)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		run(new StringReader(script), toInputStream(input), output, true);
	}

	/**
	 * Executes the specified AWK script against the given input and returns the
	 * printed output as a {@link String}.
	 *
	 * @param script AWK script to execute (as a {@link Reader})
	 * @param input text to process
	 * @return result of the execution as a String
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static String run(Reader script, String input)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		run(script, input, out);
		return out.toString(StandardCharsets.UTF_8.name());
	}

	/**
	 * Executes the specified AWK script against the given input and writes the
	 * result to the provided {@link OutputStream}.
	 *
	 * @param script AWK script to execute (as a {@link Reader})
	 * @param input text to process
	 * @param output destination for the printed output
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static void run(Reader script, String input, OutputStream output)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		run(script, toInputStream(input), output, true);
	}

	/**
	 * Executes the specified AWK script against the given input and returns the
	 * printed output as a {@link String}.
	 *
	 * @param script AWK script to execute
	 * @param input text reader to process
	 * @return result of the execution as a String
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static String run(String script, Reader input)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		run(script, input, out);
		return out.toString(StandardCharsets.UTF_8.name());
	}

	/**
	 * Executes the specified AWK script against the given input and writes the
	 * result to the provided {@link OutputStream}.
	 *
	 * @param script AWK script to execute
	 * @param input text reader to process
	 * @param output destination for the printed output
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static void run(String script, Reader input, OutputStream output)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		run(new StringReader(script), toInputStream(input), output, true);
	}

	/**
	 * Executes the specified AWK script against the given input and returns the
	 * printed output as a {@link String}.
	 *
	 * @param script AWK script to execute (as a {@link Reader})
	 * @param input text reader to process
	 * @return result of the execution as a String
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static String run(Reader script, Reader input)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		run(script, input, out);
		return out.toString(StandardCharsets.UTF_8.name());
	}

	/**
	 * Executes the specified AWK script against the given input and writes the
	 * result to the provided {@link OutputStream}.
	 *
	 * @param script AWK script to execute (as a {@link Reader})
	 * @param input text reader to process
	 * @param output destination for the printed output
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static void run(Reader script, Reader input, OutputStream output)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		run(script, toInputStream(input), output, true);
	}

	/**
	 * Executes the specified AWK script against the given input file and returns
	 * the printed output as a {@link String}.
	 *
	 * @param script AWK script to execute
	 * @param input file containing text to process
	 * @return result of the execution as a String
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static String run(String script, File input)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		try (InputStream in = new FileInputStream(input)) {
			return run(script, in);
		}
	}

	/**
	 * Executes the specified AWK script against the given input file and writes
	 * the printed output to the provided {@link OutputStream}.
	 *
	 * @param script AWK script to execute
	 * @param input file containing text to process
	 * @param output destination for the printed output
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static void run(String script, File input, OutputStream output)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		try (InputStream in = new FileInputStream(input)) {
			run(script, in, output);
		}
	}

	/**
	 * Executes the specified AWK script against the given input file and returns
	 * the printed output as a {@link String}.
	 *
	 * @param script AWK script to execute (as a {@link Reader})
	 * @param input file containing text to process
	 * @return result of the execution as a String
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static String run(Reader script, File input)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		try (InputStream in = new FileInputStream(input)) {
			return run(script, in);
		}
	}

	/**
	 * Executes the specified AWK script against the given input file and writes
	 * the printed output to the provided {@link OutputStream}.
	 *
	 * @param script AWK script to execute (as a {@link Reader})
	 * @param input file containing text to process
	 * @param output destination for the printed output
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static void run(Reader script, File input, OutputStream output)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		try (InputStream in = new FileInputStream(input)) {
			run(script, in, output);
		}
	}

	/**
	 * Executes the specified AWK script against the provided input stream and
	 * returns the printed output as a {@link String}.
	 *
	 * @param script AWK script to execute
	 * @param input stream to process
	 * @return result of the execution as a String
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static String run(String script, InputStream input)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		run(script, input, out);
		return out.toString(StandardCharsets.UTF_8.name());
	}

	/**
	 * Executes the specified AWK script against the provided input stream and
	 * writes the result to the given {@link OutputStream}.
	 *
	 * @param script AWK script to execute
	 * @param input stream to process
	 * @param output destination for the printed output
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static void run(String script, InputStream input, OutputStream output)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		run(new StringReader(script), input, output, false);
	}

	/**
	 * Executes the specified AWK script against the provided input stream and
	 * returns the printed output as a {@link String}.
	 *
	 * @param script AWK script to execute (as a {@link Reader})
	 * @param input stream to process
	 * @return result of the execution as a String
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static String run(Reader script, InputStream input)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		run(script, input, out);
		return out.toString(StandardCharsets.UTF_8.name());
	}

	/**
	 * Executes the specified AWK script against the provided input stream and
	 * writes the result to the given {@link OutputStream}.
	 *
	 * @param script AWK script to execute (as a {@link Reader})
	 * @param input stream to process
	 * @param output destination for the printed output
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException if intermediate code cannot be loaded
	 * @throws ExitException if the script terminates with a non-zero exit code
	 */
	public static void run(Reader script, InputStream input, OutputStream output)
			throws IOException,
			ClassNotFoundException,
			ExitException {
		run(script, input, output, false);
	}

	/**
	 * Internal method that configures default {@link AwkSettings} and executes
	 * the AWK script.
	 */
	private static void run(
			Reader scriptReader,
			InputStream inputStream,
			OutputStream outputStream,
			boolean textInput)
			throws IOException,
			ClassNotFoundException,
			ExitException {

		AwkSettings settings = new AwkSettings();
		if (inputStream != null) {
			settings.setInput(inputStream);
		}

		if (textInput) {
			settings.setDefaultRS("\n");
			settings.setDefaultORS("\n");
		}

		settings
				.setOutputStream(
						new PrintStream(
								outputStream,
								false,
								StandardCharsets.UTF_8.name()));

		settings
				.addScriptSource(
						new ScriptSource(
								ScriptSource.DESCRIPTION_COMMAND_LINE_SCRIPT,
								scriptReader,
								false));

		Awk awk = new Awk();
		try {
			awk.invoke(settings);
		} catch (ExitException e) {
			if (e.getCode() != 0) {
				throw e;
			}
		}
	}

	/**
	 * Compile an expression to evaluate (not a full script)
	 * <p>
	 *
	 * @param expression AWK expression to compile to AwkTuples
	 * @param extensions Extensions that can be used in the expression
	 * @return AwkTuples to be interpreted by AVM
	 * @throws IOException if anything goes wrong with the compilation
	 */
	public static AwkTuples compileForEval(String expression, Map<String, JawkExtension> extensions) throws IOException {

		// Create a ScriptSource
		ScriptSource expressionSource = new ScriptSource(
				ScriptSource.DESCRIPTION_COMMAND_LINE_SCRIPT,
				new StringReader(expression),
				false);

		// Parse the expression
		AwkParser parser = new AwkParser(false, false, extensions);
		AwkSyntaxTree ast = parser.parseExpression(expressionSource);

		// Create the tuples that we will return
		AwkTuples tuples = new AwkTuples();

		// Attempt to traverse the syntax tree and build
		// the intermediate code
		if (ast != null) {
			// 1st pass to tie actual parameters to back-referenced formal parameters
			ast.semanticAnalysis();
			// 2nd pass to tie actual parameters to forward-referenced formal parameters
			ast.semanticAnalysis();
			// build tuples
			ast.populateTuples(tuples);
			// Calls touch(...) per Tuple so that addresses can be normalized/assigned/allocated
			tuples.postProcess();
			// record global_var -> offset mapping into the tuples
			// so that the interpreter can assign variables
			parser.populateGlobalVariableNameToOffsetMappings(tuples);
		}

		return tuples;
	}

	/**
	 * Evaluates the specified AWK expression (not a full script, just an expression)
	 * and returns the value of this expression.
	 * <p>
	 *
	 * @param expression Expression to evaluate (e.g. <code>2+3</code>)
	 * @return the value of the specified expression
	 * @throws IOException if anything goes wrong with the evaluation
	 */
	public static Object eval(String expression) throws IOException {
		return eval(expression, null, null, Collections.emptyMap());
	}

	/**
	 * Evaluates the specified AWK expression (not a full script, just an expression)
	 * and returns the value of this expression.
	 * <p>
	 *
	 * @param expression Expression to evaluate (e.g. <code>2+3</code> or <code>$2 "-" $3</code>
	 * @param input Optional text input (that will be available as $0, and tokenized as $1, $2, etc.)
	 * @return the value of the specified expression
	 * @throws IOException if anything goes wrong with the evaluation
	 */
	public static Object eval(String expression, String input) throws IOException {
		return eval(expression, input, null, Collections.emptyMap());
	}

	/**
	 * Evaluates the specified AWK expression (not a full script, just an expression)
	 * and returns the value of this expression.
	 * <p>
	 *
	 * @param expression Expression to evaluate (e.g. <code>2+3</code> or <code>$2 "-" $3</code>
	 * @param input Optional text input (that will be available as $0, and tokenized as $1, $2, etc.)
	 * @param fieldSeparator Value of the FS global variable used for parsing the input
	 * @return the value of the specified expression
	 * @throws IOException if anything goes wrong with the evaluation
	 */
	public static Object eval(String expression, String input, String fieldSeparator) throws IOException {
		return eval(expression, input, fieldSeparator, Collections.emptyMap());
	}

	/**
	 * Evaluates the specified AWK expression (not a full script, just an expression)
	 * and returns the value of this expression.
	 * <p>
	 *
	 * @param expression Expression to evaluate (e.g. <code>2+3</code> or <code>$2 "-" $3</code>
	 * @param input Optional text input (that will be available as $0, and tokenized as $1, $2, etc.)
	 * @param extensions Extensions that can be used in the expression
	 * @return the value of the specified expression
	 * @throws IOException if anything goes wrong with the evaluation
	 */
	public static Object eval(String expression, String input, Map<String, JawkExtension> extensions)
			throws IOException {
		return eval(expression, input, null, extensions);
	}

	/**
	 * Evaluates the specified AWK expression (not a full script, just an expression)
	 * and returns the value of this expression.
	 * <p>
	 *
	 * @param expression Expression to evaluate (e.g. <code>2+3</code> or <code>$2 "-" $3</code>
	 * @param input Optional text input (that will be available as $0, and tokenized as $1, $2, etc.)
	 * @param fieldSeparator Value of the FS global variable used for parsing the input
	 * @param extensions Extensions that can be used in the expression
	 * @return the value of the specified expression
	 * @throws IOException if anything goes wrong with the evaluation
	 */
	public static Object eval(
			String expression,
			String input,
			String fieldSeparator,
			Map<String, JawkExtension> extensions)
			throws IOException {
		return eval(compileForEval(expression, Collections.emptyMap()), input, fieldSeparator, extensions);
	}

	/**
	 * Evaluates the specified AWK tuples, i.e. the result of the execution of the
	 * TERNARY_EXPRESSION AST (the value that has been pushed in the stack).
	 * <p>
	 *
	 * @param tuples Tuples returned by {@link Awk#compileForEval(String, Map)}
	 * @param input Optional text input (that will be available as $0, and tokenized as $1, $2, etc.)
	 * @param fieldSeparator Value of the FS global variable used for parsing the input
	 * @param extensions Extensions that can be used in the expression
	 * @return the value of the specified expression
	 * @throws IOException if anything goes wrong with the evaluation
	 */
	public static Object eval(
			AwkTuples tuples,
			String input,
			String fieldSeparator,
			Map<String, JawkExtension> extensions)
			throws IOException {

		AwkSettings settings = new AwkSettings();
		if (input != null) {
			settings.setInput(toInputStream(input));
		} else {
			settings.setInput(toInputStream(""));
		}

		settings.setDefaultRS("\n");
		settings.setDefaultORS("\n");
		settings.setFieldSeparator(fieldSeparator);

		settings
				.setOutputStream(
						new PrintStream(new ByteArrayOutputStream(), false, StandardCharsets.UTF_8.name()));

		AVM avm = new AVM(settings, extensions);
		return avm.eval(tuples, input);
	}

	/**
	 * Converts a text input into an {@link InputStream} using UTF-8 encoding.
	 */
	private static InputStream toInputStream(String input) {
		if (input == null) {
			return new ByteArrayInputStream(new byte[0]);
		}
		return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Reads all characters from the supplied {@link Reader} and returns an
	 * {@link InputStream} containing the same data using UTF-8 encoding.
	 */
	private static InputStream toInputStream(Reader reader) throws IOException {
		if (reader == null) {
			return new ByteArrayInputStream(new byte[0]);
		}
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[4096];
		int len;
		while ((len = reader.read(buf)) != -1) {
			sb.append(buf, 0, len);
		}
		return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
	}

	private static Object readObjectFromInputStream(InputStream is) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(is);
		Object retval = ois.readObject();
		ois.close();
		return retval;
	}

	private static void writeObjectToFile(Object object, String filename) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename));
		oos.writeObject(object);
		oos.close();
	}

	private static Map<String, JawkExtension> getJawkExtensions() {
		String extensionsStr = System.getProperty("jawk.extensions", null);
		if (extensionsStr == null) {
			// return Collections.emptyMap();
			extensionsStr = DEFAULT_EXTENSIONS;
		} else {
			extensionsStr = DEFAULT_EXTENSIONS + "#" + extensionsStr;
		}

		// use reflection to obtain extensions

		Set<Class<?>> extensionClasses = new HashSet<Class<?>>();
		Map<String, JawkExtension> retval = new HashMap<String, JawkExtension>();

		StringTokenizer st = new StringTokenizer(extensionsStr, "#");
		while (st.hasMoreTokens()) {
			String cls = st.nextToken();
			LOG.trace("cls = {}", cls);
			try {
				Class<?> c = Class.forName(cls);
				// check if it's a JawkExtension
				if (!JawkExtension.class.isAssignableFrom(c)) {
					throw new ClassNotFoundException(cls + " does not implement JawkExtension");
				}
				if (extensionClasses.contains(c)) {
					LOG.warn("class {} is multiple times referred in extension class list. Skipping.", cls);
					continue;
				} else {
					extensionClasses.add(c);
				}

				// it is...
				// create a new instance and put it here
				try {
					Constructor<?> constructor = c.getDeclaredConstructor(); // Default constructor
					JawkExtension ji = (JawkExtension) constructor.newInstance();
					String[] keywords = ji.extensionKeywords();
					for (String keyword : keywords) {
						if (retval.get(keyword) != null) {
							throw new IllegalArgumentException(
									"keyword collision : "
											+ keyword
											+ " for both "
											+ retval.get(keyword).getExtensionName()
											+ " and "
											+ ji.getExtensionName());
						}
						retval.put(keyword, ji);
					}
				} catch (
						InstantiationException
						| IllegalAccessException
						| NoSuchMethodException
						| SecurityException
						| IllegalArgumentException
						| InvocationTargetException e) {
					LOG.warn("Cannot instantiate " + c.getName(), e);
				}
			} catch (ClassNotFoundException cnfe) {
				LOG.warn("Cannot classload {} : {}", new Object[] { cls, cnfe });
			}
		}

		return retval;
	}
}
