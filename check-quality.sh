#!/bin/bash
# check-quality.sh
# Run all code-quality gates locally in one shot.
# Mirrors what the CI "code-quality" job does.
#
# Usage:
#   ./check-quality.sh            # full quality check
#   ./check-quality.sh --no-tests # skip tests (useful after a full test run)

set -euo pipefail

SKIP_TESTS=false
for arg in "$@"; do
  [[ "$arg" == "--no-tests" ]] && SKIP_TESTS=true
done

SKIP_FLAG=""
$SKIP_TESTS && SKIP_FLAG="-DskipTests=true"

echo "═══════════════════════════════════════════════════"
echo "  SmartKoszyka Microservices — Quality Check"
echo "═══════════════════════════════════════════════════"

run() {
  local label=$1; shift
  echo ""
  echo "▶  ${label}…"
  "$@"
}

EXIT_CHECKSTYLE=0
EXIT_SPOTBUGS=0
EXIT_PMD=0
EXIT_TESTS=0
EXIT_COVERAGE=0

run "Checkstyle"  ./mvnw checkstyle:check               || EXIT_CHECKSTYLE=$?
run "SpotBugs"    ./mvnw spotbugs:check    -P quality    || EXIT_SPOTBUGS=$?
run "PMD"         ./mvnw pmd:check         -P quality    || EXIT_PMD=$?

if ! $SKIP_TESTS; then
  run "Tests + JaCoCo report" ./mvnw clean test jacoco:report || EXIT_TESTS=$?
fi

run "JaCoCo coverage gate" ./mvnw jacoco:check           || EXIT_COVERAGE=$?

echo ""
echo "═══════════════════════════════════════════════════"
echo "  Summary"
echo "═══════════════════════════════════════════════════"
echo "  Checkstyle : $([ $EXIT_CHECKSTYLE -eq 0 ] && echo '🟢 PASSED' || echo '🛑 FAILED')"
echo "  SpotBugs   : $([ $EXIT_SPOTBUGS   -eq 0 ] && echo '🟢 PASSED' || echo '🛑 FAILED')"
echo "  PMD        : $([ $EXIT_PMD        -eq 0 ] && echo '🟢 PASSED' || echo '🛑 FAILED')"
echo "  Tests      : $([ $EXIT_TESTS      -eq 0 ] && echo '🟢 PASSED' || echo '🛑 FAILED')"
echo "  Coverage   : $([ $EXIT_COVERAGE   -eq 0 ] && echo '🟢 PASSED' || echo '🛑 FAILED')"
echo ""
echo "  Reports:"
echo "    JaCoCo  → target/site/jacoco/index.html"
echo "    SpotBugs→ **/target/spotbugs/spotbugsXml.xml"
echo "    PMD     → **/target/pmd/pmd.xml"
echo "═══════════════════════════════════════════════════"

TOTAL=$((EXIT_CHECKSTYLE + EXIT_SPOTBUGS + EXIT_PMD + EXIT_TESTS + EXIT_COVERAGE))
if [ "$TOTAL" -ne 0 ]; then
  echo "🛑 One or more checks FAILED. See output above."
  exit 1
else
  echo "🟢 All checks PASSED — ready to push! 🎉"
  exit 0
fi
