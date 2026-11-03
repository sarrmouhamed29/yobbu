# Consistency strategy

What is transactional, what is asynchronous, and where idempotency is required.

**Status:** validated by the owner on 2026-11-03.

Sources: [domain breakdown](domain-breakdown.md), [event catalogue](event-catalogue.md),
[sequence diagrams](sequence-diagrams.md), [C4 model](c4.md),
[ADR 0004](../adr/0004-messaging-kafka.md).

---

## 1. Intra-service consistency

Everything that happens within one service is **synchronous and transactional**. A single
database transaction covers:

- Writing the new aggregate state (e.g., `orders.status = 'CONFIRMED'`)
- Writing the status history entry (e.g., `order_status_history`)
- Writing the idempotency key (if the operation came with one)

**Consequence:** if the transaction rolls back (constraint violation, optimistic lock, any
DB error), none of these writes happen. The aggregate state and the audit trail stay consistent.

The Kafka event is published *after* the transaction commits (Spring
`@TransactionalEventListener(phase = AFTER_COMMIT)`). If the Kafka publish fails (Kafka
temporarily unavailable), the DB write has already committed. The event is lost in V1.
This is the known V1 limitation — see Section 5 below.

---

## 2. Cross-service consistency via Kafka

Cross-service state is **eventually consistent**. No distributed transaction spans two
services. The delivery assignment is a good example:

```
dispatch-svc writes:
  - offers: Offer B → ACCEPTED
  - courier_snapshots: active_deliveries += 1

dispatch-svc publishes:
  - DeliveryAssigned {deliveryId, courierId}

order-svc receives DeliveryAssigned and writes:
  - deliveries: status → ASSIGNED, assigned_courier_id = courierId
  - delivery_status_history: PENDING → ASSIGNED

tracking-svc receives DeliveryAssigned and writes:
  - in-memory routing: subscribe this SSE connection to courierId's positions
```

Between the time `dispatch-svc` commits and the time the consumers process the event,
the system is in a temporary inconsistent state: dispatch knows the courier is assigned but
order and tracking do not yet. This window is typically < 100 ms at demo scale.

### What is eventually consistent

| State | Owner | Lag |
|-------|-------|-----|
| `deliveries.status` reflects `DeliveryAssigned` | order-svc | < 100 ms typical |
| `courier_snapshots.active_deliveries` | dispatch-svc | immediate (local write) |
| `courier_snapshots.last_known_position` | dispatch-svc | ≤ 15 s (position reporting interval) |
| `payment_records.status` reflects `OrderPlaced` | payment-svc | < 100 ms typical |
| Notification sent to customer | notification-svc | < 1 s typical |

### What is NOT eventually consistent

- The invariant "at most one PENDING offer per delivery" is enforced by a **partial unique
  index** in `dispatch-svc`'s own DB. It is a synchronous, transactional check — not
  eventually consistent.
- A command that requires up-to-date state (e.g., "can this order be cancelled?") reads the
  current state from the owning service's DB, not from a stale projection.

---

## 3. Idempotency requirements and patterns

Every state-changing operation has an idempotency guarantee.

### Command idempotency (HTTP layer)

| Command | Key | Behaviour on duplicate |
|---------|-----|------------------------|
| `POST /orders` | `idempotency_key` in request body | Returns the existing order without re-creating |
| `POST /positions/batch` | Per-position `idempotencyKey` | Duplicate positions silently dropped (UNIQUE constraint on `positions.idempotency_key`) |
| `POST /payment/callback` | `idempotencyKey` in callback body | Returns 200 OK without re-processing; no second `PaymentCompleted` event |

### Event idempotency (Kafka layer)

Every Kafka event carries a UUID `eventId`. Consumers that produce side effects (DB writes,
outbound notifications) check whether `eventId` has already been processed:

| Consumer | Deduplication mechanism |
|----------|------------------------|
| `dispatch-svc` on `OrderReady` | `offers.idempotency_key` = `eventId`; duplicate returns existing offer without re-dispatching |
| `dispatch-svc` on `CourierPositionSampled` | Upsert on `courier_snapshots.courier_id` by `last_position_at` — only updates if the new position is newer |
| `payment-svc` on `OrderPlaced` | `payment_records.idempotency_key` = `eventId`; duplicate ignored |
| `notification-svc` on any event | Processed `eventId` recorded in `notification_records` before send; duplicate skipped |
| `tracking-svc` — position batch | `positions.idempotency_key` = per-position key from the client |

### Offer idempotency in dispatch

The dispatch loop is the most complex sequence to make idempotent:

1. Dispatch creates `Offer A` with `idempotency_key = eventId(OrderReady) + courierId_A`.
2. If the service restarts and reprocesses `OrderReady`, the DB UNIQUE constraint on
   `idempotency_key` rejects the second insert → dispatch reads the existing offer and
   continues the loop from its current state.
3. If `Offer A` is already REFUSED/EXPIRED, dispatch moves to the next candidate.

---

## 4. Choreography-based saga

The nominal delivery path is a **choreography-based saga** with no central orchestrator.
Each service reacts to the previous service's event.

```
                Saga steps
Customer          order-svc         payment-svc      dispatch-svc
   |                 |                   |                |
   |-- POST /orders->|                   |                |
   |                 |--[OrderPlaced]--->|                |
   |                 | (merchant sim)    |                |
   |                 |--[OrderConfirmed]------------------>|
   |                 |--[OrderReady]-------------------->  (dispatch starts)
   |                 |<-[DeliveryAssigned]----------------|
   |                 | (delivery updates)                  |
   |                 |--[CourierDelivered]--------------->|
   |                 |<-[PaymentCompleted]----------------|
```

**Compensating transactions** for the cancellation saga:

| Step to undo | Compensating action | Trigger event |
|---|---|---|
| Pending offer | `dispatch-svc` cancels offer on `DeliveryCancelled` | `OrderCancelled` → `DeliveryCancelled` |
| Pending payment | `payment-svc` cancels `PaymentRecord` on `OrderCancelled` | `OrderCancelled` |
| Paid payment | Refund process (deferred to V2 / payment design) | `OrderCancelled` with status PAID |

In V1, only the PENDING compensation is implemented. PAID refund is explicitly deferred
and documented as a known gap (see [sequence diagrams, Cancellation](sequence-diagrams.md)).

---

## 5. V1 limitation: `@TransactionalEventListener` instead of outbox

In V1, Kafka events are published using Spring's `@TransactionalEventListener(phase = AFTER_COMMIT)`.
This means:

- Events are published **after** the DB transaction commits — no phantom events.
- If Kafka is unavailable at publish time, **the event is lost** (no retry, no persistence).
- A service crash between DB commit and Kafka publish also loses the event.

**Acceptable for V1** because: all data is simulated; a lost event in demo means a missed
notification or an unassigned delivery, both of which are visible in the operator console.

**For V2+**: replace with the **transactional outbox pattern**:
1. Write the event payload to an `outbox` table in the same DB transaction.
2. A separate process (Debezium CDC, or a polling publisher) reads the outbox and publishes
   to Kafka with at-least-once delivery.
3. Consumers remain idempotent (they already are by V1 design).

This is the standard pattern for reliable event-driven microservices and will be introduced
in phase 2 alongside the full offline mode and resilience requirements.
