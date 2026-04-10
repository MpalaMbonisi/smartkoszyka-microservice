# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

LABEL service="{{SERVICE_NAME}}"
LABEL maintainer="mpalambonisi"

WORKDIR /workspace

# Copy root pom + common-lib first for better layer caching
COPY pom.xml .
COPY shared/common-lib ./shared/common-lib
COPY services/{{SERVICE_NAME}} ./services/{{SERVICE_NAME}}

# Build only this module (common-lib is compiled as a dependency)
RUN mvn clean package \
      -pl services/{{SERVICE_NAME}} \
      -am \
      -DskipTests \
      --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Patch Alpine packages for known CVEs
RUN apk update && \
    apk upgrade --no-cache && \
    rm -rf /var/cache/apk/*

# Non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser  -u 1001 -S appuser -G appgroup

WORKDIR /app
COPY --from=build /workspace/services/{{SERVICE_NAME}}/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE {{PORT}}

ENTRYPOINT ["java", "-jar", "app.jar"]
