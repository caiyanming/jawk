# Instructions for AI Agents

## Code style

We use tabs here, as sane people.

## Build

The project uses Maven to build. Use `mvn` with the `--offline` option if there is no Internet connection available.

@codex, please don't try to use mvnw or to install Maven since you don't have access to the Internet. Maven is already installed and runs perfectly well. All dependencies and plugins have been downloaded and installed in the local repository during the environment setup with a complete build (`mvn verify` and `mvn site`). You can run all Maven commands offline.

## Test

All tests must pass with `mvn test`.

## Code quality reports

Run `mvn site` to produce all the required code style reports (PMD, CPD, checkstyle, and spotbugs). Make sure the reports don't show new errors and problems because of your changes.
