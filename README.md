# SmartKoszyka Microservices

Microservices rewrite of the SmartKoszyka shopping-list platform.
Built with **Java 21**, **Spring Boot 3.3**, **Maven multi-module**, and **Docker Compose**.

---

## Project Structure

```
SmartKoszyka-microservices/
│
├── pom.xml                          ← Root / parent POM (aggregator)
├── docker-compose.yml               ← Local dev orchestration
├── check-quality.sh                 ← Run all quality gates locally
├── commitlint.config.js             ← Conventional Commits config
├── .pre-commit-config.yaml          ← Git hook definitions
├── .env.example                     ← Secret template (copy → .env)
│
├── config/
│   ├── pmd-ruleset.xml              ← Shared PMD rules
│   └── spotbugs-exclude.xml         ← Shared SpotBugs exclusions
│
├── scripts/
│   ├── mvnw-java21.sh               ← Maven wrapper forcing Java 21
│   ├── find-java21.sh               ← Locates JAVA_HOME for hooks
|   └── new-service.sh               ← Create Services in Spring Boot for a consistency
│
├── shared/
│   └── common-lib/                  ← Shared library (JWT, exceptions, DTOs)
│       └── src/main/java/com/github/mpalambonisi/common/
│           ├── service/             ← JwtService + JwtServiceImpl
│           ├── exception/           ← GlobalExceptionHandler, ErrorResponse, …
│           └── dto/                 ← Shared request/response DTOs
│
└── services/                        ← (created in Stage 2+)
    ├── auth-service/
    ├── product-service/
    ├── shopping-list-service/
    └── api-gateway/
```

---

## Planned Microservice Breakdown

| Service                   | Port | Responsibility                              |
|---------------------------|------|---------------------------------------------|
| `api-gateway`             | 8080 | Single entry point, JWT validation, routing |
| `auth-service`            | 8081 | Register, login, JWT issuance               |
| `product-service`         | 8082 | Product catalogue + Biedronka scraper       |
| `shopping-list-service`   | 8083 | Shopping lists + list items                 |

---

## Commit Convention

This repo enforces **[Conventional Commits](https://www.conventionalcommits.org)**
via commitlint (CI + local pre-commit hook).

```
<type>(<scope>): <short description>

Types: feat | fix | docs | style | refactor | perf | test | build | ci | chore | revert
Scope: module name, e.g. common-lib | auth-service | product-service

Examples:
  feat(auth-service): add JWT refresh token endpoint
  fix(common-lib): defensive copy in ErrorResponse.getMessage()
  ci: add Trivy scan to docker-build job
```

---

## Maven Profiles

| Profile         | Purpose                                               |
|-----------------|-------------------------------------------------------|
| *(default)*     | Compile + test + JaCoCo agent — no static analysis   |
| `quality`       | + Checkstyle + SpotBugs + PMD + JaCoCo coverage gate |
| `skip-quality`  | Skip all static analysis (fast local iteration)       |

```bash
./mvnw verify -P quality          # full gates
./mvnw package -P skip-quality    # fast build, no checks
```

---

**Author: Mbonisi Mpala**
