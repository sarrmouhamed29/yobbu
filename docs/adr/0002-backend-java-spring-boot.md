# ADR 0002: Backend language and framework

- **Status:** proposed
- **Date:** 2026-10-27
- **Deciders:** project owner

## Context

ADR 0001 fixed a microservices architecture (`gateway`, `order`, `dispatch`, `tracking`, `payment`, `notification`). This ADR chooses the language and framework of those services.

Constraints:

- One developer, six services: shared tooling and little boilerplate matter.
- One-command startup must stay realistic with six services on a developer machine.
- Containers must be lean, multi-stage and non-root.
- `tracking` handles many positions per second over long-lived connections; `dispatch` runs timed offers with concurrency.
- OpenAPI, contract tests, OpenTelemetry and OAuth2/OIDC must be well supported.
- The frontend will be strict TypeScript, so shared types are a plus.
- The learning goal is microservices (ADR 0001), not a new language.
- The owner is most proficient in Java and Spring Boot.

## Options considered

### A. Go (standard library and a light router)

- Pros: static binaries, very small images, fast startup, low memory, native concurrency, strong cloud native ecosystem.
- Cons: more verbose, less ergonomic data access and migrations, a new language on top of the new architecture concepts.

### B. TypeScript on Node (Fastify or NestJS)

- Pros: one language with the frontend, shared types and schemas, fast iteration, good for I/O-bound services.
- Cons: heavier images, single-threaded CPU-bound work in `dispatch`, a different language from the owner's strongest one.

### C. Java with Spring Boot

- Pros: owner already productive, so learning effort goes to distributed-systems concerns. Mature ecosystem for the required features: Spring Security (OAuth2/OIDC resource server), Spring Cloud Gateway, Micrometer with OpenTelemetry, contract testing, schema migrations. Virtual threads (recent Java versions) cover the many-long-connections case of `tracking` without a reactive programming model. Widely recognised on a senior profile.
- Cons: heavier than Go or Node (memory per service, image size, startup time of several seconds). TypeScript clients cannot share types with the backend, so they are generated from OpenAPI.

### D. Polyglot

- Pros: best tool per service.
- Cons: double tooling for one developer; against "few, well-justified technologies". Not retained for the first versions; API contracts keep this option open later.

## Decision

**Option C: Java with Spring Boot for all backend services.**

- Hand-written code. JHipster (the owner has worked on a JHipster-based architecture) is used as an architectural reference only, not as a generator, so that every structural choice stays explicit and recorded in its own ADR.
- A current LTS version of Java and a current stable Spring Boot release; exact versions are pinned in the build when the skeleton is created and checked against the official release information at that time. They are not asserted in this ADR.
- Libraries for data access, messaging, security and observability are decided in ADR 0003, 0004, 0007 and the observability work, not here.

## Consequences

- Resource use is higher than with Go. Mitigations: measure real memory and startup instead of assuming, Compose profiles to start a subset of services, layered and slimmed container images (non-root, multi-stage), native compilation kept as an option only if measurements justify it.
- TypeScript clients for the frontend are generated from the OpenAPI specifications; the specification is the contract (ADR to confirm tooling later).
- A single language across services allows shared build tooling and a common service template, but shared libraries must stay minimal to avoid coupling services through code.
- Framework features that "just work" must not hide the learning goal: idempotency, retries and sagas are written and tested explicitly rather than hidden behind opaque configuration.

## Revisit when

- Measured memory or startup of the services blocks the one-command startup or the autoscaling demonstration: reconsider native compilation, or move `tracking` to another runtime behind its existing contract.
