# Event catalogue

All Kafka domain events produced and consumed in Yóbbu V1. This is the authoritative
reference for cross-service communication.

**Status:** validated by the owner on 2026-11-03.

Sources: [domain breakdown](domain-breakdown.md), [sequence diagrams](sequence-diagrams.md),
[ADR 0004](../adr/0004-messaging-kafka.md).

---

## Conventions

| Convention | Value |
|------------|-------|
| Topic naming | `yobbu.<bounded-context>` (e.g., `yobbu.order`) |
| Message key | `{aggregateType}:{aggregateId}` (e.g., `order:a1b2-…`) — ensures ordering per entity |
| Message value | JSON, UTF-8 |
| Schema version | Integer field `v` in every event; starts at `1` |
| Required fields | Every event carries `eventId` (UUID, idempotency), `type` (discriminator), `occurredAt` (ISO 8601 with UTC offset), `v` (version) |
| Idempotency key | `eventId` — consumers record processed IDs to deduplicate |
| Cross-schema refs | UUID only; no embedded objects from other bounded contexts |

---

## Topic: `yobbu.order`

Producer: **order-svc**

| Event `type` | Trigger | Key fields | Consumers |
|---|---|---|---|
| `OrderPlaced` | Customer places an order | `orderId`, `customerId`, `orderType`, `pickupPoint` {lat, lon}, `dropoffPoint` {lat, lon}, `amountValue`, `amountCurrency`, `idempotencyKey` | payment-svc, notification-svc |
| `OrderConfirmed` | Simulated merchant confirms | `orderId` | notification-svc |
| `OrderReady` | Simulated merchant declares ready; Delivery created | `orderId`, `deliveryId`, `pickupPoint` {lat, lon} | dispatch-svc, notification-svc |
| `CourierPickedUp` | Courier confirms pickup | `orderId`, `deliveryId`, `courierId`, `pickedUpAt` | notification-svc |
| `CourierDelivered` | Courier confirms delivery | `orderId`, `deliveryId`, `courierId`, `deliveredAt` | payment-svc, notification-svc |
| `OrderCancelled` | Customer cancels before pickup | `orderId`, `deliveryId` (nullable), `reason` | dispatch-svc, payment-svc, notification-svc |
| `DeliveryCancelled` | Internal: order emits to coordinate dispatch | `deliveryId`, `orderId` | dispatch-svc, notification-svc |

---

## Topic: `yobbu.dispatch`

Producer: **dispatch-svc**

| Event `type` | Trigger | Key fields | Consumers |
|---|---|---|---|
| `DeliveryAssigned` | Courier accepts an offer | `deliveryId`, `orderId`, `courierId`, `assignedAt` | order-svc, tracking-svc, notification-svc |
| `OfferCreated` | Dispatch creates offer for next candidate | `offerId`, `deliveryId`, `courierId`, `deadline` | notification-svc |
| `OfferCancelled` | Order cancelled while offer was PENDING | `offerId`, `deliveryId`, `courierId` | notification-svc |
| `BlockedDelivery` | All candidates refused / expired | `deliveryId`, `orderId`, `reason` | notification-svc (→ operator alert) |

---

## Topic: `yobbu.tracking`

Producer: **tracking-svc**

| Event `type` | Trigger | Key fields | Consumers |
|---|---|---|---|
| `CourierPositionSampled` | tracking-svc stores a new position | `courierId`, `deliveryId` (nullable), `lat`, `lon`, `reportedAt`, `idempotencyKey` | dispatch-svc (snapshot upsert) |
| `CourierAvailabilityChanged` | Courier toggles availability | `courierId`, `availability` (`AVAILABLE`/`UNAVAILABLE`) | dispatch-svc (snapshot update) |

Note: `CourierAvailabilityChanged` is published by tracking-svc after writing to its own
schema. The courier sends `PATCH /couriers/{id}/availability` to the gateway; tracking-svc
owns availability as part of the courier state it manages.

---

## Topic: `yobbu.payment`

Producer: **payment-svc**

| Event `type` | Trigger | Key fields | Consumers |
|---|---|---|---|
| `PaymentCompleted` | Simulated provider callback confirms payment | `paymentId`, `orderId`, `amountValue`, `amountCurrency` | order-svc (optional ack), notification-svc |
| `PaymentCancelled` | Order was cancelled; payment record cancelled | `paymentId`, `orderId` | notification-svc |

---

## Consumer matrix

Rows = producers (topics). Columns = consumers. ✓ = subscribes.

| Topic | order-svc | dispatch-svc | tracking-svc | payment-svc | notification-svc |
|---|:---:|:---:|:---:|:---:|:---:|
| `yobbu.order` | — | ✓ | — | ✓ | ✓ |
| `yobbu.dispatch` | ✓ | — | ✓ | — | ✓ |
| `yobbu.tracking` | — | ✓ | — | — | — |
| `yobbu.payment` | ✓ | — | — | — | ✓ |

`notification-svc` subscribes to all four topics. It is the only service that does so;
it is a "projection" of all domain events into human-readable notifications.

`tracking-svc` subscribes to `yobbu.dispatch` for `DeliveryAssigned` only, to know
which SSE stream to associate with a new courier-delivery pair.

---

## Idempotency notes

- Every consumer records `eventId` in a local deduplication table or uses the idempotency
  key for its own DB operations.
- `dispatch-svc` uses `offers.idempotency_key` to avoid creating duplicate offers.
- `payment-svc` uses `payment_records.idempotency_key` to deduplicate provider callbacks.
- `tracking-svc` uses `positions.idempotency_key` to deduplicate batch-replayed positions.
- `notification-svc` logs processed `eventId` values to avoid sending duplicate notifications.

---

## Schema evolution rules

1. Add fields as optional (nullable / with default). Never remove a field in a minor version.
2. Breaking changes (field removal, type change) require bumping `v` and running both versions
   in parallel during the transition window.
3. All consumers must ignore unknown fields (JSON tolerant parsing). This enables additive
   changes without a coordinated deploy.
