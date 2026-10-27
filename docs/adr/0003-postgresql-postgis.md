# ADR 0003: Database and geospatial data

- **Status:** accepted
- **Date:** 2026-10-27
- **Deciders:** project owner

## Context

ADR 0001 fixed a microservices architecture where each service owns its data. Five services store data: `order`, `dispatch`, `tracking`, `payment` and `notification`. This ADR chooses the storage, the isolation between services, and the way geospatial queries are done. It also settles two points ADR 0001 left open: who owns courier availability, and whether a cache is needed.

Constraints:

- Mastered SQL: indexes, transactions, geospatial queries, versioned migrations (project requirements).
- A service never reads another service's tables.
- `dispatch` must quickly find available couriers around a merchant.
- `tracking` receives many positions per second, but only the latest position per courier matters live.
- A second storage technology is accepted only if justified.
- One-command startup: a single database server locally.

## Options considered

### A. PostgreSQL with PostGIS for everything

One logical database and one dedicated role per service, in a single PostgreSQL instance for development and demo (instances can be split in production). GiST indexes for proximity search. Latest positions are upserted, one row per courier; history is sampled.

- Pros: one data technology; reliable transactions for order, ride and payment; mature geospatial queries; strong Spring support.
- Cons: the volume of position writes is the weak point and must be measured under load.

### B. PostgreSQL with PostGIS, plus Redis for live positions

- Pros: very fast position writes, cache usable elsewhere.
- Cons: a second technology to run, back up and secure, without evidence that it is needed.

### C. MongoDB with geospatial indexes

- Pros: flexible document model, good write throughput.
- Cons: weaker fit for the transactional order/payment core, and SQL mastery has to be shown anyway.

## Decision

**Option A: PostgreSQL with PostGIS.**

- **Isolation:** one database and one role per service, with separate credentials. No cross-database access; services use APIs or events. The isolation is verifiable, not only a convention. A single instance is used locally; separate instances per service remain possible in production.
- **Migrations:** Flyway, one independent set of versioned migrations per service.
- **Courier availability:** `dispatch` owns the courier state (available or not, current load, last known position). It maintains it from sampled position events published by `tracking`. The proximity search is therefore a local indexed query, without a network call on the assignment path. `tracking` owns the raw position stream, history and live broadcast.
- **Cache:** no distributed cache for now. In-memory caches are used only where a need is identified (token validation keys, rarely changed per-country configuration). Redis would be justified only by one of these triggers, backed by load test numbers:
  1. PostgreSQL cannot sustain the target rate of position writes;
  2. shared rate-limiting counters across several `gateway` instances cannot be handled acceptably otherwise (decided in the security and API ADRs).
- **Out of scope:** route computation and arrival-time estimation. In the first version, a straight-line distance with a speed factor may be used and must be declared as a simplification. A routing engine would be a separate brick to justify later.

## Consequences

- Position traffic from `tracking` to `dispatch` must be sampled and dimensioned in ADR 0004 (messaging).
- Data in `dispatch` is eventually consistent with `tracking`; the dispatch algorithm (ADR 0008) must tolerate slightly stale positions.
- Load tests must report position write throughput and proximity query latency, since they decide whether option B is ever needed.
- Backups, restoration and zero-downtime migrations are handled in the production phase, per database.
- Per-country data residency, if studied later, can be approached by deploying database instances per region, since services already own separate databases.

## Revisit when

- A load test shows PostgreSQL cannot sustain the position write rate after sampling and batching: introduce Redis for live positions, with the measurements in a new ADR.
- Operating one database per service in production becomes costly without benefit: merge instances (keeping separate databases and roles), not data ownership.
