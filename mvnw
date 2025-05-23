#!/bin/sh

# Simple Maven Wrapper script

set -e

BASEDIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_DIR="$BASEDIR/.mvn/wrapper"
PROPERTIES="$WRAPPER_DIR/maven-wrapper.properties"
JAR="$WRAPPER_DIR/maven-wrapper.jar"

if [ -f "$PROPERTIES" ]; then
  . "$PROPERTIES"
fi

if [ ! -f "$JAR" ]; then
  echo "Downloading Maven wrapper jar..." >&2
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL -o "$JAR" "$wrapperUrl"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$JAR" "$wrapperUrl"
  else
    echo "Error: neither curl nor wget is installed" >&2
    exit 1
  fi
fi

exec java $MAVEN_OPTS -jar "$JAR" "$@"
