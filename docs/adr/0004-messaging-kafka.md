# ADR 0004: Messaging and asynchronous processing

- **Status:** proposed
- **Date:** 2026-10-27
- **Deciders:** project owner

## Context

ADR 0001 fixed a microservices architecture. This ADR first states whether asynchronous messaging is needed, then chooses the technology.

What must be asynchronous:

1. Domain events between services: order ready (`order` to `dispatch`), ride assigned (`dispatch` to `order`), payment confirmed (`payment` to `order`), notifications to send.
2. The courier position flow from `tracking` to `dispatch`, sampled (ADR 0003).
3. Delays: a ride offer expires after a few seconds and goes to the next courier.
4. Reliability: no loss when a service is down, messages are replayed, and a message processed twice has no double effect.

Test of the project rule ("if the brick is removed, does the product become less reliable?"): without a transport between services, `order` would call `dispatch` over HTTP, and an order would be blocked if `dispatch` is down at that moment, unless a retry queue is written by hand. The need is real.

## Options considered

### A. No broker: transactional outbox and HTTP delivery with retries

- Pros: no extra component.
- Cons: emitters must know their consumers (coupling), no fan-out or replay, the position flow fits poorly over HTTP, and it reimplements what a broker already does.

### B. RabbitMQ

- Pros: queues with acknowledgements, routing, native retry and dead-letter handling, light to run locally, good Spring support.
- Cons: no replayable log (a consumed message is gone), ordering guaranteed per queue only.

### C. Apache Kafka

- Pros: replayable log, partitioning by key (ordering per courier or per order), very high throughput, widely used in industry and well suited to learning event-driven architecture.
- Cons: heavier to run (memory, configuration) while six JVM services already load the machine. No native delayed delivery. Its distinctive strengths (replay, very high throughput) are not strictly required by the expected volume.

### D. NATS JetStream

- Pros: single light binary, persistent streams, replay.
- Cons: thinner Java and Spring ecosystem.

## Decision

**Option C: Apache Kafka, with Option B (RabbitMQ) as a defined fallback.**

The choice is partly a learning choice, as for ADR 0001: the owner wants hands-on experience with an event log. It is made knowing it is heavier than needed for the expected volume. The decision is explicitly experimental and reversible.

Rules that hold whatever the broker:

- **Transactional outbox:** an event is written in the same transaction as the data it describes, then published by a relay.
- **Idempotent consumers:** a table of processed message identifiers; delivery is at-least-once, never assumed exactly-once.
- **Retries with increasing delay and a dead-letter destination** for messages that keep failing.
- **Keys:** order events keyed by order identifier, position events keyed by courier identifier, so that ordering holds where it matters. Consumers also compare event timestamps and ignore stale positions.
- **Event envelope:** JSON with an explicit type, version, identifier and timestamp. No schema registry for now (a further component without proven need); compatibility is checked by contract tests, and events are documented (AsyncAPI or equivalent, tooling to confirm later).
- **Delays are not delegated to the broker:** `dispatch` stores the offer deadline in its database and a periodic job re-offers expired rides. This survives restarts and keeps business logic independent of the broker.
- **Synchronous HTTP** stays for reads and queries; asynchronous messaging is for commands and events that change state.
- **Boundary:** application code reaches the broker through a thin publisher/consumer layer (outbox relay, idempotent handler), not scattered client calls, so that a fallback to RabbitMQ touches that layer rather than business code.
- **Local setup:** a single broker node without replication (replication factor of 1 is a declared simplification of the local and demo setups; production topology is decided in the deployment ADR).

## Consequences

- Heavier local stack: memory and startup of the broker add to six JVM services. Compose profiles allow starting a subset.
- The full delivery history is stored in the services' databases, not in the log retention, so traceability does not depend on topic configuration.
- Kafka's operational complexity (partitions, consumer groups, offsets) becomes part of the learning scope and of the documentation.
- Version and distribution of Kafka are pinned and verified against official sources when the skeleton is created, not asserted here.

## Revisit when (fallback to RabbitMQ)

The thresholds are to be written down as numbers in an amendment of this ADR once the skeleton exists and measurements are available. The fallback is considered when:

- the full stack with the broker no longer starts or stays responsive with the one-command startup on a reference developer machine, even with reasonable profiles and settings;
- broker memory or startup time is identified as the main obstacle to phase goals or to the CI duration;
- load tests show that Kafka's features bring nothing measurable for the volumes involved while costing operation effort.

A fallback is recorded in a new ADR that supersedes this one.
