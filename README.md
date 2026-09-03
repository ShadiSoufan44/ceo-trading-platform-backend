# ceo-trading-platform-backend

Backend for our LEAP trading platform school project: buying/selling, per-user statistics, graphing data, and an audit trail of activity. This README also serves as a running log of the architectural decisions we've made and why, since the goal is to learn the stack deliberately rather than have it generated wholesale.

## Tech stack

- **Java 21**, **Spring Boot 4.1.1** (Spring Framework 7 under the hood — fully Jakarta EE, no `javax.*`)
- **Maven** (`pom.xml`, `mvnw` wrapper)
- **PostgreSQL** as the database
- **Flyway** for schema migrations (schema-as-code, not a diagram that drifts from reality)
- **Spring Data JPA** (Hibernate) for persistence
- **Spring Security** for auth
- **Spring Validation** (Bean Validation) for request validation
- **Spring WebMVC** for REST endpoints
- **Docker** for containerization (see Deployment below)

## Architecture decisions

Decisions below are recorded with the reasoning so we don't have to re-litigate them later, and so it's clear what's still open.

### 1. Layered + package-by-feature structure
Standard Spring layering (Controller → Service → Repository → Entity), with packages split by domain feature (`user`, `order`, `portfolio`, `marketdata`, `statistics`, `audit`) rather than by technical layer at the top level. Keeps related code together as the codebase grows.

### 2. DTOs at the API boundary
Controllers never return `@Entity` objects directly over HTTP. Every endpoint has its own request/response DTOs. Reasons: avoids leaking lazy-loading proxies and DB structure into the API contract, and lets the API shape evolve independently of the schema.

### 3. Authentication: stateless JWT
Chosen over server-side sessions because the frontend is a separate client hitting the API — no shared server-rendered session state to lean on. Spring Security will validate a bearer token per request rather than maintaining session state.

### 4. Real-time data: WebSocket streaming
The statistics/graphing features need live-updating prices, not just periodic polling, so we'll use Spring's WebSocket/STOMP support to push market data updates to connected clients rather than relying on the client to poll REST endpoints.

### 5. Trade execution: adapter pattern around an unknown external API
We will eventually integrate with an external broker/exchange API to execute trades, but its exact contract isn't known yet. To avoid coupling business logic to an API we haven't seen:
- Trade execution is defined behind an internal interface (e.g. `TradeExecutionClient`) that the rest of the app depends on.
- For now (and for local dev/tests), a simulated/internal implementation fills orders against our own ledger.
- Once the real API contract is known, we implement a second class satisfying the same interface and swap it in via configuration — no changes needed elsewhere in the codebase.

### 6. Money as `BigDecimal`, never `float`/`double`
Floating-point rounding error in balances or prices is a correctness bug, not a cosmetic one. All monetary/quantity fields use `BigDecimal`.

### 7. Audit logging as data, not just log lines
Two distinct concerns:
- **Application logs** (SLF4J, structured) for debugging/operations.
- **Audit trail**: an append-only record (who did what, when) for trading actions — orders placed, filled, cancelled — kept as actual database rows, not just log output, since it's the kind of thing a real trading platform would need to answer questions about after the fact.

### 8. Database schema: Flyway-first
The schema lives as versioned SQL migration files under `src/main/resources/db/migration/` (`V1__...sql`, `V2__...sql`, ...). This is the source of truth; JPA entities are written to match it, not the other way around.

### 9. Deployment: containerized, target AWS
The app is containerized via the existing `Dockerfile` so it can run anywhere consistently (local dev, CI, eventual hosting). We intend to deploy to AWS, but don't yet have AWS access/account details — the container is being built to be cloud-agnostic in the meantime so that decision doesn't block local development.

## Open questions / not yet decided

- **Database schema**: being finalized — will land as the first set of Flyway migrations.
- **External broker/exchange API contract**: unknown until it's provided; the adapter interface (#5) exists specifically to absorb this unknown.
- **AWS specifics** (ECS/Fargate vs. EC2 vs. App Runner, RDS for Postgres, secrets management, networking): deferred until we have AWS access.
- **Testing strategy in detail**: planned to use Mockito for service-layer unit tests, `@DataJpaTest` for repository tests, and Testcontainers (real Postgres in Docker) for integration tests — not yet implemented.

## Project layout

```
src/main/java/com/ceo/trading_platform_backend/
├── TradingPlatformBackendApplication.java
├── config/        # @Configuration classes (security, websocket, etc.)
├── common/        # cross-cutting: exceptions, global error handling
├── user/
├── order/
├── portfolio/
├── marketdata/
├── statistics/
└── audit/

src/main/resources/
├── application.properties
└── db/migration/  # Flyway SQL migrations
```
