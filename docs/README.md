# Yóbbu — documentation index

All design documents and architecture decision records for the Yóbbu project.

See [`../CLAUDE.md`](../CLAUDE.md) for working rules and [`../PLAN.md`](../PLAN.md) for
the phase tracker.

---

## Architecture decision records (`docs/adr/`)

| ADR | Title | Status | Linked diagrams |
|-----|-------|--------|----------------|
| [0001](adr/0001-architecture-microservices.md) | Microservices vs. modular monolith | accepted | [c4.md](design/c4.md), [infrastructure-view.md](design/infrastructure-view.md) |
| [0002](adr/0002-architecture-backend-java-spring.md) | Backend: Java + Spring Boot | accepted | [c4.md](design/c4.md) |
| [0003](adr/0003-database-postgresql-postgis.md) | Database: PostgreSQL + PostGIS | accepted | [data-model.md](design/data-model.md), [erd-logical.puml](design/erd-logical.puml) |
| [0004](adr/0004-messaging-kafka.md) | Messaging: Kafka | accepted | [event-catalogue.md](design/event-catalogue.md), [consistency-strategy.md](design/consistency-strategy.md) |
| [0005](adr/0005-realtime-sse.md) | Real-time: SSE + HTTP POST | accepted | [c4-containers.puml](design/c4-containers.puml), [tracking-api](design/openapi/tracking-api.yaml) |
| [0006](adr/0006-frontend-react-typescript-vite.md) | Frontend: React + TypeScript + Vite | accepted | [c4.md](design/c4.md) |
| [0007](adr/0007-authentication-keycloak.md) | Authentication: Keycloak OIDC | accepted | [threat-model.md](design/threat-model.md), [api-design.md](design/api-design.md) |
| [0009](adr/0009-diagrams-plantuml.md) | Diagram format: PlantUML | accepted | all `.puml` files |

---

## Design documents (`docs/design/`)

### Functional scope

| Document | Content |
|----------|---------|
| [glossary.md](design/glossary.md) | Ubiquitous language: course, dispatch, offre, point de retrait… |
| [use-cases.md](design/use-cases.md) | Use cases per actor with UML diagram |
| [non-functional-requirements.md](design/non-functional-requirements.md) | Latency targets, throughput, availability SLOs, volume estimates |
| [constraints-and-risks.md](design/constraints-and-risks.md) | Unstable networks, multi-country, PII, peak load — risk register |
| [domain-breakdown.md](design/domain-breakdown.md) | Bounded contexts, context map, ownership per aggregate |

### UML models

| Document | Content |
|----------|---------|
| [class-diagram.md](design/class-diagram.md) | Aggregates, entities, value objects, invariants (PlantUML) |
| [state-diagrams.md](design/state-diagrams.md) | State machines: Order, Delivery, Offer, CourierAvailability |
| [sequence-diagrams.md](design/sequence-diagrams.md) | Nominal flow, refusal, expiry, offline resync, payment callback, cancellation |
| [activity-dispatch.md](design/activity-dispatch.md) | Dispatch algorithm: candidates, scoring, fallback |
| [architecture-diagrams.md](design/architecture-diagrams.md) | Component and deployment diagrams |

### Data model

| Document | Content |
|----------|---------|
| [data-model.md](design/data-model.md) | Logical + physical model, full DDL per service (types, constraints, GiST indexes) |
| [erd-logical.puml](design/erd-logical.puml) / [.svg](design/erd-logical.svg) | Logical ERD — 9 tables, 5 schemas |
| [data-model-retention.md](design/data-model-retention.md) | Volume estimates, retention policies, Flyway zero-downtime rules |
| [data-classification.md](design/data-classification.md) | PII inventory, access control matrix, right-to-erasure gap |

### Architecture views

| Document | Content |
|----------|---------|
| [c4.md](design/c4.md) | C4 model narrative and decisions |
| [c4-context.puml](design/c4-context.puml) / [.svg](design/c4-context.svg) | C4 L1: system context |
| [c4-containers.puml](design/c4-containers.puml) / [.svg](design/c4-containers.svg) | C4 L2: containers |
| [c4-components.puml](design/c4-components.puml) / [.svg](design/c4-components.svg) | C4 L3: order-svc + dispatch-svc components |
| [functional-view.md](design/functional-view.md) | Capability matrix: 17 capabilities × 5 services × actor |
| [infrastructure-view.md](design/infrastructure-view.md) | Environments, storage, network flows |
| [infrastructure-view.puml](design/infrastructure-view.puml) / [.svg](design/infrastructure-view.svg) | Docker Compose network topology |
| [event-catalogue.md](design/event-catalogue.md) | 14 events, 4 Kafka topics, consumer matrix, schema evolution rules |
| [threat-model.md](design/threat-model.md) | STRIDE across 5 trust boundaries, risk + mitigation per threat |
| [consistency-strategy.md](design/consistency-strategy.md) | Transactions, eventual consistency, idempotency, choreography saga |

### API contracts

| Document | Content |
|----------|---------|
| [api-design.md](design/api-design.md) | Global REST conventions: versioning, auth, idempotency, RFC 7807, pagination |
| [openapi/order-api.yaml](design/openapi/order-api.yaml) | OpenAPI 3.1.0 — order-svc |
| [openapi/dispatch-api.yaml](design/openapi/dispatch-api.yaml) | OpenAPI 3.1.0 — dispatch-svc |
| [openapi/tracking-api.yaml](design/openapi/tracking-api.yaml) | OpenAPI 3.1.0 — tracking-svc (incl. SSE) |
| [openapi/payment-api.yaml](design/openapi/payment-api.yaml) | OpenAPI 3.1.0 — payment-svc |
| [conventions.md](design/conventions.md) | Hexagonal structure, naming (Java/REST/Kafka/TS), error handling, logging |

### Observability and testing

| Document | Content |
|----------|---------|
| [observability-plan.md](design/observability-plan.md) | Log events, Micrometer metrics, OTel traces, correlation ID, SLO-01/02/03 |
| [test-strategy.md](design/test-strategy.md) | Test pyramid, Testcontainers, Pact contracts, Playwright E2E, k6 load, resilience |

### Interfaces

| Document | Content |
|----------|---------|
| [user-journeys.md](design/user-journeys.md) | User journeys + low-fidelity wireframes for all 4 interfaces |
| [offline-design.md](design/offline-design.md) | Courier PWA offline mode: IndexedDB stores, position queue, sync, conflict rules |

### Design review

| Document | Content |
|----------|---------|
| [design-review.md](design/design-review.md) | Cross-check: NFRs → use cases → data, state/event consistency, open points |

---

## PlantUML diagrams

All `.puml` sources are in `docs/design/`. Rendered SVGs are committed alongside them.
To regenerate all SVGs:

```bash
bash scripts/diagrams.sh
```

Uses pinned image `plantuml/plantuml@sha256:9b5071ab56a4192aac458ab4b0d42261c22c8109695894ae5be6013c3be6cac1`.
