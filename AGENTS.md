# Instructions for AI Agents

## Code style

Your never need to worry about code formatting at all. Simply run `mvn formatter:format` before committing changes to make sure the new code follows this project's code formatting rules.

## Build

The project uses Maven to build. Use `mvn` with the `--offline` option if there is no Internet connection available.

@codex, please don't try to use `mvnw` (Maven Wrapper) or to install Maven since you don't have access to the Internet. Maven is already installed and runs perfectly well. All dependencies and plugins have been downloaded and installed in the local repository during the environment setup with a complete build (`mvn verify` and `mvn site`). You can run all Maven commands offline.

## Test

All tests must pass with `mvn test`. Don't use the `-q` (silent) option, as you want to see the result of successful tests.

## Code quality reports

Run `mvn site` to produce all the required code style reports (PMD, CPD, checkstyle, and spotbugs). Make sure the reports don't show new errors and problems because of your changes.
