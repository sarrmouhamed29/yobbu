# ADR 0005: Real-time delivery to maps

- **Status:** proposed
- **Date:** 2026-10-27
- **Deciders:** project owner

## Context

Customers follow their courier on a map, operators watch all deliveries in progress, and couriers receive ride offers with a short deadline. This ADR chooses the protocol between browsers and the `tracking` service (and the `gateway` in front of it).

Traffic:

- Server to browser (downstream): courier positions, delivery state changes, ride offers. This is the dominant flow.
- Courier to server (upstream): positions and state updates. They can be batched and replayed, which the offline mode of the courier app requires.

Constraints:

- Unstable networks: automatic reconnection, resumption without losing a state change, low data usage.
- Upstream traffic must be idempotent and replayable after a network loss.
- Must work through the `gateway`, several `tracking` instances and a load balancer.
- No extra component if possible.

## Options considered

### A. Server-Sent Events downstream, HTTP POST upstream

- Pros: plain HTTP, built-in reconnection with resumption by event identifier, light on data, easy to load balance and to pass through the gateway. Batched, idempotent POST requests match the offline queue: it is flushed in batches when the network returns.
- Cons: one direction per connection. The browser's native `EventSource` cannot send an authorization header, so a `fetch`-based SSE client or a short-lived ticket is needed (see ADR 0007), never a token in the URL. Keep-alive messages are needed so intermediaries do not close idle connections.

### B. WebSocket

- Pros: bidirectional on one connection, lower per-message overhead, binary frames.
- Cons: stateful connection; resumption, reconnection, ordering and replay are written by hand; harder with proxies and load balancers; the offline queue still has to be built. Its main advantage (bidirectional) is not a need here.

### C. Polling

- Pros: simplest and most robust.
- Cons: latency, wasted requests, more data and load. A last-resort fallback, not a main choice.

### D. MQTT over WebSocket

- Pros: designed for unreliable networks (quality-of-service levels).
- Cons: an extra broker while Kafka already exists and browsers do not speak it. Rejected under the rule on infrastructure bricks.

## Decision

**Option A: SSE for downstream, HTTP POST for upstream.**

- **Reconnection:** on reconnect the client first fetches a snapshot of the current state through a REST call, then resumes the stream. For positions the latest one is enough. State changes and ride offers are persisted with an identifier, so none is lost; the stream carries event identifiers for resumption.
- **Fan-out:** each `tracking` instance consumes the sampled position events (ADR 0004) and pushes them only to clients subscribed to the matching delivery. Updates are coalesced (for example one update every one to two seconds per client) and sent as compact JSON. Exact figures are to be measured in load tests, not assumed here.
- **Operator console:** subscription to a map area, not to the whole flow.
- **Upstream:** courier positions and state updates are sent by HTTP POST, batched, each item carrying a client-generated identifier so the server can ignore duplicates after a replay.
- **Connection upkeep:** periodic keep-alive messages; the `gateway` must not buffer or time out streaming responses (to be verified, not assumed).
- **Authentication of the stream:** to be settled in ADR 0007 (fetch-based client or short-lived ticket); no token in the URL.
- **Scaling:** any `tracking` instance must be able to serve any subscriber; distribution of subscriptions across instances is studied in the production phase.

## Consequences

- No bidirectional channel: any future need for low-latency two-way messaging (for example chat between courier and customer) would need a new decision.
- The offline courier flow reuses the same upstream endpoints, with idempotent batches, instead of a separate mechanism.
- The gateway configuration for long-lived streaming responses is a risk to verify with a test early in phase 1.
- Load tests must report the number of concurrent streams, the position update rate pushed, and the end-to-end latency from position received to map update.

## Revisit when

- A real need for bidirectional, low-latency messaging appears: introduce WebSocket for that use case only.
- Measurements show that concurrent SSE connections or the gateway's handling of long-lived streams is a bottleneck that cannot be fixed by configuration.
