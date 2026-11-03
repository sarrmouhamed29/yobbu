# Observability plan

Logs, metrics, distributed traces, correlation ID, and first SLOs for Yóbbu V1.

**Status:** validated by the owner on 2026-11-03.

Sources: [non-functional requirements](non-functional-requirements.md),
[event catalogue](event-catalogue.md), [conventions](conventions.md),
[C4 model](c4.md).

---

## 1. Principles

Three pillars are required before any service can be considered production-ready:
1. **Logs** — what happened in each service (structured JSON, searchable)
2. **Metrics** — aggregated measurements over time (Prometheus, Grafana)
3. **Traces** — end-to-end timing for a single request or event across all services
   (OpenTelemetry, Jaeger)

A fourth pillar connects them:

4. **Correlation** — every log line, metric label and trace span for a single delivery
   carries the same `traceId` so an operator can follow one delivery from `POST /orders`
   to the final notification.

---

## 2. Structured logs

Log format is defined in [conventions.md](conventions.md). This section defines what
business events must be logged at `INFO` level in each service.

### Required INFO log events

| Service | Event | Mandatory fields |
|---------|-------|-----------------|
| order-svc | Order placed | `orderId`, `customerId`, `type` |
| order-svc | Order status changed | `orderId`, `fromStatus`, `toStatus`, `triggerEvent` |
| order-svc | Delivery created | `orderId`, `deliveryId` |
| dispatch-svc | Dispatch started | `deliveryId`, `candidateCount` |
| dispatch-svc | Offer created | `offerId`, `deliveryId`, `courierId`, `deadlineSeconds` |
| dispatch-svc | Offer answered | `offerId`, `result` (ACCEPTED / REFUSED / EXPIRED) |
| dispatch-svc | Delivery assigned | `deliveryId`, `courierId` |
| dispatch-svc | Delivery blocked | `deliveryId`, `reason` |
| tracking-svc | Position stored | `courierId`, `idempotencyKey` (no lat/lon — PII) |
| tracking-svc | Batch processed | `courierId`, `stored`, `deduplicated`, `total` |
| payment-svc | Payment record created | `paymentId`, `orderId` |
| payment-svc | Payment completed | `paymentId`, `orderId` |
| payment-svc | Duplicate callback ignored | `paymentId`, `idempotencyKey` |
| notification-svc | Notification dispatched | `notificationId`, `targetUserId`, `channel`, `eventType` |
| notification-svc | Notification failed | `notificationId`, `channel`, `reason` |

**Coordinates are never logged** (lat/lon are sensitive location data).

---

## 3. Metrics

Stack: **Micrometer** (Spring Boot auto-configuration) → **Prometheus** (scrape) →
**Grafana** (dashboards and alerts).

### Standard metrics (all services, auto-configured)

| Metric | Type | Labels |
|--------|------|--------|
| `http_server_requests_seconds` | Histogram | `service`, `method`, `uri`, `status` |
| `jvm_memory_used_bytes` | Gauge | `service`, `area` |
| `jvm_threads_live` | Gauge | `service`, `state` |
| `hikaricp_connections_active` | Gauge | `service`, `pool` |
| `hikaricp_connections_pending` | Gauge | `service`, `pool` |

### Custom business metrics

| Metric | Type | Labels | Service |
|--------|------|--------|---------|
| `yobbu_orders_total` | Counter | `type` (MEAL/PARCEL) | order-svc |
| `yobbu_orders_by_status` | Gauge | `status` | order-svc |
| `yobbu_dispatch_duration_seconds` | Histogram | — | dispatch-svc |
| `yobbu_dispatch_attempts_total` | Counter | `result` (ASSIGNED/BLOCKED) | dispatch-svc |
| `yobbu_offer_response_time_seconds` | Histogram | `result` (ACCEPTED/REFUSED/EXPIRED) | dispatch-svc |
| `yobbu_positions_received_total` | Counter | `mode` (single/batch) | tracking-svc |
| `yobbu_positions_deduplicated_total` | Counter | — | tracking-svc |
| `yobbu_active_deliveries` | Gauge | — | tracking-svc |
| `yobbu_notifications_sent_total` | Counter | `channel`, `status` | notification-svc |

### Kafka consumer metrics (all consumers)

| Metric | Type | Labels |
|--------|------|--------|
| `kafka_consumer_records_lag` | Gauge | `service`, `topic`, `partition` |
| `kafka_consumer_fetch_latency_avg` | Gauge | `service`, `topic` |

Consumer lag is the most important Kafka metric: if lag grows, events are not being
processed and eventual consistency windows grow.

---

## 4. Distributed traces

Stack: **OpenTelemetry Java agent** (auto-instrumentation) → **OTLP** →
**Jaeger** (collector + UI, development).

### Propagation

- **HTTP**: W3C `traceparent` header propagated by the gateway and between services.
- **Kafka**: trace context stored in Kafka record headers (`traceparent`, `tracestate`)
  by the OpenTelemetry Kafka instrumentation.

This ensures that a trace started by `POST /orders` is continued in every Kafka consumer
that processes events from that order, all the way to the notification send.

### Minimum spans for a nominal delivery

| Span | Service | Operation |
|------|---------|-----------|
| `POST /api/v1/orders` | gateway | HTTP inbound |
| `placeOrder` | order-svc | Command handler |
| `INSERT orders` | order-svc | DB write |
| `publish OrderPlaced` | order-svc | Kafka produce |
| `consume OrderReady` | dispatch-svc | Kafka consume |
| `startDispatch` | dispatch-svc | Business logic |
| `findCandidates` | dispatch-svc | PostGIS query |
| `INSERT offers` | dispatch-svc | DB write |
| `consume DeliveryAssigned` | order-svc | Kafka consume |
| `consume DeliveryAssigned` | tracking-svc | Kafka consume |
| `consume OfferCreated` | notification-svc | Kafka consume |
| `sendNotification` | notification-svc | HTTP outbound to stub |

A single delivery trace spans ~10–12 services calls and is readable as a Gantt chart in
Jaeger, showing where time is spent and where bottlenecks occur.

### Span attributes (required on every span)

```
service.name         = "order-svc"
deployment.environment = "development"
yobbu.orderId        = "a1b2c3d4-…"    (when applicable)
yobbu.deliveryId     = "e5f6g7h8-…"    (when applicable)
yobbu.courierId      = "i9j0k1l2-…"    (when applicable)
```

`orderId` and `deliveryId` must be added to spans as custom attributes from the first
HTTP handler onwards, so that Jaeger's search can find all spans for a given delivery.

---

## 5. Correlation ID

The trace ID from OpenTelemetry (`traceId`) is the correlation ID. It is:

1. Generated by the gateway on the first inbound request.
2. Propagated to every service via W3C `traceparent` HTTP header.
3. Propagated to every Kafka consumer via record headers.
4. Written to every log line via `MDC` (Mapped Diagnostic Context).
5. Included in every error response body as `traceId`.

A support team can take `traceId` from a user-reported error response and find all
related logs across all services in a single Grafana Loki / OpenSearch query.

---

## 6. First SLOs (V1 targets)

These are the first three SLOs. They align with the non-functional requirements in
[non-functional-requirements.md](non-functional-requirements.md).

### SLO-01 — API availability

**Target:** 99.5% of requests to `/api/v1/**` (excluding `5xx` responses) over a rolling
7-day window.

```
availability = (total_requests - 5xx_requests) / total_requests
```

Measured via `http_server_requests_seconds_count{status=~"5.."}` in Prometheus.
Alert when availability drops below 99.5% over 15 minutes.

### SLO-02 — Dispatch latency

**Target:** p95 dispatch duration ≤ 3 s (from `OrderReady` event received to `DeliveryAssigned`
event published), measured over a rolling 1-hour window.

```
dispatch_p95 = histogram_quantile(0.95, yobbu_dispatch_duration_seconds_bucket)
```

Alert when p95 exceeds 3 s over 5 minutes.

### SLO-03 — Position processing latency

**Target:** p95 time from position reported by the courier to position available in the
`courier_snapshots` table ≤ 5 s (covers: HTTP receive → store → publish
`CourierPositionSampled` → dispatch consumer upserts snapshot).

Measured as the gap between `reported_at` (device clock) and `last_position_at` in the
snapshot table, sampled via a dedicated Prometheus gauge written by dispatch-svc.

---

## 7. Development observability stack

All tools are included in Docker Compose:

| Tool | Container | Port | Purpose |
|------|-----------|------|---------|
| Prometheus | `prometheus` | 9090 | Metric scraping and storage |
| Grafana | `grafana` | 3000 | Dashboards and alert rules |
| Jaeger | `jaeger` | 16686 (UI), 4317 (OTLP) | Distributed trace visualisation |
| Loki + Promtail | `loki`, `promtail` | 3100 | Log aggregation from Docker containers |

Grafana datasources: Prometheus + Loki + Jaeger (linked from trace to logs via `traceId`).

Minimum dashboards for V1:
1. **System overview** — request rate, error rate, latency (p50/p95/p99) per service
2. **Delivery funnel** — orders placed / dispatched / completed / cancelled per hour
3. **Kafka lag** — consumer lag per topic, coloured by service
4. **Active deliveries map** — live count (optional; pulled from a metrics endpoint)
