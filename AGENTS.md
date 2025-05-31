# Instructions for AI Agents

## Code format

Your never need to worry about code formatting at all. Simply run `mvn formatter:format` before committing changes to make sure the new code follows this project's code formatting rules.

All files must include the proper license header. When you add a new file, make sure to include the proper license header by running the `mvn license:update-file-header` command before committing (or even before trying the build and test, since the build will fail if a file doesn't include the proper license header).

## Build

The project uses Maven to build. Use `mvn` with the `--offline` option if there is no Internet connection available.

@codex, please don't try to use `mvnw` (Maven Wrapper) or to install Maven since you don't have access to the Internet. Maven is already installed and runs perfectly well. All dependencies and plugins have been downloaded and installed in the local repository during the environment setup with a complete build (`mvn verify` and `mvn site`). You can run all Maven commands offline.

A full build is performed with `mvn verify site`.

## Test

Whenever required, when you add code or when you modify code that is not covered with unit tests, add the corresponding unit tests. All tests must pass with `mvn test`. Don't use the `-q` (silent) option, as you want to see the result of successful tests. Tests are run with the Maven surefire plugin and results are stored in the ./target/surefire-reports directory.

Compatibility tests are run with `mvn verify` to assess the compatibility with other implementations of AWK. These tests are run with the Maven failsafe plugin and results are stored in the ./target/failsafe-reports directory.

## Code quality reports

Run `mvn site` to produce all the required code style reports (PMD, CPD, checkstyle, and spotbugs). Make sure the reports don't show new errors and problems because of your changes. To compare these reports with the initial build during environment setup, it is required to copy the content of these reports to a temporary location first, before they get overwritten when you execute `mvn site` or `mvn verify site`. Typically, you can copy target/*.xml to target/before/ (mkdir target/before).

If the reports show new problems comparing to the initial report, fix these before submitting your changes.

## Documentation

Any change that affects the end user of Jawk must be properly documented in README.md and src/site/*.md.
