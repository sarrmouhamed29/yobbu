# Domain breakdown and context map

Bounded contexts, data ownership, and inter-service communication for V1 (one simulated country,
Dakar; simulated merchant; simulated payment).

**Status:** validated by the owner on 2026-11-03.

Sources: [ADR 0001](../adr/0001-microservices.md) (services), [ADR 0003](../adr/0003-postgresql-postgis.md)
(data ownership), [ADR 0004](../adr/0004-messaging-kafka.md) (messaging),
[ADR 0005](../adr/0005-realtime-sse.md) (real-time), [ADR 0007](../adr/0007-authentication-keycloak.md)
(identity), [glossary](glossary.md), [use cases](use-cases.md).

See [domain-breakdown.svg](domain-breakdown.svg) — source: [domain-breakdown.puml](domain-breakdown.puml).

---

## Bounded contexts

### Identity — Keycloak (external)

**DDD relationship:** upstream, Open Host Service; all backend services are Conformist downstream.

Keycloak owns all user accounts and roles. Every backend service validates tokens locally using
Keycloak's public key (fetched at startup); no call to Keycloak per request (ADR 0007). No backend
service writes to Keycloak's data store.

Data owned: user accounts, roles (`CUSTOMER`, `COURIER`, `OPERATOR`), refresh tokens, session state.

---

### gateway

Not a domain context. Routes HTTP requests and SSE connections, checks that tokens are valid
(signature and expiry), and enforces rate limiting. Holds no business data.

It is the single entry point (**Open Host Service**) for all frontends and for the simulator.

---

### order

**Core domain.** Owns the commercial lifecycle of a request.

| | |
|---|---|
| **Data owned** | `Order` (type, status, pickup/drop-off point, simulated amount), `Delivery` (state, assignment reference), idempotency log |
| **Sync commands received** | create order, cancel order, confirm order (simulated merchant), declare order ready (simulated merchant) |
| **Events published** | `OrderConfirmed`, `OrderReady`, `DeliveryCancelled`, `DeliveryCompleted` |
| **Events consumed** | `DeliveryAssigned` (from `dispatch`), `PaymentCompleted`, `PaymentFailed` (from `payment`) |

State machine: `PLACED → CONFIRMED → READY → DISPATCHING → IN_PROGRESS → COMPLETED / CANCELLED`

The **simulated merchant** is an internal component of `order` that auto-confirms and auto-declares
orders ready after a configurable delay. It has no interface; the mechanism (timer, event, or direct
state change) is decided during the skeleton and order service design.

---

### dispatch

**Core domain.** Owns the assignment process.

| | |
|---|---|
| **Data owned** | `Offer` (status, deadline, answer), `CourierSnapshot` (availability, load, last known position — local copy fed by events), `Assignment` |
| **Sync commands received** | set courier availability, accept offer, refuse offer |
| **Events published** | `OfferCreated`, `OfferAccepted`, `OfferRefused`, `OfferExpired`, `DeliveryAssigned` |
| **Events consumed** | `OrderReady` (from `order`), `CourierPositionSampled` (from `tracking`), `DeliveryAssigned` (self, to update courier load) |

`dispatch` maintains a local `CourierSnapshot` table (one row per courier) fed by sampled position
events from `tracking` (one event every 15 s per courier, ADR 0003). The proximity search at
assignment time is therefore a local PostGIS query, without a synchronous call to `tracking`. The
data is **eventually consistent**; the dispatch algorithm (ADR 0008) must declare its staleness
tolerance.

---

### tracking

**Core domain.** Owns the position stream and live broadcast.

| | |
|---|---|
| **Data owned** | `Position` (raw, one row per report, with timestamp), active SSE subscriptions (in-memory) |
| **Sync commands received** | post position (courier or simulator), open live stream (customer, operator) |
| **Events published** | `CourierPositionSampled` (one every 15 s per courier, consumed by `dispatch`) |
| **Events consumed** | `DeliveryAssigned` (from `dispatch`, to map a courier to a delivery for SSE routing) |

`tracking` broadcasts live courier positions to SSE subscribers:
- **customer app**: one stream per active delivery (the customer follows their courier);
- **operator console**: all active couriers on the map.

`tracking` does not compute ETA. The frontend receives raw positions; the `order` service exposes a
pre-computed ETA using the straight-line distance and a speed factor (declared simplification,
ADR 0003).

---

### payment

**Supporting domain.** Isolated, thin, stateful.

| | |
|---|---|
| **Data owned** | `PaymentRecord` (status, idempotency key, simulated provider reference) |
| **Sync commands received** | none (driven by events in V1) |
| **Events published** | `PaymentCompleted`, `PaymentFailed` |
| **Events consumed** | `OrderConfirmed` (from `order`) |

The simulated provider is an internal timer in `payment` that fires a fake idempotent callback after
a short delay (ADR 0004). No real money; no external network call.

If the payment integration creates too much overhead for V1, the fallback is simulated cash-on-delivery:
`order` sets the payment status to `PAID` on delivery confirmation without invoking `payment` at
all (see `docs/PROJECT.md`, section 10, and ADR 0001 revisit criteria).

---

### notification

**Supporting domain.** Pure subscriber, Conformist.

| | |
|---|---|
| **Data owned** | `NotificationRecord` (channel, target, status, sent-at) |
| **Sync commands received** | none |
| **Events published** | none |
| **Events consumed** | `OrderConfirmed`, `OrderReady`, `DeliveryAssigned`, `DeliveryCompleted`, `DeliveryCancelled`, `OfferCreated`, `PaymentCompleted`, `PaymentFailed` |

Channels in V1: in-app push (through the existing SSE connection), SMS (simulated: logged only),
email (simulated: logged only). `notification` is a **Conformist**: it subscribes to events published
by the upstream services and formats them per channel without influencing those services.

---

### Simulator (tool — not a production service)

Not a bounded context. A standalone Compose service that simulates courier behavior for demos and
load tests:

- registers virtual courier accounts at startup;
- sets them as available;
- reports positions at regular intervals;
- auto-accepts or auto-refuses offers according to a configurable scenario.

The simulator uses the same gateway APIs as the real courier app. No privileged access, no internal
calls, no separate database.

---

## Communication summary

### Synchronous (REST via gateway)

| Source | Target | Operations |
|---|---|---|
| Customer app | `order` | create, cancel, status |
| Customer app | `tracking` | open live stream (SSE) |
| Courier app / Simulator | `dispatch` | set availability, accept/refuse offer |
| Courier app / Simulator | `tracking` | post position |
| Operator console | `dispatch` | active deliveries, live map |
| Operator console | `tracking` | open live map stream (SSE) |

### Asynchronous (Kafka events)

| Producer | Event | Consumers |
|---|---|---|
| `order` | `OrderConfirmed` | `payment`, `notification` |
| `order` | `OrderReady` | `dispatch` |
| `order` | `DeliveryCompleted`, `DeliveryCancelled` | `notification` |
| `tracking` | `CourierPositionSampled` (1 per courier / 15 s) | `dispatch` |
| `dispatch` | `OfferCreated` | `notification` |
| `dispatch` | `DeliveryAssigned` | `order`, `notification` |
| `dispatch` | `OfferAccepted`, `OfferRefused`, `OfferExpired` | `notification` |
| `payment` | `PaymentCompleted`, `PaymentFailed` | `order`, `notification` |

### SSE live stream (downstream, via gateway)

| Source | Route | Subscribers |
|---|---|---|
| `tracking` | gateway → customer app | courier position for one active delivery |
| `tracking` | gateway → operator console | all active courier positions |

---

## Context map — DDD relationship summary

| Context A | Relationship | Context B | Notes |
|---|---|---|---|
| Keycloak | U → D Conformist | all services | JWT validated locally; services conform to Keycloak's token format |
| `order` | Partnership | `dispatch` | `order` triggers dispatch; `dispatch` returns the assignment |
| `tracking` | U → D Customer/Supplier | `dispatch` | `dispatch` depends on sampled positions from `tracking` (eventually consistent) |
| `order` | Partnership | `payment` | `order` triggers payment; `payment` returns payment status |
| `order` / `dispatch` / `payment` | U → D Conformist | `notification` | `notification` conforms to the event vocabulary of the upstream services |
| `tracking` | U → live clients | SSE subscribers | direct live stream routed through `gateway` |
| Simulator | mirror | courier app APIs | uses the same gateway API as the real courier app |

---

## Decisions

Confirmed by the owner on 2026-11-03.

1. **No shared tables.** No service reads another service's tables (ADR 0001). Consistency across
   services is eventual, via Kafka events.
2. **`dispatch` holds a local `CourierSnapshot`.**  This avoids a synchronous call to `tracking` at
   assignment time and keeps the proximity query local. The trade-off is eventual consistency; the
   dispatch algorithm (ADR 0008) must declare its staleness tolerance.
3. **`tracking` consumes `DeliveryAssigned`.**  This is a dependency not explicit in ADR 0001. It
   is needed so `tracking` knows which SSE stream to route a position update to. It is recorded here
   and will appear in the sequence diagrams.
4. **The simulator is not a service.**  It uses the courier app API through `gateway`. No internal
   calls, no privileged access, no separate database.
5. **`payment` and `notification` are intentionally thin.**  If either creates too much plumbing
   overhead, ADR 0001's revisit criterion applies (merge into `order`).
6. **`tracking` does not compute ETA.**  Raw positions go to the browser; ETA is a field on
   `Order`, computed in `order` using the straight-line formula (ADR 0003, declared simplification).
