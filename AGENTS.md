# Instructions for AI Agents

## Code format

Your never need to worry about code formatting at all. Simply run `mvn formatter:format` before committing changes to make sure the new code follows this project's code formatting rules.

All files must include the proper license header. When you add a new file, make sure to include the proper license header by running the `mvn license:update-file-header` command before committing (or even before trying the build and test, since the build will fail if a file doesn't include the proper license header).

## Build

The project uses Maven to build. A full build is performed with `mvn verify site` (or `mvn clean verify site` when applicable).

@codex, please don't try to use `mvnw` (Maven Wrapper). Maven is already installed and runs perfectly well.

## Test

Whenever required, when you add code or when you modify code that is not covered with unit tests, add the corresponding unit tests. All tests must pass with `mvn test`. Don't use the `-q` (silent) option, as you want to see the result of successful tests. Tests are run with the Maven surefire plugin and results are stored in the ./target/surefire-reports directory.

Compatibility tests are run with `mvn verify` to assess the compatibility with other implementations of AWK. These tests are run with the Maven failsafe plugin and results are stored in the ./target/failsafe-reports directory.

## Code quality reports

Code quality checks are performed during the build with `mvn verify` (checkstyle, pmd, and spotbugs). Fix any problem reported here before committing and submitting your code!

## Documentation

Any change that affects the end user of Jawk must be properly documented in README.md and src/site/*.md.
