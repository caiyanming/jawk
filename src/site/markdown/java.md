# AWK in Java

<!-- MACRO{toc|fromDepth=1|toDepth=2|id=toc} -->

## Getting Started

Add Jawk in the list of dependencies in your [Maven **pom.xml**](https://maven.apache.org/pom.html):

```xml
<dependencies>
  <!-- [...] -->
  <dependency>
    <groupId>org.metricshub</groupId>
    <artifactId>jawk</artifactId>
    <version>${project.version}</version>
  </dependency>
</dependencies>
```

Jawk artifacts are published on Maven Central, so the dependency can be resolved automatically by most build tools.

## Examples

### Invoke AWK scripts files on input files

```java
/**
 * Executes the specified AWK script
 * <p>
 * @param scriptFile File containing the AWK script to execute
 * @param inputFileList List of files that contain the input to be parsed by the AWK script
 * @return the printed output of the script as a String
 * @throws ExitException when the AWK script forces its exit with a specified code
 * @throws IOException on I/O problems
 */
private String runAwk(File scriptFile, List<String> inputFileList) throws IOException, ExitException {

    AwkSettings settings = new AwkSettings();

    // Set the input files
    settings.getNameValueOrFileNames().addAll(inputFileList);

    // Create the OutputStream, to collect the result as a String
    ByteArrayOutputStream resultBytesStream = new ByteArrayOutputStream();
    settings.setOutputStream(new PrintStream(resultBytesStream));

    // Sets the AWK script to execute
    settings.addScriptSource(new ScriptFileSource(scriptFile.getAbsolutePath()));

    // Execute the awk script against the specified input
    Awk awk = new Awk();
    awk.invoke(settings);

    // Return the result as a string
    return resultBytesStream.toString(StandardCharsets.UTF_8);

}
```

### Execute AWK script (as String) on String input

```java
/**
 * Executes the specified script against the specified input
 * <p>
 * @param script AWK script to execute (as a String)
 * @param input Text to process (as a String)
 * @return result as a String
 * @throws ExitException when the AWK script forces its exit with a specified code
 * @throws IOException on I/O problems
 */
private String runAwk(String script, String input) throws IOException, ExitException {

    AwkSettings settings = new AwkSettings();

    // Set the input files
    settings.setInput(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

    // We force \n as the Record Separator (RS) because even if running on Windows
    // we're passing Java strings, where end of lines are simple \n
    settings.setDefaultRS("\n");

    // Create the OutputStream, to collect the result as a String
    ByteArrayOutputStream resultBytesStream = new ByteArrayOutputStream();
    settings.setOutputStream(new PrintStream(resultBytesStream));

    // Sets the AWK script to execute
    settings.addScriptSource(new ScriptSource("Body", new StringReader(script), false));

    // Execute the awk script against the specified input
    Awk awk = new Awk();
    awk.invoke(settings);

    // Return the result as a string
    return resultBytesStream.toString(StandardCharsets.UTF_8);

}
```

## Javadoc

* [AwkSettings](apidocs/org/metricshub/jawk/util/AwkSettings.html)
* [Awk](apidocs/org/metricshub/jawk/Awk.html)

## Java Scripting API (JSR 223)

**Jawk** can be invoked via the standard Java scripting framework introduced in JSR&nbsp;223.
The following example loads Jawk through the `ScriptEngineManager` and evaluates
an AWK script from a Java `String`:

```java
ScriptEngineManager manager = new ScriptEngineManager();
ScriptEngine engine = manager.getEngineByName("jawk");

String script = "{ print toupper($0) }";
String input = "hello world";

Bindings bindings = engine.createBindings();
bindings.put("input", new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

StringWriter result = new StringWriter();
engine.getContext().setWriter(new PrintWriter(result));

engine.eval(script, bindings);

System.out.println(result.toString());
```

## Limitations and Differences

When embedding Jawk into an application remember that the interpreter follows
the AWK language closely but not everything from other implementations is
available.  The most notable differences are:

* Regular expressions use Java's implementation and therefore have slightly
  different semantics compared to traditional AWK.
* `printf`/`sprintf` formatting relies on `java.util.Formatter`.  Unexpected
  argument types will raise an exception unless the `_INTEGER`, `_DOUBLE` or
  `_STRING` helpers are enabled with the `-y` option or their equivalents in
  `AwkSettings`.
* Extensions must be explicitly enabled.  Only the core extensions bundled with
  Jawk are available by default.

For a more complete list see the [project overview](index.html#features).
