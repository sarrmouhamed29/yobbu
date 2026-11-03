# Data model — migration, volume and retention

Case 3 of the design checklist: Flyway migration strategy, per-table volume estimates,
and retention policies.

**Status:** validated by the owner on 2026-11-03.

Sources: [data model](data-model.md), [ADR 0002](../adr/0002-architecture-microservices.md),
[docs/PROJECT.md](../PROJECT.md).

---

## 1. Volume estimates

All estimates are for Dakar V1 demo scale unless marked otherwise.

### Assumptions

| Parameter | Demo (V1) | Stress test |
|-----------|-----------|-------------|
| Active couriers | 100 | 500 |
| Orders per day | 200 | 5 000 |
| Position reporting interval | 5 s (when moving) | 5 s |
| Active delivery duration | 30 min avg | 30 min avg |
| Peak hours per day | 4 h | 4 h |
| Notifications per order | ~6 | ~6 |

### Table-by-table estimates

#### `positions` (dominant table)

```
positions / s  = active_couriers × (1 position / 5 s)
             = 100 × 0.2 = 20 rows/s  (demo)
             = 500 × 0.2 = 100 rows/s (stress)

Row size       ≈ 100 bytes (id UUID 16 B + courier_id 16 B + delivery_id 16 B
                            + point 48 B + 3 timestamps 24 B)

Volume/day     = 20 × 86 400 × 100 B  ≈ 173 MB/day  (demo)
             = 100 × 86 400 × 100 B ≈ 864 MB/day  (stress)

With 30-day retention:
             ≈ 5 GB  (demo)
             ≈ 26 GB (stress)
```

#### `orders` + `deliveries`

```
Rows/day       = 200 orders, 200 deliveries  (demo)
Row size       ≈ 300 bytes (orders with geography columns)
Volume/day     < 1 MB  (negligible)
With 2-year retention: < 200 MB
```

#### `assignment_records`

```
Rows/day       ≈ 200–220 (1–1.1 records per delivery in V1)
Volume/day     < 0.1 MB  (negligible)
```

#### `order_status_history` / `delivery_status_history`

```
Rows per order     ≈ 5 (order) + 3 (delivery) = 8 rows
Rows/day           = 200 × 8 = 1 600
Volume/day         < 0.5 MB  (negligible)
With 2-year retention: < 400 MB
```

#### `offers`

```
Rows/day       ≈ 210 (some deliveries need 1–2 offers)
Volume/day     < 0.1 MB  (negligible)
```

#### `courier_snapshots`

```
Row count      = number of couriers (≤ 100 in demo; max 500)
Total size     < 1 MB  (static; upserted in place)
```

#### `payment_records`

```
Rows/day       = 200  (one per order)
Volume/day     < 0.1 MB  (negligible)
With 5-year retention (regulatory): < 100 MB
```

#### `notification_records`

```
Rows/day       = 200 × 6 ≈ 1 200
Volume/day     < 2 MB  (payload is short JSON)
With 90-day retention: < 180 MB
```

### Summary

| Table | Growth rate | Dominant? |
|-------|-------------|-----------|
| `positions` | 173 MB/day (demo) | **Yes** |
| All others combined | < 3 MB/day | No |

`positions` is the only table that requires a retention policy. All others can grow
indefinitely at demo scale without concern.

---

## 2. Retention policies

| Table | Retention | Rationale |
|-------|-----------|-----------|
| `positions` | 30 days | Enough for operational investigation; volume would grow unbounded otherwise |
| `orders`, `deliveries` | Indefinite (V1) | Business history; small volume |
| `assignment_records` | Indefinite | Audit trail for every delivery |
| `order_status_history` | Indefinite | Audit trail |
| `delivery_status_history` | Indefinite | Audit trail |
| `offers` | 90 days | Short-lived records; 90 days covers any dispute window |
| `courier_snapshots` | N/A (no retention) | Upserted; always one row per courier |
| `payment_records` | 5 years | Simulated payments; mirrors financial regulatory expectation |
| `notification_records` | 90 days | Operational logs; no long-term value |

### Position cleanup strategy (V1)

In V1, a scheduled job runs daily and deletes positions older than 30 days:

```sql
-- Run daily by a scheduled job (pg_cron or application scheduler)
DELETE FROM "tracking".positions
WHERE received_at < NOW() - INTERVAL '30 days';
```

This is simple and sufficient for demo scale. If volume reaches stress levels, switch to
PostgreSQL declarative range partitioning on `reported_at` (monthly partitions): dropping a
partition is instantaneous and avoids a full-table `DELETE`. See Decision 1 below.

---

## 3. Migration strategy

### Tool: Flyway (per service)

Each service owns its migrations under `db/migrations/`:

```
services/
  order/
    db/migrations/
      V1__initial_schema.sql
      V2__add_landmark_columns.sql   ← future
  dispatch/
    db/migrations/
      V1__initial_schema.sql
  tracking/
    db/migrations/
      V1__initial_schema.sql
  payment/
    db/migrations/
      V1__initial_schema.sql
  notification/
    db/migrations/
      V1__initial_schema.sql
```

Flyway runs at service startup (`flyway.baseline-on-migrate = true` for the first deploy
against a pre-existing schema). Migration checksums are enforced: modifying an applied
migration fails the startup.

### Zero-downtime migration rules

These rules apply to all future migrations that change existing tables:

1. **Never remove a column in the same migration that stops using it.** First deploy the code
   that stops reading/writing the column, then remove the column in a follow-up migration.
2. **Never rename a column directly.** Add the new column, migrate the data in a background
   job, switch the code to the new column, drop the old column.
3. **Never add a NOT NULL column without a DEFAULT** unless the table is empty or the migration
   backfills values atomically.
4. **Index creation uses `CREATE INDEX CONCURRENTLY`** (not inside a transaction) to avoid
   locking the table. Flyway's `CONCURRENTLY` support requires wrapping the statement in a
   `non-transactional` migration (set `-- flyway:executeInTransaction=false`).
5. **Enum extensions** (adding a value to a CHECK constraint): update the CHECK constraint
   with `ALTER TABLE … DROP CONSTRAINT …` then `ADD CONSTRAINT …`. Do this in a single
   transaction; it takes a brief table lock but does not rewrite rows.

### Schema ownership

| Service | Schema | Database user |
|---------|--------|---------------|
| order | `order` | `order_svc` |
| dispatch | `dispatch` | `dispatch_svc` |
| tracking | `tracking` | `tracking_svc` |
| payment | `payment` | `payment_svc` |
| notification | `notification` | `notification_svc` |

Each service user has `CONNECT`, `USAGE` on its schema, and `SELECT/INSERT/UPDATE/DELETE` on
its tables. It has no access to other schemas. The migration user (`flyway_owner`) holds
`CREATE` on the schema and runs only at startup.

---

## 4. Decisions

Confirmed by the owner on 2026-11-03.

1. **Defer positions partitioning to phase 0.4 load tests.** Declarative range partitioning
   on `reported_at` is the right strategy at volume, but it adds schema complexity and a
   `PARTITION BY` clause must be defined at table creation time (migrating later requires
   recreating the table). The load test target (R-02 in phase 0.4) will confirm whether the
   simple `DELETE`-based cleanup is sufficient or whether partitioning is necessary before
   launch.

2. **Per-service schemas in one PostgreSQL instance (V1), separate instances later.** Running
   all schemas in one Postgres instance keeps the demo simple (`docker compose up`). Because
   no cross-schema FK constraints exist, extracting a service to its own instance requires only
   removing the schema from the shared instance and pointing the service at the new one. This
   migration is linear work, not a redesign.

3. **Flyway over Liquibase.** Flyway's SQL-first model (plain `.sql` files) means migrations
   are readable without a tool, diffable in git, and runnable manually in psql during debugging.
   Liquibase's XML/YAML format adds a layer that obscures the actual DDL. For a portfolio
   project that values transparency, Flyway is the better fit.
