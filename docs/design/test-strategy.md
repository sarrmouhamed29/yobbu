# Test strategy

Test pyramid, data strategy, contract tests, load tests and resilience plan for Yóbbu V1.

**Status:** validated by the owner on 2026-11-03.

Sources: [conventions.md](conventions.md), [api-design.md](api-design.md),
[event-catalogue.md](event-catalogue.md),
[consistency-strategy.md](consistency-strategy.md).

---

## 1. Pyramid

```
             ┌─────────────────────────────┐
             │       End-to-end (E2E)      │  ← few, slow, full stack
             ├─────────────────────────────┤
             │     Contract (Pact)         │  ← API + event schemas
             ├─────────────────────────────┤
             │  Integration (Testcontainers│  ← service + DB + Kafka
             ├─────────────────────────────┤
             │       Unit tests            │  ← most, fast, no I/O
             └─────────────────────────────┘
```

| Layer | Tool | Scope | Speed | Required before merge |
|-------|------|-------|-------|-----------------------|
| Unit | JUnit 5 + Mockito / Vitest | Domain model, command handlers, algorithm, React components | < 1 s per suite | Yes |
| Integration | JUnit 5 + Testcontainers | One service: real DB, real Kafka, real schema migrations | < 3 min per service | Yes |
| Contract | Pact (consumer-driven) | REST API + Kafka event schemas | < 2 min total | Yes |
| E2E | Playwright | Nominal delivery flow across all services in Docker Compose | < 10 min | On main merge |
| Load | k6 | Dispatch and position endpoints under peak load | As needed | No (runs separately) |

---

## 2. Unit tests

### Backend (JUnit 5 + Mockito)

**What to unit-test:**

- Domain model invariants (state machines, business rules)
- Command handlers with mocked repositories and event publishers
- The dispatch algorithm (candidate ranking, tie-breaking, boundary cases)
- Idempotency logic in payment-svc and tracking-svc
- Error mapping in `GlobalExceptionHandler`

**What NOT to unit-test:**

- JPA entities (no logic there)
- Kafka consumer retry configuration (integration-level)
- Spring wiring (`@SpringBootTest` is integration, not unit)

**Example: dispatch algorithm**

```java
@Test
void should_rank_nearest_courier_first() {
    var couriers = List.of(
        courier("a", distanceMeters(800)),
        courier("b", distanceMeters(300)),
        courier("c", distanceMeters(1500))
    );
    var ranked = algorithm.rank(couriers, delivery());
    assertThat(ranked.get(0).getId()).isEqualTo("b");
}

@Test
void should_skip_courier_with_active_offer() {
    var courier = courier("a", distanceMeters(100)).withPendingOffer();
    assertThat(algorithm.rank(List.of(courier), delivery())).isEmpty();
}
```

### Frontend (Vitest + React Testing Library)

- Component rendering with mocked data
- State machine hooks (`useOrderStatus`, `useOfferCountdown`)
- Offline queue logic in the courier PWA
- Form validation

---

## 3. Integration tests

**Stack:** JUnit 5 + Testcontainers (PostgreSQL + PostGIS, Kafka, Keycloak stub).

**Principle:** one `@SpringBootTest` per service, one shared `@Container` instance per test
class (started once via `@ClassRule`). No embedded H2 — PostGIS extensions must be real.

**What to test per service:**

| Service | Key integration scenarios |
|---------|--------------------------|
| order-svc | Place order → order persisted + `OrderPlaced` event published to Kafka |
| order-svc | Cancel order in wrong state → `409 Conflict` + no event |
| order-svc | Duplicate `Idempotency-Key` → `200` with same response, no second insert |
| dispatch-svc | Consume `OrderReady` → couriers queried from PostGIS + offer inserted |
| dispatch-svc | Offer expires → `OfferExpired` event published, next candidate offered |
| tracking-svc | Batch of 10 positions → stored count correct + duplicate key skipped |
| tracking-svc | Consume `DeliveryAssigned` → tracking record created |
| payment-svc | Callback received → `PaymentCompleted` event published |
| payment-svc | Duplicate callback → `200` without second event |

**Testcontainers setup (shared base class):**

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
abstract class BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgis/postgis:16-3.4")
            .withReuse(true);

    @Container
    static final KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.0"))
            .withReuse(true);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

Flyway migrations run on startup: integration tests always validate the real schema.

---

## 4. Contract tests (Pact)

Consumer-driven contract testing ensures that a service's published API or events match
what the consumers actually use.

### REST contracts

| Consumer | Provider | Contract |
|----------|----------|---------|
| customer-app | order-svc | `POST /orders`, `GET /orders/{id}`, `GET /orders/{id}/delivery` |
| courier-app | dispatch-svc | `GET /offers/{id}`, `POST /offers/{id}/accept`, `POST /offers/{id}/refuse`, `PUT /couriers/{id}/availability` |
| courier-app | tracking-svc | `POST /positions`, `POST /positions/batch` |
| customer-app | tracking-svc | `GET /couriers/{id}/stream` (SSE — schema of the `data` field) |

**Workflow:**

1. Each consumer project defines its Pact (in `__tests__/pacts/`)
2. Pact files are published to the Pact Broker (or checked into `contracts/` for V1)
3. Provider verifies the contract in CI using `@PactTestFor`

### Kafka event contracts

| Producer | Consumer | Event |
|----------|----------|-------|
| order-svc | dispatch-svc | `OrderReady` |
| dispatch-svc | order-svc | `DeliveryAssigned` |
| dispatch-svc | tracking-svc | `DeliveryAssigned` |
| dispatch-svc | notification-svc | `OfferCreated` |
| tracking-svc | dispatch-svc | `CourierPositionSampled` |
| payment-svc | order-svc | `PaymentCompleted` |

Each Kafka event has a JSON Schema checked into `contracts/events/<EventName>.schema.json`.
Consumers validate incoming events against the schema; producers assert that emitted
events pass the schema in unit or integration tests.

---

## 5. End-to-end tests

**Tool:** Playwright (TypeScript).

**Environment:** Docker Compose (all services + simulators + observability stack).

**Scope:** one nominal path per actor, verified from the browser UI.

### E2E scenarios (V1)

| # | Scenario | Actor(s) | Expected outcome |
|---|----------|----------|-----------------|
| E2E-01 | Customer places order → livreur simulator accepts → customer sees courier on map | Customer, Simulator | Delivery reaches `ASSIGNED` state; map shows moving courier |
| E2E-02 | Courier refuses offer → next courier proposed automatically | Simulator × 2 | First offer `REFUSED`; second offer `PENDING` within 5 s |
| E2E-03 | Courier marks delivery as delivered | Courier PWA | Order reaches `DELIVERED`; customer notification sent |
| E2E-04 | Operator views active deliveries | Operator console | All in-progress deliveries appear on the console map |

Playwright tests run in headless mode in CI. Screenshots and traces are uploaded as
artifacts on failure.

---

## 6. Load tests

**Tool:** k6 (JavaScript scenarios, run from the CI agent or locally).

**Baseline targets** (from non-functional requirements):

| Scenario | Target | Metric |
|----------|--------|--------|
| Order placement | 50 RPS sustained | p95 < 500 ms |
| Position reporting | 1000 RPS sustained (100 couriers × 10 Hz) | p95 < 200 ms |
| Dispatch cycle | 50 deliveries/min | p95 < 3 s end-to-end |

**Scripts location:** `load-tests/` at repo root.

**Runs:** manually on demand (not in every CI pipeline run). Results are checked in as
`load-tests/results/<scenario>-<date>.json` with a summary committed alongside.

**k6 example (position endpoint):**

```js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 100,
  duration: '60s',
  thresholds: {
    http_req_duration: ['p(95)<200'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const res = http.post(`${__ENV.BASE_URL}/api/v1/positions`, JSON.stringify({
    lat: 14.6928 + Math.random() * 0.01,
    lon: -17.4467 + Math.random() * 0.01,
    reportedAt: new Date().toISOString(),
  }), {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${__ENV.COURIER_TOKEN}`,
      'Idempotency-Key': crypto.randomUUID(),
    },
  });
  check(res, { 'status 201': (r) => r.status === 201 });
  sleep(0.1);
}
```

---

## 7. Resilience plan

These are the failure modes that must be handled without data loss, tested via integration
tests and manual chaos experiments in Docker Compose.

| Failure mode | Expected behaviour | Test approach |
|--------------|-------------------|---------------|
| Kafka broker restart during dispatch | `@TransactionalEventListener` retries via Spring Retry; no event lost | Integration: stop Kafka, place order, restart Kafka, verify event consumed |
| Courier loses connectivity mid-delivery | Batch endpoint on reconnect syncs all buffered positions | Integration: offline courier → `POST /positions/batch` with 50 entries |
| `POST /orders` duplicate (retry on client timeout) | Idempotency-Key deduplicates; second call returns same `orderId` | Integration: send same request twice, assert one row in DB |
| Order service restart while offer pending | Offer deadline evaluated on reconnect; expired offers auto-refused | Integration: place order, stop order-svc, wait > deadline, restart, assert offer EXPIRED |
| Position batch with duplicate keys | Deduplication by `idempotency_key`; stored count + deduplicated count correct | Unit + integration |
| Dispatch blocked (no available couriers) | Delivery moves to `PENDING_ASSIGNMENT`; operator alerted | Integration: no available couriers in DB, send `OrderReady`, assert `PENDING_ASSIGNMENT` |

---

## 8. Test data

**Backend:** each integration test suite uses a Flyway `afterMigrate.sql` seed that
creates a fixed set of UUIDs (couriers, merchants, customers) with known coordinates in
Dakar. Seed data is checked into `db/seeds/test/`.

**Frontend:** MSW (Mock Service Worker) intercepts API calls in Vitest and Playwright,
returning fixtures from `src/test/fixtures/`.

**Sensitive data:** no real coordinates, no real names, no real phone numbers. All
fixtures use clearly fictional data (e.g., `customer-test-001`).

**Golden IDs** (stable across test runs):

```
CUSTOMER_ID  = "00000000-0000-0000-0000-000000000001"
MERCHANT_ID  = "00000000-0000-0000-0000-000000000010"
COURIER_A_ID = "00000000-0000-0000-0000-000000000020"
COURIER_B_ID = "00000000-0000-0000-0000-000000000021"
```

Using fixed IDs avoids cascading failures when one test's UUID is accidentally reused
in another's assertion.
