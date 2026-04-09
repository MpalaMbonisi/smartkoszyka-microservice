#!/bin/bash
# scripts/mvnw-java21.sh
# Wrapper that ensures Maven always runs with Java 21,
# regardless of the shell's current JAVA_HOME.
# Used by pre-commit hooks.

set -euo pipefail

PROJECT_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
cd "$PROJECT_ROOT"

# Locate Java 21 and set JAVA_HOME accordingly.
if [ -z "${JAVA_HOME:-}" ]; then
  for candidate in \
    /usr/lib/jvm/java-21-openjdk-amd64 \
    /usr/lib/jvm/java-21-openjdk \
    /usr/lib/jvm/temurin-21 \
    "$HOME/.sdkman/candidates/java/current"
  do
    if [ -d "$candidate" ]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi

if [ -z "${JAVA_HOME:-}" ]; then
  export JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$(which java)")")")
fi

echo "► mvnw-java21: JAVA_HOME=${JAVA_HOME}"
java -version

exec ./mvnw "$@"
