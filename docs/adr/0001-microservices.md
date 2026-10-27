# ADR 0001: Microservices from the start

- **Status:** accepted
- **Date:** 2026-10-27
- **Deciders:** project owner

## Context

Yóbbu is a real-time delivery dispatch platform and a portfolio project (see `docs/PROJECT.md`). The deployment and decomposition model of the backend has to be fixed first, because it shapes messaging, real-time delivery, observability and deployment.

Constraints:

- One developer. Operating cost must stay bearable.
- One-command startup (`docker compose up` or `make dev`) must keep working.
- Different load profiles: thousands of courier positions per second (long-lived connections) versus a few orders per minute for business logic.
- An order must never be lost (idempotency, recovery after failure).
- Planned evolutions: per-country rules, offline courier app.
- Infrastructure bricks should be justified by the domain (`CLAUDE.md`).
- **Explicit goal of the owner: learn and demonstrate microservices.** This is a primary driver of the decision, not a side effect.

## Options considered

### A. Pure modular monolith

One process, modules with enforced boundaries.

- Pros: simple transactions, easy debugging, fast CI and startup, few moving parts.
- Cons: the position stream and the API scale together; weaker demonstration of autoscaling and of distributed concerns.

### B. Microservices

Independent services, each owning its data, communicating over APIs and events.

- Pros: independent scaling and deployment, clear ownership, direct practice of distributed-systems concerns (sagas, idempotency, contract tests, distributed tracing), which matches the owner's learning goal and the senior full stack + DevOps showcase.
- Cons: distributed consistency, inter-service contracts, several deployables, observability required just to debug, heavier local startup and CI. For one developer, a large share of the effort goes to plumbing.

### C. Modular monolith deployed in several roles

One codebase and image launched as `api`, `worker` and `realtime`.

- Pros: independent scaling of the real-time path at low cost; keeps simple transactions.
- Cons: does not give hands-on practice of service boundaries, which is the owner's stated priority.

## Decision

**Option B: microservices.**

The primary justification is the learning objective of the owner. The domain also supports it partially (the position stream has a distinct load profile; asynchronous reliability is central), but we do not claim that a monolith would be unfit for this scope. This is documented here on purpose, so that the choice is defended as a conscious trade-off.

### Initial decomposition (to be refined by later ADRs)

Kept small on purpose: too many services would hide the learning behind plumbing.

| Service | Responsibility |
| --- | --- |
| `gateway` | Single entry point: authentication check, routing, rate limiting |
| `order` | Orders (meal or parcel), order state machine, simulated merchant |
| `dispatch` | Courier selection, offers with short timeout, refusals, expiry |
| `tracking` | Courier positions (ingest, geo queries, live broadcast to maps) |
| `payment` | Simulated mobile money provider, idempotent callback |
| `notification` | Notifications to clients and couriers |

Identity is delegated to an identity provider (ADR 0007), not written as a service. The frontends (client, courier, operator) are separate from this list.

### Rules that follow

- Each service owns its data; no service reads another service's tables (physical layout decided in ADR 0003).
- Cross-service workflows (order, dispatch, payment) are asynchronous with idempotent handlers; the need and the technology are decided in ADR 0004.
- One monorepo, to keep tooling, CI and one-command startup manageable.
- Contract tests between services and distributed tracing are mandatory from the first inter-service call, not added later.

## Consequences

- The messaging ADR (0004) is now justified by the architecture and by the reliability requirement; it still has to choose the technology and compare it with simpler options.
- Observability (structured logs, metrics, traces) moves earlier in the plan: it is needed as soon as two services talk to each other.
- Local startup and CI get heavier; mitigations: monorepo, shared build tooling, Compose profiles to start only a subset of services.
- Phase 1 scope is more at risk. Mitigation: the first iteration implements one complete delivery through the minimum number of services; `payment` and `notification` can start as thin services.
- Who owns courier availability and the source of truth for locations is open: this ADR places live positions in `tracking`, and `dispatch` queries it. To be confirmed in ADR 0003 and 0004.

## Revisit when

- The overhead of operating the services blocks delivery of the phase goals: merge services (for example `payment` and `notification` into `order`) rather than add tooling.
- Two services always change and deploy together: they are probably one service.
