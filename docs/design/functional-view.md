# Functional view

Capabilities per actor and the services that support them. Answers the question: "to implement
this capability, which services must work?"

**Status:** validated by the owner on 2026-11-03.

Sources: [use cases](use-cases.md), [domain breakdown](domain-breakdown.md),
[C4 model](c4.md).

---

## Capability map

Each row is one functional capability from the use cases. Columns show which backend services
are involved (P = primary owner, C = consumer/contributor). Capabilities with no V1 column
are out of scope for V1.

| Capability | order | dispatch | tracking | payment | notification | V1 |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **Customer** | | | | | | |
| Place an order (CUS-01) | P | | | C | C | ✓ |
| Track delivery on map (CUS-02) | C | | P | | | ✓ |
| Receive notifications at each step (CUS-03) | C | C | | C | P | ✓ |
| Cancel order before pickup (CUS-06) | P | C | | C | C | ✓ |
| View order history (CUS-04) | P | | | | | ✓ |
| Rate and report after delivery (CUS-05) | P | | | | C | V2 |
| **Courier** | | | | | | |
| Toggle availability (COU-01) | | P | C | | | ✓ |
| Receive and answer offer (COU-02) | | P | | | C | ✓ |
| Confirm pickup (COU-03) | P | | | | C | ✓ |
| Confirm delivery (COU-04) | P | | | C | C | ✓ |
| View itinerary and useful info (COU-05) | P | | P | | | ✓ |
| Report position while offline (COU-06 partial) | | | P | | | ✓ (positions only) |
| Full offline mode + sync (COU-06 full) | P | C | P | | C | V2 |
| View daily earnings and history (COU-07) | P | | | C | | V2 |
| **Merchant (simulated in V1)** | | | | | | |
| Confirm order (MER-01) | P | | | | C | ✓ (simulated) |
| Declare order ready (MER-02) | P | C | | | C | ✓ (simulated) |
| Track incoming courier (MER-03) | | | P | | | V2 |
| View order history (MER-04) | P | | | | | V2 |
| **Operator** | | | | | | |
| View live delivery map (SYS-01) | C | C | P | | | ✓ |
| View delivery list and status (SYS-02) | P | C | | | | ✓ |
| See alert on blocked delivery (SYS-04) | P | P | | | P | ✓ |
| Force-assign a courier (SYS-03) | P | P | | | C | V2 |
| View basic metrics (SYS-05) | P | C | C | C | C | ✓ (minimal) |

---

## Dependency analysis

### Critical path: place → dispatch → track

The nominal delivery path touches four services in sequence:

```
order → [OrderReady event] → dispatch → [DeliveryAssigned event]
      → order (delivery status) + tracking (SSE routing)
      → [CourierPositionSampled] → dispatch (snapshot update)
```

A failure in any of these four services blocks the nominal path. All others
(payment, notification) are non-blocking: the delivery proceeds if they are
temporarily unavailable.

### Services with no external dependency at request time

`dispatch-svc` reads only its local `courier_snapshots` table at dispatch time.
It does not call `tracking-svc` synchronously. This is the key design decision
in [ADR 0003](../adr/0003-database-postgresql-postgis.md) (CourierSnapshot local
to dispatch).

### Payment and notification are decoupled

Payment completion (`PaymentCompleted`) and delivery completion (`CourierDelivered`)
are independent. A delivery can complete before payment (normal for mobile money
callbacks) or after. The order service handles both events independently.

Notification is fire-and-forget: `notification-svc` consumes events and sends
notifications without any response expected. A notification failure does not affect
the delivery state.

---

## Functional interfaces by actor

### Customer PWA

Screens: order form, live map, order status, notification centre, order history.

The live map subscribes to `tracking-svc` SSE stream for the assigned courier's position.
Position updates arrive every ~5 s while the courier is moving.

### Courier PWA

Screens: availability toggle, incoming offer card (with countdown), delivery details
(map + pickup info), status buttons (Picked up / Delivered), earnings (V2).

The offline position queue stores positions in IndexedDB when the network is unavailable.
On reconnect, it sends a `POST /positions/batch`. No other capability requires offline
support in V1 (full offline mode is V2).

### Operator Console

Screens: live map of all active deliveries, alert list (blocked deliveries), delivery
table (filterable by status), minimal metrics.

In V1 the operator can see everything but cannot act (no force-assign). Alerts are
visible; resolution requires human intervention outside the system.
