# Domain class diagram

Entities, value objects, aggregates and invariants for V1 (one simulated country, Dakar).

**Status:** validated by the owner on 2026-11-03.

Sources: [glossary](glossary.md), [domain breakdown](domain-breakdown.md),
[ADR 0001](../adr/0001-microservices.md), [ADR 0003](../adr/0003-postgresql-postgis.md).

See [class-diagram.svg](class-diagram.svg) — source: [class-diagram.puml](class-diagram.puml).

---

## Reading the diagram

| Stereotype | Meaning |
|---|---|
| `<<aggregate root>>` | Owns its consistency boundary. Modified only through its own methods. Persisted via its own repository. Cross-context references point to its `id` only. |
| `<<entity>>` | Has identity but is accessed through its aggregate root. Never referenced directly from outside the aggregate. |
| `<<value object>>` | No identity; immutable; compared by value. Copied, not referenced. |

---

## Aggregates per bounded context

### order — two aggregate roots

**`Order`** owns the commercial lifecycle: from the customer's request to the payment and the
final state of the delivery. Key transitions: `PLACED → CONFIRMED → READY → DISPATCHING →
IN_PROGRESS → COMPLETED / CANCELLED`. The simulated merchant drives `CONFIRMED` and `READY`.

**`Delivery`** owns the transport job: which courier carries it, at what times, and with what
history of assignments. Created when `Order` transitions to `READY`. `Delivery.orderId` references
`Order.id`; the two are separate aggregates with separate repositories in the same service.

`AssignmentRecord` is an entity inside the `Delivery` aggregate. Each time a courier is assigned
or replaced, a record is appended (immutable once written). This is the audit trail for the
transport job.

### dispatch — one aggregate root and one local read model

**`Offer`** owns the short-lived contract between the dispatch algorithm and one candidate courier.
At most one `PENDING` offer exists per delivery at any time. A new offer is created for the next
candidate on refusal or expiry.

**`CourierSnapshot`** is a local read model, not an aggregate root. It is upserted on each
`CourierPositionSampled` event from `tracking` (one per courier every 15 s, ADR 0003). `dispatch`
uses it for proximity queries and availability checks without a synchronous call to `tracking`.
The data is **eventually consistent**; staleness tolerance is set in ADR 0008.

### tracking — one entity

**`Position`** is append-only: one row per position report. It is never updated. The retention
period is defined in the data model work later in phase 0.3.

### payment — one aggregate root

**`PaymentRecord`** holds the simulated payment state. Duplicate callbacks from the simulated
provider are identified by `idempotencyKey` and silently ignored (ADR 0004).

### notification — one entity

**`NotificationRecord`** is written once per notification attempt and never updated (a failed send
creates a new record on retry). SMS and email channels are logged only in V1.

---

## Shared value objects

**`Location`**: latitude, longitude, optional written address, optional landmark. Used in `Order`
(pickup and drop-off points), `CourierSnapshot` (last known position) and `Position` (the reported
point). Copied by value into each aggregate.

**`Money`**: amount and currency. Simulated; no real financial operations (C-01).

---

## Key invariants (summary)

| Aggregate | Invariant |
|---|---|
| `Order` | `cancel()` only before pickup (statuses PLACED → DISPATCHING); `startDispatching()` only from READY; exactly one `Delivery` created per `Order` in V1 |
| `Delivery` | Transitions are strictly ordered (PENDING → ASSIGNED → PICKED_UP → DELIVERED); each reassignment appends an `AssignmentRecord` |
| `Offer` | `accept()` / `refuse()` only if PENDING; at most one PENDING offer per delivery; `idempotencyKey` prevents duplicate offers on retry |
| `PaymentRecord` | `complete()` / `fail()` only if PENDING; same `idempotencyKey` → idempotent, no second record |

---

## Decisions

Confirmed by the owner on 2026-11-03.

1. **`Order` and `Delivery` are separate aggregates** in the same `order` service. They have
   different lifecycles and the dispatch service references `deliveryId` directly. `Delivery`
   holds a `orderId` reference; `Order` does not reference `Delivery` directly.
2. **`Delivery` is created when `Order` becomes READY,** before dispatch starts. Its initial
   status is PENDING. This keeps the transport job separate from the commercial request from
   the beginning.
3. **`AssignmentRecord` is immutable** once written. Reassignment appends a new record; it does
   not update the previous one. This gives a full audit trail for free.
4. **`CourierSnapshot` is a local read model in `dispatch`,** not an aggregate root with a command
   interface. It is written only by event handlers. Its staleness tolerance is a parameter of the
   dispatch algorithm (ADR 0008, still to be written).
5. **`Position` is append-only.** The live position is the latest row, found by an indexed query
   on `(courierId, reportedAt DESC)`. No `UPDATE` ever runs on this table.
6. **All cross-context references use UUIDs.** No service reads another service's tables (ADR 0001).
   The diagram shows which IDs reference which entities for documentation purposes; these are
   not foreign keys in the database sense.
