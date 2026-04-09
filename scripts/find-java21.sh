#!/bin/bash
# scripts/find-java21.sh
# Prints the JAVA_HOME path for Java 21 to stdout.
# Usage:  export JAVA_HOME=$(scripts/find-java21.sh)

for candidate in \
  /usr/lib/jvm/java-21-openjdk-amd64 \
  /usr/lib/jvm/java-21-openjdk \
  /usr/lib/jvm/temurin-21 \
  "$HOME/.sdkman/candidates/java/current"
do
  if [ -d "$candidate" ]; then
    echo "$candidate"
    exit 0
  fi
done

# Fall back to whatever 'java' is on PATH
echo "$(dirname "$(dirname "$(readlink -f "$(which java)")")")"
