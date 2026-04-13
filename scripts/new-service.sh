#!/usr/bin/env bash
# =============================================================================
# scripts/create-service.sh
#
# Creates a new Spring Boot microservice module under services/
# Usage: ./scripts/create-service.sh <service-name> [port]
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

# ── Locate repo root ──────────────────────────────────────────────────────────
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
  echo "  service-name  lowercase, hyphens allowed (e.g. 'api-gateway')"
  echo "  port          optional HTTP port"
  echo ""
  exit 1
fi

SERVICE_NAME="$1"
PORT="${2:-}"
MANAGEMENT_PORT="$((PORT + 100))"

# ── UPDATED: Validate service name (No "-service" requirement) ───────────────
if [[ ! "$SERVICE_NAME" =~ ^[a-z][a-z0-9-]*$ ]]; then
  die "Invalid service name '${SERVICE_NAME}'.
       Must match: ^[a-z][a-z0-9-]*\$
       Examples: api-gateway, auth-service, mailer"
fi

# ── Derive names ──────────────────────────────────────────────────────────────
# Remove "-service" only IF it exists for the package name logic
SERVICE_STRIPPED="${SERVICE_NAME%-service}"
PACKAGE_SEGMENT="${SERVICE_STRIPPED//-/}"
JAVA_PACKAGE="com.github.mpalambonisi.${PACKAGE_SEGMENT}"

# PascalCase class prefix
PASCAL_NAME=""
IFS='-' read -ra PARTS <<< "$SERVICE_STRIPPED"
for part in "${PARTS[@]}"; do
  PASCAL_NAME+="${part^}"
done
MAIN_CLASS_NAME="${PASCAL_NAME}Application" # Removed 'Service' from class name for generic feel

ARTIFACT_ID="$SERVICE_NAME"

# ── Resolve port ──────────────────────────────────────────────────────────────
if [[ -z "$PORT" ]]; then
  USED_PORTS=$(grep -rh "server.port=" services/*/src/main/resources/application.properties \
                 2>/dev/null | grep -oP '\d+' | sort -n || true)
  PORT=8081
  while echo "$USED_PORTS" | grep -q "^${PORT}$"; do
    PORT=$((PORT + 1))
  done
  warn "No port specified — assigned port ${PORT}"
fi

# ── Guard: don't overwrite ────────────────────────────────────────────────────
SERVICE_DIR="$REPO_ROOT/services/$SERVICE_NAME"
if [[ -d "$SERVICE_DIR" ]]; then
  die "Service directory already exists: services/${SERVICE_NAME}"
fi

# ── Template substitution helper ─────────────────────────────────────────────
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

# ── Check templates ───────────────────────────────────────────────────────────
for tpl in "$TEMPLATES_DIR/service-pom.xml" "$TEMPLATES_DIR/Dockerfile" "$TEMPLATES_DIR/application.properties"; do
  [[ -f "$tpl" ]] || die "Missing template: ${tpl}"
done

# ── Create structure ──────────────────────────────────────────────────────────
PACKAGE_PATH="${JAVA_PACKAGE//.//}"
MAIN_SRC="$SERVICE_DIR/src/main/java/$PACKAGE_PATH"
TEST_SRC="$SERVICE_DIR/src/test/java/$PACKAGE_PATH"
RESOURCES="$SERVICE_DIR/src/main/resources"
TEST_RESOURCES="$SERVICE_DIR/src/test/resources"

mkdir -p "$MAIN_SRC" "$TEST_SRC" "$RESOURCES" "$TEST_RESOURCES"

# ── Copy & substitute ─────────────────────────────────────────────────────────
cp "$TEMPLATES_DIR/service-pom.xml" "$SERVICE_DIR/pom.xml"
substitute "$SERVICE_DIR/pom.xml"

cp "$TEMPLATES_DIR/Dockerfile" "$SERVICE_DIR/Dockerfile"
substitute "$SERVICE_DIR/Dockerfile"

cp "$TEMPLATES_DIR/application.properties" "$RESOURCES/application.properties"
substitute "$RESOURCES/application.properties"

# ── Generate Main Class ───────────────────────────────────────────────────────
cat > "$MAIN_SRC/${MAIN_CLASS_NAME}.java" << JAVA
package ${JAVA_PACKAGE};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"${JAVA_PACKAGE}", "com.github.mpalambonisi.common"})
public class ${MAIN_CLASS_NAME} {
  public static void main(String[] args) {
    SpringApplication.run(${MAIN_CLASS_NAME}.class, args);
  }
}
JAVA

# ── Generate Test ─────────────────────────────────────────────────────────────
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
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Test
  void contextLoads() {}
}
JAVA

success "Service '${SERVICE_NAME}' created successfully at services/${SERVICE_NAME}"
