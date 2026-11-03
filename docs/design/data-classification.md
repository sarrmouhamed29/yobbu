# Data classification — personal data inventory

Case 5 of the design checklist: what personal data the system stores, why, for how long,
who can access it, and how the design limits exposure.

**Status:** validated by the owner on 2026-11-03.

**Scope reminder:** Yóbbu V1 uses only simulated data. No real users, no real couriers,
no real locations. This document describes the classification that must apply when real data
is introduced. It is written now so that the design does not inadvertently make GDPR-style
compliance harder.

Sources: [data model](data-model.md), [domain breakdown](domain-breakdown.md),
[docs/PROJECT.md](../PROJECT.md).

---

## 1. Classification levels

| Level | Definition |
|-------|------------|
| **Public** | No restriction. No user can be identified from this data alone. |
| **Internal** | Operational data. Not personal, but not published. Access limited to services that need it. |
| **Personal** | Data that directly or indirectly identifies a natural person (GDPR Art. 4). Treated with heightened care. |
| **Sensitive** | A subset of Personal that reveals location history or financial behaviour. Stricter access control and shorter default retention. |

---

## 2. What goes to Keycloak vs our databases

The key design decision is that Yóbbu's own databases do **not** store identity attributes
(name, email address, phone number). These live in Keycloak, which is the system of record
for user identity.

| Attribute | Where stored | Justification |
|-----------|-------------|---------------|
| Name | Keycloak | Identity only; our services never need it for their logic |
| Email | Keycloak | Authentication only |
| Phone number | Keycloak | Authentication / SMS auth; notification service reads it at send time via Keycloak user API, never persists it |
| Password hash | Keycloak | Never visible to our services |
| Role (customer / courier / operator) | Keycloak (JWT claim) | Verified at the gateway on each request |
| Customer UUID | `orders.customer_id` | Needed to query a customer's orders |
| Courier UUID | `deliveries.assigned_courier_id`, `assignment_records.courier_id`, `courier_snapshots.courier_id`, `positions.courier_id`, `offers.courier_id` | Needed for dispatch and position tracking |
| Operator UUID | Not stored | Operators act through the console; their identity is in the audit log of the gateway (access log) |

**Consequence:** deleting a user in Keycloak does not automatically remove their UUIDs from
our databases. A GDPR "right to erasure" request requires a separate process (documented in
CONTRIBUTING as a known gap for V1).

---

## 3. Personal data inventory by table

### `order` schema

#### `orders`

| Column | Classification | Justification |
|--------|---------------|---------------|
| `customer_id` | Personal | UUID linked to a real person via Keycloak |
| `pickup_point` | Sensitive | Customer's location at time of pickup (home, workplace) |
| `pickup_address` | Sensitive | Text form of pickup location |
| `pickup_landmark` | Sensitive | Free text that may reveal exact position |
| `dropoff_point` | Sensitive | Customer's destination |
| `dropoff_address` | Sensitive | Text form of destination |
| `dropoff_landmark` | Sensitive | Free text that may reveal exact position |
| `amount_value` | Internal | Financial behaviour pattern; not directly identifying |
| `type`, `status`, `eta_seconds` | Public | No personal data |
| `idempotency_key` | Internal | Client-generated token; no personal data |
| `created_at`, `updated_at` | Internal | Timestamps; not personal on their own |

**Retention:** indefinite for orders table itself (V1); a right-to-erasure process would
anonymise `customer_id` and nullify location columns.

#### `deliveries`

| Column | Classification | Justification |
|--------|---------------|---------------|
| `assigned_courier_id` | Personal | UUID linked to a courier |
| `order_id`, `status`, timestamps | Internal | No personal data |

#### `assignment_records`

| Column | Classification | Justification |
|--------|---------------|---------------|
| `courier_id` | Personal | UUID linked to a courier |
| `delivery_id`, timestamps, `end_reason` | Internal | Operational data |

#### `order_status_history`, `delivery_status_history`

All columns are Internal. No identity attributes.

---

### `dispatch` schema

#### `offers`

| Column | Classification | Justification |
|--------|---------------|---------------|
| `courier_id` | Personal | UUID linked to a courier |
| `delivery_id`, `status`, `deadline`, timestamps | Internal | Operational |

#### `courier_snapshots`

| Column | Classification | Justification |
|--------|---------------|---------------|
| `courier_id` | Personal | UUID linked to a courier |
| `last_known_position` | Sensitive | Real-time location of a courier |
| `last_position_at` | Personal | Timestamp of last location |
| `availability`, `active_deliveries`, `last_updated_at` | Internal | Operational |

**Retention:** row is upserted in place; no history. The Sensitive `last_known_position`
reflects only the current position, not a history. Position history lives in `tracking`.

---

### `tracking` schema

#### `positions`

| Column | Classification | Justification |
|--------|---------------|---------------|
| `courier_id` | Personal | UUID linked to a courier |
| `delivery_id` | Personal | Linkable to a customer via `deliveries` |
| `point` | Sensitive | Location of the courier at a specific moment |
| `reported_at`, `received_at` | Personal | Timestamps that together with location reveal movement patterns |
| `idempotency_key` | Internal | Deduplication token |

**Retention:** 30 days (see [data-model-retention.md](data-model-retention.md)).

This table has the highest personal data density in the system. It is the primary
reason for the 30-day retention policy and for the service-isolation architecture
(no other service schema can query it directly).

---

### `payment` schema

#### `payment_records`

| Column | Classification | Justification |
|--------|---------------|---------------|
| `order_id` | Personal | Linkable to a customer via `orders` |
| `amount_value` | Internal | Financial behaviour; not identifying on its own in a simulated context |
| `provider_ref` | Internal | Simulated reference; would be Sensitive in production |
| `status`, `amount_currency`, timestamps | Internal | Operational |

**Retention:** 5 years (mirrors financial regulatory expectation for future production use).

---

### `notification` schema

#### `notification_records`

| Column | Classification | Justification |
|--------|---------------|---------------|
| `target_user_id` | Personal | UUID linked to a user (customer or courier) |
| `payload` | Personal | May contain order status text; no raw PII (no names or locations in the payload) |
| `channel`, `event_type`, `status`, timestamps | Internal | Operational |

**Retention:** 90 days.

---

## 4. Access control by role

Each service's database user (`order_svc`, `dispatch_svc`, …) can only access its own schema.
Cross-schema queries are impossible at the DB layer.

At the application layer, API access is gated by the JWT role claim:

| Table / data | Customer | Courier | Operator | Service (internal) |
|---|:---:|:---:|:---:|:---:|
| Own orders, own delivery status | R | — | R/W | R/W |
| Other customers' orders | — | — | R | — |
| `courier_snapshots` | — | own | R | R/W |
| `positions` for own deliveries | Read latest | R/W | R | R/W |
| All `positions` | — | — | R | R/W |
| `payment_records` | own | — | R | R/W |
| `notification_records` | own | own | R | R/W |

Legend: R = read, W = write, — = no access, "own" = own records only.

---

## 5. Personal data minimisation decisions

1. **No PII stored in notification payloads.** The notification service receives an event type
   and an order or delivery ID. Payload templates are resolved at send time from order status;
   they never include names, phone numbers, or location text.

2. **Landmark fields are nullable.** The system works without a landmark. Collecting it is
   optional; if a user does not provide it, no extra PII is stored.

3. **`courier_snapshots` stores current position only.** The snapshot is overwritten on every
   update (upsert). Historical positions live only in the `positions` table under a 30-day TTL.

4. **Right-to-erasure gap acknowledged.** Deleting a Keycloak user does not cascade to our
   databases in V1. This gap must be addressed before production: a `DELETE /users/{id}` admin
   endpoint in each service that anonymises or deletes the user's personal data.
   Documented here rather than in code, so the gap is visible.
