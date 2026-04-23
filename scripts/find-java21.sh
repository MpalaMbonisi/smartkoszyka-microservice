#!/bin/bash
# scripts/find-java21.sh
# Optimized specifically for Linux Fedora

set -euo pipefail

# ── 1. Check current JAVA_HOME ───────────────────────────────────────────────
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    version=$("$JAVA_HOME/bin/java" -version 2>&1 | head -n 1)
    if [[ "$version" == *' "21'* ]]; then
        echo "$JAVA_HOME"
        exit 0
    fi
fi

# ── 2. Fedora System Paths ──────────────────────────────────────────
for candidate in \
    "/usr/lib/jvm/java-21-openjdk" \
    "/usr/lib/jvm/java-21" \
    "$HOME/.local/share/mise/installs/java/21"* \
    "/usr/lib/jvm/temurin-21"
do
    # Use -d and check for the binary to validate the directory
    if [ -d "$candidate" ] && [ -x "$candidate/bin/java" ]; then
        echo "$candidate"
        exit 0
    fi
done

# Fall back to whatever 'java' is on PATH
echo "$(dirname "$(dirname "$(readlink -f "$(which java)")")")"


# ── 3. Error ─────────────────────────────────────────────────────────────────
echo "ERROR: Java 21 not found." >&2
echo "Run: 'sudo dnf install java-21-openjdk-devel'" >&2
exit 1
