# Threat model — STRIDE analysis

Security threat analysis at the five key trust boundaries of Yóbbu V1.

**Status:** validated by the owner on 2026-11-03.

**Scope reminder:** Yóbbu V1 uses simulated data only. No real money, no real personal data,
no real couriers. This document describes threats that would exist in a production deployment.
Mitigations marked `V1` are implemented for V1; those marked `V2+` or `Phase 3` are deferred.

Sources: [C4 model](c4.md), [infrastructure view](infrastructure-view.md),
[data classification](data-classification.md),
[ADR 0007](../adr/0007-authentication-keycloak.md).

---

## Trust boundaries

Five boundaries where an attacker can intercept, inject, or escalate:

| Boundary | Description |
|----------|-------------|
| **B1** — Internet → API Gateway | All external HTTP/HTTPS traffic |
| **B2** — API Gateway → Backend Services | Authenticated, internally routed requests |
| **B3** — Backend Services → Kafka | Event publishing and consuming |
| **B4** — Backend Services → PostgreSQL | Data read/write |
| **B5** — Payment Provider → Payment Service | Simulated provider callback |

---

## STRIDE table

### B1 — Internet → API Gateway

| Threat | STRIDE | Risk | Mitigation | Status |
|--------|--------|------|------------|--------|
| Attacker sends forged JWT signed with a fake key | **S**poofing | High | Gateway validates JWT signature against Keycloak JWKS; rejects any token with an unknown `kid` | V1 |
| Attacker intercepts credentials in transit | **T**ampering / **I**nfo Disclosure | High | TLS required for all external connections (dev: HTTP on localhost only; staging/prod: HTTPS) | Phase 3 |
| Attacker replays a stolen JWT | **S**poofing | Medium | JWTs have short expiry (15 min access token); refresh tokens are rotated on use | V1 (Keycloak config) |
| DDoS or request flood | **D**enial of Service | Medium | Rate limiting at gateway (per IP and per user); `spring-cloud-gateway` rate limiter | V1 (configured) |
| Customer reads another customer's order | **E**levation of Privilege | High | Gateway forwards user identity in header; each service checks ownership before returning data | V1 |
| Operator endpoint accessible by a customer | **E**levation of Privilege | High | Role claim (`CUSTOMER`, `COURIER`, `OPERATOR`) checked per endpoint; enforced by Spring Security | V1 |
| Sensitive data in logs / error responses | **I**nfo Disclosure | Medium | Structured logs omit PII; error responses return only error codes, not stack traces | V1 |

### B2 — API Gateway → Backend Services

| Threat | STRIDE | Risk | Mitigation | Status |
|--------|--------|------|------------|--------|
| A service trusts a gateway header it received but the request bypassed the gateway | **S**poofing | Medium | Services are not exposed outside backend-net; in dev, gateway is the only ingress on port 80 | V1 |
| Malicious input passes validation | **T**ampering | Medium | Input validated at controller level (Bean Validation); coordinates validated as valid lat/lon ranges | V1 |
| A service acts on an expired or revoked token | **S**poofing | Low | Gateway validates token before forwarding; services trust the forwarded identity header (no secondary validation in V1) | Acceptable for V1 |
| Injection (SQL, JSON) via request fields | **T**ampering | High | Parameterised queries (JPA/JDBC); no dynamic SQL construction; address/landmark fields stored as text, not executed | V1 |

### B3 — Backend Services → Kafka

| Threat | STRIDE | Risk | Mitigation | Status |
|--------|--------|------|------------|--------|
| Service publishes to a topic it does not own | **E**levation of Privilege | Medium | ACLs per service user (each service authenticates with its own Kafka principal); V2+ (Kafka auth not configured in V1 dev) | V2+ |
| A consumer processes a duplicate event and creates a side effect twice | **T**ampering | High | Idempotency keys on every event; consumers deduplicate by `eventId` (see [event catalogue](event-catalogue.md)) | V1 |
| Kafka topic retention expires before a consumer processes all events | **D**enial of Service | Low | 72h retention in V1; consumers catch up before that; if a consumer falls behind, an alert fires | V1 (72h config) |
| Malicious event injected directly into Kafka | **S**poofing | Medium | In production, Kafka listens on the internal network only; no external access; in V1 dev, port 9092 is exposed for tooling only | Phase 3 |

### B4 — Backend Services → PostgreSQL

| Threat |ITAGE | Risk | Mitigation | Status |
|--------|--------|------|------------|--------|
| One service reads another service's schema | **E**levation of Privilege | High | Per-service DB users with `USAGE` only on their own schema; no cross-schema SELECT grants | V1 |
| SQL injection via unsanitised input | **T**ampering | High | Parameterised JDBC/JPA; no string concatenation in queries | V1 |
| Sensitive data at rest is readable by a sysadmin | **I**nfo Disclosure | Low | Encryption at rest for production (managed Postgres with disk encryption); V1 dev uses local Docker volume (acceptable) | Phase 3 |
| DB credentials in environment variables or logs | **I**nfo Disclosure | High | Secrets provided via Docker Compose `.env` file (not committed); in Phase 3 via Vault or managed secrets | V1 (.env) / Phase 3 (Vault) |

### B5 — Payment Provider → Payment Service

| Threat | STRIDE | Risk | Mitigation | Status |
|--------|--------|------|------------|--------|
| Attacker forges a payment confirmation callback | **S**poofing | High | Callback validated with HMAC signature from shared secret (V2+); in V1 simulated callback comes from within the service itself — no external caller | V1 (simulated; no real attack surface) |
| Provider sends duplicate callback (retry) | **T**ampering | High | `idempotency_key` on `payment_records`; duplicate returns 200 OK without re-processing | V1 |
| Provider callback carries a different amount than the order | **T**ampering | Medium | Payment service compares callback amount with the order amount stored at payment creation time; rejects if mismatched | V1 |

---

## Open security items for V2+

| Item | Priority |
|------|----------|
| Kafka ACLs (per-service Kafka principals, topic-level ACLs) | High |
| HMAC validation of payment provider callbacks | High |
| TLS for all internal service communication (mTLS with service accounts) | Medium |
| PostgreSQL encryption at rest (managed cloud DB with disk encryption) | Medium |
| Vault or equivalent for secret management; rotation without container rebuild | High (Phase 3) |
| Dependency vulnerability scanning in CI (OWASP Dependency-Check or Trivy) | V1 (Phase 0.4) |
| Container image scanning (Trivy, Grype) | V1 (Phase 0.4) |
| Static analysis (SpotBugs + Find Security Bugs) | V1 (Phase 0.4) |

---

## What is not a threat in V1 (simulation scope)

- **No real PII**: all customer and courier data is synthetic. A data breach exposes no real
  personal information.
- **No real money**: simulated mobile money. Financial fraud is not possible.
- **No regulatory compliance claimed**: GDPR-style compliance is a design target, not a
  V1 guarantee (see [data classification](data-classification.md)).
