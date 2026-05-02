# SmartKoszyka Microservices

Microservices rewrite of the SmartKoszyka shopping-list platform.
Built with **Java 21**, **Spring Boot 3.3**, **Maven multi-module**, and **Docker Compose**.

---

## Architecture

### Monolith to Microservices Migration

The original SmartKoszyka application was a single Spring Boot monolith. This rewrite decomposes it into four independently deployable services behind a single entry point. Each service owns its own database schema, its own deployment lifecycle, and communicates over HTTP.

### Runtime Stack Overview

```
Client (Browser / Mobile)
        |
        | HTTPS
        v
+--------------------------+
|    api-gateway           |   port 8080   Spring WebFlux / Netty
|  (Spring Cloud           |
|   Gateway)               |
+--------------------------+
   |         |         |
   |         |         |
   v         v         v
auth-     product-   shopping-list-
service   service    service
(8081)    (8082)     (8083)
Spring    Spring     Spring MVC
MVC       MVC        (Servlet)
   |         |         |
   v         v         v
         PostgreSQL (shared instance, separate schemas)
```

### Spring WebFlux vs Spring MVC: Where and Why

This project uses both the reactive and servlet stacks deliberately. Understanding the boundary between them is important for future development.

**api-gateway -> Spring WebFlux (Reactor / Netty)**

The API gateway runs on Spring Cloud Gateway, which is built on Spring WebFlux and the Netty event-loop server. It never does blocking I/O. Its sole responsibilities are JWT validation, request routing, and propagating the authenticated user identity downstream via the `X-Authenticated-User` header. Because every inbound request passes through the gateway before being forwarded, a non-blocking model is the correct fit: the gateway is latency-sensitive and spend most of its time waiting on network, not doing CPU work.

Security is configured via `ServerHttpSecurity` (the reactive equivalent of `HttpSecurity`). The custom `JwtAuthenticationGatewayFilter` extends `AbstractGatewayFilterFactory` and returns a `Mono<Void>`, which is the reactive contract. There is no thread-per-request model here.

**auth-service, product-service, shopping-list-service -> Spring MVC (Servlet / Tomcat)**

The three downstream services use the traditional servlet stack. They perform blocking JDBC calls via Spring Data JPA and Hibernate. The servlet model is the correct fit here because JPA is inherently blocking and the overhead of bridging blocking calls into a reactive pipeline (via `subscribeOn(boundedElastic())`) would add complexity with no benefit. Each service runs on an embedded Tomcat instance.

### Cross-Stack Communication

The shopping-list-service calls product-service over HTTP to fetch product details at the moment an item is added to a list. This is an outbound HTTP call from a servlet-based service. Spring WebFlux's `WebClient` is used here [ not `RestTemplate` ] because `WebClient` is the modern, non-deprecated HTTP client regardless of whether the caller is reactive or not. Adding `spring-boot-starter-webflux` to a servlet application does not switch the server to Netty; Tomcat remains the servlet container and `WebClient` is used purely as an outbound client.

```
shopping-list-service (Tomcat / MVC)
        |
        | WebClient (outbound HTTP)
        v
product-service (Tomcat / MVC)
GET /api/products/{id}
```

The response is blocked synchronously with `.block()` inside the servlet thread. This is intentional and acceptable here because the shopping-list-service is already on a blocking thread and the call volume does not justify a fully reactive chain.

### JWT Authentication Flow

JWT validation happens exclusively at the gateway. Downstream services do not re-validate tokens.

```
1. Client sends:  Authorization: Bearer <token>
2. Gateway extracts and validates the JWT using JwtService (shared common-core library)
3. On success, gateway strips the Authorization header and adds:
        X-Authenticated-User: user@example.com
4. Downstream service (e.g product-service) reads the header directly from the request
5. Downstream service trusts this header [it only arrives via the gateway]
```

The `JwtService` and `JwtServiceImpl` live in `shared/common-core`, which has no web dependency. Both the reactive gateway and the servlet-based services share the same JWT implementation without pulling in the wrong web stack.

### Exception Handling: Two Handlers for Two Stacks

Because the gateway runs on WebFlux and the downstream services run on the servlet stack, there are two separate exception handlers:

| Component | Handler | Base Class |
|-----------|---------|------------|
| api-gateway | `GatewayExceptionHandler` | `ErrorWebExceptionHandler` (reactive) |
| auth-service, product-service, shopping-list-service | `GlobalExceptionHandler` | `ResponseEntityExceptionHandler` (servlet) |

`GlobalExceptionHandler` lives in `shared/common-servlet` and is picked up automatically by the three servlet-based services via `@ComponentScan`. The gateway cannot use it because `ResponseEntityExceptionHandler` depends on the servlet dispatcher.

### Observability

Every service exposes a separate management port for Prometheus scraping. Prometheus and Grafana run as sidecar containers in Docker Compose.

| Service | App Port | Management Port |
|---------|----------|----------------|
| api-gateway | 8080 | 9080 |
| auth-service | 8081 | 9081 |
| product-service | 8082 | 9082 |
| shopping-list-service | 8083 | 9083 |

### Shared Library Design

The shared code is split into two modules to prevent the reactive gateway from accidentally pulling in servlet dependencies.

```
shared/common-core       No web dependency. Safe for all services.
  - JwtService / JwtServiceImpl
  - Exception types (ResourceNotFoundException, EmailAlreadyExistsException)
  - Request/response DTOs

shared/common-servlet    Extends common-core. Servlet-only additions.
  - GlobalExceptionHandler
  - ErrorResponse
```

The api-gateway depends only on `common-core`. The three servlet-based services depend on `common-servlet`, which transitively provides `common-core`.

---

## Project Structure

```
SmartKoszyka-microservices/
|
+-- pom.xml                              Root / parent POM (aggregator)
+-- docker-compose.yml                   Local dev orchestration
+-- check-quality.sh                     Run all quality gates locally
+-- commitlint.config.js                 Conventional Commits config
+-- .pre-commit-config.yaml              Git hook definitions
+-- .env.example                         Secret template (copy to .env)
|
+-- config/
|   +-- pmd-ruleset.xml                  Shared PMD rules
|   +-- spotbugs-exclude.xml             Shared SpotBugs exclusions
|
+-- scripts/
|   +-- mvnw-java21.sh                   Maven wrapper forcing Java 21
|   +-- find-java21.sh                   Locates JAVA_HOME for pre-commit hooks
|   +-- new-service.sh                   Scaffold a new service consistently
|
+-- shared/
|   +-- common-core/                     No web dependency — safe for all services
|   |   +-- src/main/java/com/github/mpalambonisi/common/
|   |       +-- service/                 JwtService + JwtServiceImpl
|   |       +-- exception/              ResourceNotFoundException, EmailAlreadyExistsException
|   |       +-- dto/                    Shared request/response DTOs
|   |
|   +-- common-servlet/                  Extends common-core — servlet services only
|       +-- src/main/java/com/github/mpalambonisi/common/
|           +-- exception/              GlobalExceptionHandler, ErrorResponse
|
+-- services/
|   +-- api-gateway/                     Port 8080 — Spring WebFlux, Netty
|   |   +-- Dockerfile
|   |   +-- pom.xml
|   |   +-- src/main/java/.../gateway/
|   |       +-- config/                 GatewaySecurityConfig (ServerHttpSecurity)
|   |       +-- filter/                 JwtAuthenticationGatewayFilter
|   |       +-- exception/             GatewayExceptionHandler (reactive)
|   |
|   +-- auth-service/                    Port 8081 — Spring MVC, Tomcat
|   |   +-- Dockerfile
|   |   +-- pom.xml
|   |   +-- src/main/java/.../auth/
|   |       +-- config/                 SecurityConfig
|   |       +-- controller/            AuthController, AccountController
|   |       +-- model/                 Account
|   |       +-- repository/            AccountRepository
|   |       +-- service/               AccountService, AccountServiceImpl
|   |                                  AccountDetailsServiceImpl
|   |
|   +-- product-service/                 Port 8082 — Spring MVC, Tomcat
|   |   +-- Dockerfile
|   |   +-- pom.xml
|   |   +-- src/main/java/.../product/
|   |       +-- config/                 SecurityConfig
|   |       +-- controller/            ProductController, CategoryController
|   |       +-- dto/                   ProductResponse, CategoryResponse
|   |       +-- model/                 Product, Category
|   |       +-- repository/            ProductRepository, CategoryRepository
|   |       +-- service/               ProductService, CategoryService (+ impls)
|   |       +-- scraper/               ScraperService, ScraperScheduled
|   |
|   +-- shopping-list-service/           Port 8083 — Spring MVC, Tomcat
|       +-- Dockerfile
|       +-- pom.xml
|       +-- src/main/java/.../shoppinglist/
|           +-- client/                 ProductClient (WebClient — outbound only)
|           |                           ProductResponse (slim DTO)
|           +-- config/                 SecurityConfig
|           +-- controller/            ShoppingListController, ShoppingListItemController
|           +-- dto/                   request/, response/
|           +-- model/                 ShoppingList, ShoppingListItem
|           +-- repository/            ShoppingListRepository, ShoppingListItemRepository
|           +-- service/               ShoppingListService, ShoppingListItemService (+ impls)
|
+-- monitoring/
    +-- prometheus.yml                   Scrape config for all four services
    +-- grafana/
        +-- provisioning/                Grafana datasource and dashboard provisioning
```

---

## Microservice Breakdown

| Service | Port | Responsibility |
|---------|------|----------------|
| `api-gateway` | 8080 | Single entry point, JWT validation, routing |
| `auth-service` | 8081 | Register, login, JWT issuance |
| `product-service` | 8082 | Product catalogue and Biedronka scraper |
| `shopping-list-service` | 8083 | Shopping lists and list items |

---

## Maven Profiles

| Profile | Purpose |
|---------|---------|
| *(default)* | Compile and test — no static analysis |
| `quality` | + Checkstyle + SpotBugs + PMD |
| `skip-quality` | Skip all static analysis (fast local iteration) |

```bash
./mvnw verify -P quality          # full gates
./mvnw package -P skip-quality    # fast build, no checks
```

---

**Author: Mbonisi Mpala**
