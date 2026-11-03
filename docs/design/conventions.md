# Conventions — naming, structure, error handling, logging

Technical conventions applied uniformly across all Yóbbu backend services and frontends.
New contributors follow this document; pull requests that deviate from it require a
documented justification.

**Status:** validated by the owner on 2026-11-03.

Sources: [C4 model](c4.md), [api-design.md](api-design.md),
[ADR 0002](../adr/0002-architecture-backend-java-spring.md),
[ADR 0006](../adr/0006-frontend-react-typescript-vite.md).

---

## 1. Backend module structure

Each service follows a four-layer hexagonal structure. Package names are in English.

```
com.yobbu.<service>/
  domain/
    model/           # Aggregates, entities, value objects (no framework annotations)
    service/         # Domain services (pure business logic)
    event/           # Domain events (published internally via Spring ApplicationEvent)
    exception/       # Domain exceptions (e.g., OrderNotFoundException, InvalidStateException)
  application/
    command/         # Command objects (PlaceOrderCommand, CancelOrderCommand)
    handler/         # Command handlers (one handler per use case)
    query/           # Query objects and read models
  infrastructure/
    persistence/     # JPA entities, repositories, mappers (JPA annotations go here, not in domain)
    messaging/       # Kafka producers and consumers
    simulation/      # Simulated dependencies (MerchantSimulator, etc.)
  api/
    rest/            # @RestController, request/response DTOs, mappers
    advice/          # @RestControllerAdvice (global exception handler)
```

**Key rule:** the domain layer has no dependency on Spring, JPA, or Kafka. It is pure Java.
The application layer depends on the domain and on interfaces; the infrastructure layer
implements those interfaces.

Example for `order-svc`:
```
com.yobbu.order/
  domain/model/Order.java
  domain/model/Delivery.java
  domain/exception/OrderNotFoundException.java
  application/command/PlaceOrderCommand.java
  application/handler/PlaceOrderCommandHandler.java
  infrastructure/persistence/OrderJpaEntity.java
  infrastructure/persistence/OrderJpaRepository.java
  infrastructure/messaging/OrderEventPublisher.java
  api/rest/OrderController.java
  api/rest/dto/PlaceOrderRequest.java
  api/advice/GlobalExceptionHandler.java
```

---

## 2. Naming conventions

### Java

| Concept | Convention | Example |
|---------|-----------|---------|
| Aggregate | `<Name>` | `Order`, `Delivery`, `Offer` |
| Command | `<Verb><Name>Command` | `PlaceOrderCommand`, `CancelOrderCommand` |
| Command handler | `<Verb><Name>CommandHandler` | `PlaceOrderCommandHandler` |
| Domain event | `<Name><PastParticiple>Event` | `OrderPlacedEvent`, `DeliveryAssignedEvent` |
| Kafka event (DTO) | `<Name><PastParticiple>` (no suffix) | `OrderPlaced`, `DeliveryAssigned` |
| Repository interface | `<Name>Repository` | `OrderRepository` |
| JPA entity | `<Name>JpaEntity` | `OrderJpaEntity` |
| REST controller | `<Name>Controller` | `OrderController` |
| DTO (request) | `<Verb><Name>Request` | `PlaceOrderRequest` |
| DTO (response) | `<Name>Response` | `OrderResponse` |
| Exception | `<Name>Exception` | `OrderNotFoundException`, `InvalidOrderStateException` |
| Service | `<Name>Service` | `DispatchApplicationService`, `DispatchAlgorithm` |

### REST API paths

- Lowercase, hyphenated: `/api/v1/order-items`, not `/api/v1/orderItems`
- Nouns for resources, verbs for sub-resource actions: `POST /orders/{id}/cancel`
- Plural nouns for collections: `/orders`, not `/order`
- IDs are always UUIDs in path segments: `/orders/{id}`, not `/orders/{orderId}`

### Kafka topics and event types

- Topics: `yobbu.<bounded-context>` (e.g., `yobbu.order`)
- Event type discriminator (`type` field): `PascalCase` past participle (e.g., `OrderPlaced`)
- No abbreviations in event names

### TypeScript / React

- React components: `PascalCase` files and function names (`OrderCard.tsx`)
- Hooks: `useOrderStatus.ts` (camelCase with `use` prefix)
- Utilities and services: `camelCase` (`orderService.ts`, `formatCurrency.ts`)
- Constants: `UPPER_SNAKE_CASE` (`MAX_BATCH_SIZE`)
- Types/interfaces: `PascalCase`, no `I` prefix (`Order`, `DeliveryStatus`)

---

## 3. Error handling

### Backend

All runtime exceptions are caught by a single `@RestControllerAdvice`:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handle(OrderNotFoundException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setType(URI.create("https://yobbu.app/errors/order-not-found"));
        pd.setTitle("Order not found");
        pd.setDetail(ex.getMessage());
        pd.setProperty("traceId", MDC.get("traceId"));
        return pd;
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    ProblemDetail handle(InvalidOrderStateException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setType(URI.create("https://yobbu.app/errors/invalid-order-state"));
        pd.setTitle("Invalid order state");
        pd.setDetail(ex.getMessage());
        pd.setProperty("traceId", MDC.get("traceId"));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnknown(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setType(URI.create("https://yobbu.app/errors/internal"));
        pd.setTitle("Internal server error");
        pd.setProperty("traceId", MDC.get("traceId"));
        return pd;  // No detail: never expose internal messages
    }
}
```

Rules:
- Never propagate stack traces or internal class names in responses.
- The `traceId` links the response to the structured log for investigation.
- Use `ProblemDetail` (Spring 6 built-in RFC 7807 support).
- One `@ExceptionHandler` per exception type; no catch-all for known domain errors.

### Frontend

- Network errors: show a toast with a retry button; log `traceId` if available.
- `4xx` responses: show inline validation messages (400) or dedicated error screens (403, 404).
- `5xx` responses: show a generic error screen with the `traceId` for support.
- Never show raw error objects or stack traces to the user.

---

## 4. Logging conventions

### Format

All backend services use **structured JSON logging** (Logback + `logstash-logback-encoder`).
Every log line is a JSON object on a single line:

```json
{
  "timestamp": "2026-11-03T10:00:01.234Z",
  "level": "INFO",
  "logger": "com.yobbu.order.application.handler.PlaceOrderCommandHandler",
  "message": "Order placed",
  "service": "order-svc",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "a3ce929d0e0e4736",
  "orderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

Required fields in every log line:

| Field | Source |
|-------|--------|
| `timestamp` | Logback (ISO 8601, UTC) |
| `level` | Log level |
| `logger` | Class name (full path, shortened) |
| `service` | Spring application name (`spring.application.name`) |
| `traceId` | OpenTelemetry W3C trace ID (MDC) |
| `spanId` | OpenTelemetry span ID (MDC) |

### Log levels

| Level | When to use |
|-------|-------------|
| `ERROR` | Unexpected failures: unhandled exception, Kafka publish failure, DB connection lost |
| `WARN` | Recoverable situations: idempotent duplicate detected, offer expired, courier unavailable |
| `INFO` | Business events: order placed, delivery assigned, offer accepted (one line per event) |
| `DEBUG` | Development only: query parameters, state transitions detail, Kafka consumer lag |
| `TRACE` | Never in default config; only for deep protocol debugging |

Production and staging: `INFO` level. Development: `DEBUG` level.

### What not to log

- PII: no names, email addresses, phone numbers, or precise addresses in log messages.
- Secrets: no JWTs, API keys, or passwords.
- Full request/response bodies: log only IDs and status codes.
- `DEBUG` logs in production: they contain internal state and inflate log volume.

### Log message pattern

```java
// Good — stable message + structured context fields
log.info("Order placed: orderId={}, customerId={}, type={}",
         order.getId(), order.getCustomerId(), order.getType());

// Bad — message contains variable data (hard to search, grep)
log.info("Order " + order.getId() + " placed by " + order.getCustomerId());
```

Use SLF4J parameter substitution, not string concatenation.

---

## 5. Code quality gates (applied in CI)

| Tool | Purpose | Configuration |
|------|---------|---------------|
| `checkstyle` | Code style (indentation, import order, blank lines) | `checkstyle.xml` at repo root |
| `spotbugs` + `find-security-bugs` | Static analysis + security patterns | CI step in each service |
| `eslint` + `typescript-eslint` | Frontend linting and type checking | `.eslintrc.json` per PWA |
| `prettier` | Formatting for TypeScript/JSON/YAML | `.prettierrc` at repo root |

All gates must pass before a PR can be merged. Warnings are treated as errors in CI.
