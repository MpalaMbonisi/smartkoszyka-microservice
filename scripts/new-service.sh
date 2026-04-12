#!/usr/bin/env bash
# =============================================================================
# scripts/create-service.sh
#
# Creates a new Spring Boot microservice module under services/
# with a consistent structure, templated pom.xml, Dockerfile,
# application.properties, and a @SpringBootApplication main class.
#
# Usage:
#   ./scripts/create-service.sh <service-name> [port]
#
# Examples:
#   ./scripts/create-service.sh auth-service 8081
#   ./scripts/create-service.sh product-service 8082
#   ./scripts/create-service.sh shopping-list-service 8083
#
# Naming rules:
#   - lowercase letters, digits, and hyphens only
#   - must end in "-service" (enforced to keep module names consistent)
#   - e.g. "auth-service", "product-service", "shopping-list-service"
# =============================================================================

set -euo pipefail

# ── Colour helpers ────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

info()    { echo -e "${CYAN}  ▸ $*${RESET}"; }
success() { echo -e "${GREEN}  ✔ $*${RESET}"; }
warn()    { echo -e "${YELLOW}  ⚠ $*${RESET}"; }
error()   { echo -e "${RED}  ✘ $*${RESET}" >&2; }
die()     { error "$*"; exit 1; }

# ── Locate repo root (works from any subdirectory) ────────────────────────────
REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null) \
  || die "Not inside a git repository. Please run from within the project."
cd "$REPO_ROOT"

TEMPLATES_DIR="$REPO_ROOT/templates"
SCRIPTS_DIR="$REPO_ROOT/scripts"

# ── Argument validation ───────────────────────────────────────────────────────
if [[ $# -lt 1 ]]; then
  echo ""
  echo -e "${BOLD}Usage:${RESET}  ./scripts/create-service.sh <service-name> [port]"
  echo ""
  echo "  service-name  lowercase, hyphens allowed, must end in '-service'"
  echo "  port          optional HTTP port (default: next available from 8081)"
  echo ""
  echo "Examples:"
  echo "  ./scripts/create-service.sh auth-service 8081"
  echo "  ./scripts/create-service.sh product-service 8082"
  echo ""
  exit 1
fi

SERVICE_NAME="$1"
PORT="${2:-}"
MANAGEMENT_PORT=""$((PORT + 100))"  # e.g. 8081 → 8181 for management/actuator

# ── Validate service name ─────────────────────────────────────────────────────
if [[ ! "$SERVICE_NAME" =~ ^[a-z][a-z0-9-]*-service$ ]]; then
  die "Invalid service name '${SERVICE_NAME}'.
       Must match: ^[a-z][a-z0-9-]*-service\$
       Examples: auth-service, product-service, shopping-list-service"
fi

# ── Derive names from SERVICE_NAME ────────────────────────────────────────────
# auth-service           → AUTH_SERVICE (env var style)
# auth-service           → authService  (camelCase for class prefix)
# auth-service           → AuthService  (PascalCase for class names)
# com...auth_service     → Java package (hyphens → underscores NOT used;
#                          hyphens stripped, each word lowercased and joined)

# Java package segment: strip "-service" suffix, replace remaining hyphens with dots
# e.g. shopping-list-service → shopping.list  → package: ...smartkoszyka.shoppinglist
SERVICE_STRIPPED="${SERVICE_NAME%-service}"                          # auth | shopping-list
PACKAGE_SEGMENT="${SERVICE_STRIPPED//-/}"                            # auth | shoppinglist
JAVA_PACKAGE="com.github.mpalambonisi.${PACKAGE_SEGMENT}"          # com.github...auth

# PascalCase class prefix: capitalise each hyphen-separated word
PASCAL_NAME=""
IFS='-' read -ra PARTS <<< "$SERVICE_STRIPPED"
for part in "${PARTS[@]}"; do
  PASCAL_NAME+="${part^}"
done
# e.g. Auth | ShoppingList
MAIN_CLASS_NAME="${PASCAL_NAME}ServiceApplication"                   # AuthServiceApplication

# Artifact id used in pom.xml (keep hyphens — valid Maven artifact id)
ARTIFACT_ID="$SERVICE_NAME"

# ── Resolve port ──────────────────────────────────────────────────────────────
if [[ -z "$PORT" ]]; then
  # Auto-detect next free port starting at 8081
  USED_PORTS=$(grep -rh "server.port=" services/*/src/main/resources/application.properties \
                 2>/dev/null | grep -oP '\d+' | sort -n || true)
  PORT=8081
  while echo "$USED_PORTS" | grep -q "^${PORT}$"; do
    PORT=$((PORT + 1))
  done
  warn "No port specified — assigned port ${PORT} (next available)"
fi

# ── Check port is numeric and in range ───────────────────────────────────────
if ! [[ "$PORT" =~ ^[0-9]+$ ]] || (( PORT < 1024 || PORT > 65535 )); then
  die "Port must be a number between 1024 and 65535, got: ${PORT}"
fi

# ── Guard: don't overwrite an existing service ────────────────────────────────
SERVICE_DIR="$REPO_ROOT/services/$SERVICE_NAME"
if [[ -d "$SERVICE_DIR" ]]; then
  die "Service directory already exists: services/${SERVICE_NAME}
       Delete it first if you want to regenerate."
fi

# ── Template substitution helper ─────────────────────────────────────────────
# Replaces all placeholders in-place inside a file.
# Tokens follow the {{TOKEN}} convention so they can't accidentally
# collide with real XML/properties content.
substitute() {
  local file="$1"
  sed -i \
    -e "s|{{SERVICE_NAME}}|${SERVICE_NAME}|g" \
    -e "s|{{ARTIFACT_ID}}|${ARTIFACT_ID}|g" \
    -e "s|{{JAVA_PACKAGE}}|${JAVA_PACKAGE}|g" \
    -e "s|{{PACKAGE_PATH}}|${PACKAGE_PATH}|g" \
    -e "s|{{MAIN_CLASS_NAME}}|${MAIN_CLASS_NAME}|g" \
    -e "s|{{PORT}}|${PORT}|g" \
    -e "s|{{PASCAL_NAME}}|${PASCAL_NAME}|g" \
    -e "s|{{MANAGEMENT_PORT}}|${MANAGEMENT_PORT}|g" \
    "$file"
}

# ── Check templates exist ─────────────────────────────────────────────────────
for tpl in \
  "$TEMPLATES_DIR/service-pom.xml" \
  "$TEMPLATES_DIR/Dockerfile" \
  "$TEMPLATES_DIR/application.properties"
do
  [[ -f "$tpl" ]] || die "Missing template: ${tpl}
       Run this script from the repository root after cloning."
done

# ── Summary before creating anything ─────────────────────────────────────────
echo ""
echo -e "${BOLD}═══════════════════════════════════════════════════${RESET}"
echo -e "${BOLD}  Creating microservice scaffold${RESET}"
echo -e "${BOLD}═══════════════════════════════════════════════════${RESET}"
echo ""
echo -e "  Service name   : ${BOLD}${SERVICE_NAME}${RESET}"
echo -e "  Artifact ID    : ${BOLD}${ARTIFACT_ID}${RESET}"
echo -e "  Java package   : ${BOLD}${JAVA_PACKAGE}${RESET}"
echo -e "  Main class     : ${BOLD}${MAIN_CLASS_NAME}${RESET}"
echo -e "  Port           : ${BOLD}${PORT}${RESET}"
echo -e "  Target dir     : ${BOLD}services/${SERVICE_NAME}${RESET}"
echo ""

# ── Create directory tree ─────────────────────────────────────────────────────
PACKAGE_PATH="${JAVA_PACKAGE//.//}"   # com/github/mpalambonisi/auth

MAIN_SRC="$SERVICE_DIR/src/main/java/$PACKAGE_PATH"
TEST_SRC="$SERVICE_DIR/src/test/java/$PACKAGE_PATH"
RESOURCES="$SERVICE_DIR/src/main/resources"
TEST_RESOURCES="$SERVICE_DIR/src/test/resources"

info "Creating directory structure…"
mkdir -p "$MAIN_SRC"
mkdir -p "$TEST_SRC"
mkdir -p "$RESOURCES"
mkdir -p "$TEST_RESOURCES"
success "Directories created"

# ── Copy & substitute templates ───────────────────────────────────────────────
info "Generating pom.xml…"
cp "$TEMPLATES_DIR/service-pom.xml" "$SERVICE_DIR/pom.xml"
substitute "$SERVICE_DIR/pom.xml"
success "pom.xml ready"

info "Generating Dockerfile…"
cp "$TEMPLATES_DIR/Dockerfile" "$SERVICE_DIR/Dockerfile"
substitute "$SERVICE_DIR/Dockerfile"
success "Dockerfile ready"

info "Generating application.properties…"
cp "$TEMPLATES_DIR/application.properties" "$RESOURCES/application.properties"
substitute "$RESOURCES/application.properties"
success "application.properties ready"

# ── Generate @SpringBootApplication main class ────────────────────────────────
info "Generating ${MAIN_CLASS_NAME}.java…"
cat > "$MAIN_SRC/${MAIN_CLASS_NAME}.java" << JAVA
package ${JAVA_PACKAGE};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/** Entry point for the ${SERVICE_NAME}. */
@SpringBootApplication
@ComponentScan(
  basePackages = {
    "${JAVA_PACKAGE}",
    "com.github.mpalambonisi.service"   // picks up common-lib beans
})
public class ${MAIN_CLASS_NAME} {

  public static void main(String[] args) {
    SpringApplication.run(${MAIN_CLASS_NAME}.class, args);
  }
}
JAVA
success "${MAIN_CLASS_NAME}.java ready"

# ── Generate placeholder integration test ────────────────────────────────────
info "Generating ${MAIN_CLASS_NAME}Tests.java…"
cat > "$TEST_SRC/${MAIN_CLASS_NAME}Tests.java" << JAVA
package ${JAVA_PACKAGE};

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ${MAIN_CLASS_NAME}Tests {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url",      postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Test
  void contextLoads() {
    // Verifies the Spring context starts successfully with a real database.
  }
}
JAVA
success "${MAIN_CLASS_NAME}Tests.java ready"

# ── Remind developer to register the module ───────────────────────────────────
echo ""
echo -e "${BOLD}═══════════════════════════════════════════════════${RESET}"
echo -e "${GREEN}${BOLD}  ✔ Service '${SERVICE_NAME}' created successfully!${RESET}"
echo -e "${BOLD}═══════════════════════════════════════════════════${RESET}"
echo ""
echo -e "${BOLD}Next steps:${RESET}"
echo ""
echo -e "  1. Register the module in the ${BOLD}root pom.xml${RESET}:"
echo -e "     Add inside <modules>:"
echo -e "     ${CYAN}<module>services/${SERVICE_NAME}</module>${RESET}"
echo ""
echo -e "  2. Enable the service in ${BOLD}docker-compose.yml${RESET}:"
echo -e "     Uncomment the '${SERVICE_NAME}' block (or add a new one)."
echo ""
echo -e "  3. Build and verify:"
echo -e "     ${CYAN}./mvnw clean verify -pl services/${SERVICE_NAME} -am${RESET}"
echo ""
echo ""
echo -e "  4. Update prometheus.yml:"
echo -e "     - job_name: '${SERVICE_NAME}'"
echo -e "       static_configs:"
echo -e "         - targets: ['${SERVICE_NAME}:${MANAGEMENT_PORT}']"
echo ""
echo -e "  5. Expose the management port in ${BOLD}docker-compose.yml${RESET}:"
echo -e "     Add the management port to the '${SERVICE_NAME}' service block."
echo -e "     ${CYAN}     - \"${MANAGEMENT_PORT}:${MANAGEMENT_PORT}\"${RESET}"
echo ""
