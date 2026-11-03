# API design — principles and conventions

API-first design for Yóbbu V1: the OpenAPI specifications are written before the code and
serve as the contract between the frontend, the gateway, and each backend service.

**Status:** validated by the owner on 2026-11-03.

OpenAPI sources: [`openapi/`](openapi/)

Sources: [C4 model](c4.md), [event catalogue](event-catalogue.md),
[consistency strategy](consistency-strategy.md),
[ADR 0007](../adr/0007-authentication-keycloak.md).

---

## 1. Global conventions

### Base URL and versioning

```
https://<host>/api/v1/<resource>
```

URI versioning: the major version is in the path. Minor additions (new optional fields,
new endpoints) are non-breaking and do not change the version. Breaking changes (field
removal, type change, endpoint removal) require a new major version path segment (`/api/v2/`).

In development: `http://localhost/api/v1/` (nginx → gateway).

### Authentication

Every endpoint (except `POST /payments/callback`, which is server-to-server) requires a
Bearer token issued by Keycloak:

```
Authorization: Bearer <JWT>
```

The gateway validates the JWT signature (JWKS), rejects expired tokens, and forwards the
decoded identity as an internal header to the services. Services trust this header without
re-validating the token.

### Idempotency

All state-changing requests (POST/PUT/PATCH that create or modify resources) must carry:

```
Idempotency-Key: <UUID v4>
```

The server returns the same response for duplicate requests carrying the same key.
Clients must generate a new key for each intentionally new request.
Endpoints that do not accept `Idempotency-Key` return `400 Bad Request` if the header
is present.

### Error format — RFC 7807 Problem Details

All error responses use the `application/problem+json` media type:

```json
{
  "type":     "https://yobbu.app/errors/order-not-found",
  "title":    "Order not found",
  "status":   404,
  "detail":   "No order a1b2c3d4-… exists for this user.",
  "instance": "/api/v1/orders/a1b2c3d4-…",
  "traceId":  "4bf92f3577b34da6a3ce929d0e0e4736"
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `type` | Yes | URI that uniquely identifies the error kind (stable across versions) |
| `title` | Yes | Human-readable, English, stable for a given `type` |
| `status` | Yes | HTTP status code (mirrors the response status) |
| `detail` | No | Instance-specific explanation; may include safe context |
| `instance` | No | The request path |
| `traceId` | Yes | OpenTelemetry trace ID for cross-referencing with logs |

Errors never include stack traces, internal class names, or PII.

### Standard HTTP status codes

| Code | When used |
|------|-----------|
| 200 OK | Successful GET, POST for idempotent duplicate |
| 201 Created | Successful resource creation (POST) |
| 204 No Content | Successful command with no response body (POST /{id}/cancel) |
| 400 Bad Request | Validation failure (missing field, bad format, business rule) |
| 401 Unauthorized | Missing or invalid JWT |
| 403 Forbidden | Valid JWT but insufficient role or resource ownership |
| 404 Not Found | Resource does not exist or not visible to the caller |
| 409 Conflict | Command rejected because the current state does not allow it (e.g., cancel after pickup) |
| 422 Unprocessable Entity | Syntactically valid but semantically invalid request |
| 429 Too Many Requests | Rate limit exceeded |
| 500 Internal Server Error | Unexpected server error |

### Pagination

List endpoints use **cursor-based pagination** (no offset):

```json
{
  "data": [ … ],
  "pagination": {
    "nextCursor": "eyJpZCI6Ii4uLiJ9",
    "hasMore": true,
    "pageSize": 20
  }
}
```

Query parameters: `pageSize` (default 20, max 100), `cursor` (from previous `nextCursor`).
Cursors are opaque base64 strings encoding the last seen `id` + sort key.

Rationale: offset pagination breaks when new records are inserted at the top of the result
set (a common occurrence in a delivery system). Cursors always point to the same position
even as new orders are created.

### Content negotiation

All endpoints accept and return `application/json` unless noted otherwise.
SSE endpoints return `text/event-stream`.

---

## 2. Service APIs

| Service | Spec file | Base path |
|---------|-----------|-----------|
| order-svc | [openapi/order-api.yaml](openapi/order-api.yaml) | `/api/v1/orders`, `/api/v1/deliveries` |
| dispatch-svc | [openapi/dispatch-api.yaml](openapi/dispatch-api.yaml) | `/api/v1/offers`, `/api/v1/couriers` (availability) |
| tracking-svc | [openapi/tracking-api.yaml](openapi/tracking-api.yaml) | `/api/v1/positions`, `/api/v1/couriers` (stream) |
| payment-svc | [openapi/payment-api.yaml](openapi/payment-api.yaml) | `/api/v1/payments` |

Notification endpoints are not exposed as a public REST API in V1: `notification-svc`
is a pure event consumer. A notification list endpoint is planned for V2 (the PWA
notification centre).

---

## 3. Decisions

Confirmed by the owner on 2026-11-03.

1. **URI versioning over header versioning.** URI versioning is simpler to route at the
   gateway level, visible in logs, and easier for client developers to test. Header-based
   versioning (`Accept: application/vnd.yobbu.v2+json`) is more RESTful but harder to
   debug and test.

2. **RFC 7807 for errors.** A standard format means clients can parse errors generically
   without service-specific error parsers. The `type` URI is stable even if the message
   changes; clients can key on `type` rather than parsing `detail`.

3. **Cursor-based pagination.** Delivery systems produce frequent inserts at the top of
   ordered result sets. Offset pagination would skip or repeat records during a page scan.
   Cursors are safe and composable with filtering.

4. **`Idempotency-Key` on all state-changing requests.** The mobile network context
   (African markets, unstable connections) makes retries common. Without idempotency keys,
   a retry after a timeout could create a duplicate order. The key is client-generated to
   avoid a round-trip for key generation.
