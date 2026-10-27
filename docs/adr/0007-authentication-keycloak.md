# ADR 0007: Authentication and identity

- **Status:** proposed
- **Date:** 2026-10-27
- **Deciders:** project owner

## Context

Accounts must be authenticated and role-based rights enforced across six services, three web applications (PWA) and an SSE stream. This ADR also closes the open point of ADR 0005: authenticating the SSE stream without a token in the URL.

Constraints:

- OAuth2/OIDC and role-based rights (project requirements): customer, courier, operator. The merchant is simulated in the first version.
- Do not write an authentication system by hand: this is where a mistake costs the most.
- One-command startup, so no dependency on an external account.
- The courier PWA works offline: the session must survive a network loss.
- No secret in the repository; rotation possible without rebuilding.
- No real personal data: fictitious accounts only.

## Options considered

### A. Keycloak, self-hosted identity provider

- Pros: complete OIDC standard, roles, account management, password policies, ready for a simulated SMS/OTP later. Services only validate tokens (Spring Security as resource server). Configuration can be versioned as a realm export file. Project rule check: removing it means writing authentication by hand, which is less safe, so the component is justified.
- Cons: one more heavy Java component in local memory (to be measured), upgrades to follow, configuration to master.

### B. Custom `identity` service with Spring Authorization Server

- Pros: full control, good learning, all Java.
- Cons: writing and maintaining an identity provider is a security risk, more code and one more service. Hard to defend for a project meant to show judgment.

### C. Managed identity provider (Auth0, Cognito or similar)

- Pros: no operations.
- Cons: external account and secrets, incompatible with one-command startup and with a self-contained open source project.

### D. JWT issued by a custom service, without OIDC

- Cons: does not meet the OAuth2/OIDC requirement. Rejected.

## Decision

**Option A: Keycloak.**

- **Application sign-in:** authorization code flow with PKCE (public client) for the three web applications; short-lived access tokens; refresh tokens with rotation. Tokens are kept in page memory, never in `localStorage`, to limit the impact of an XSS flaw, complemented by a strict content security policy.
- **Where tokens are validated:** the `gateway` performs a first check, but **every service also validates the token and enforces its own rights** (defense in depth). Global roles (`customer`, `courier`, `operator`) are checked coarsely at the entry point; ownership is enforced inside the service (a customer sees only their own orders, a courier only their own rides).
- **Service-to-service calls:** client credentials flow with one service account per service for synchronous HTTP calls. For Kafka, the event carries the identifier of the user concerned, not their token; broker protection belongs to the deployment ADR.
- **SSE stream (closes ADR 0005):** a `fetch`-based SSE client sends the `Authorization` header like other calls. The server limits the stream lifetime to the token lifetime; the client refreshes and reconnects before expiry, using resumption by event identifier. No token in the URL and no extra ticket mechanism.
- **Offline courier:** if the refresh token expires while offline, the local queue is kept and replayed after a new sign-in. If another account signs in on the device, the previous account's queue is discarded and reported, never replayed under a different identity.
- **Sign-in in the first version:** username and password with fictitious accounts provided by demo seed data. No real secret is committed; credentials come from example configuration files. SMS/OTP sign-in with a simulated provider is a future evolution, outside the first version.
- **Configuration as code:** the realm configuration is versioned in the repository without secrets; secrets are injected at runtime (to be detailed in the secrets work).

### Rejected for now: backend-for-frontend (BFF)

The `gateway` would hold tokens and give the browser a session cookie. It protects better against token theft and allows native `EventSource`, but the `gateway` becomes stateful (a session shared across instances, probably requiring Redis, a brick to justify) and offline courier use fits less well. To reopen if exposure to XSS becomes a main concern.

## Consequences

- Keycloak's memory and startup time add to the local stack; they are measured, not assumed. Compose profiles can start without it only for work that needs no sign-in.
- Every service needs token validation and authorization tests, including negative tests (wrong role, other user's resource).
- Page memory tokens mean a reload requires a silent re-authentication; the application must handle this smoothly, including offline.
- Keycloak version and image are pinned and checked against official sources when the skeleton is created, not asserted here.

## Revisit when

- Keycloak's weight prevents the one-command startup: look for a lighter identity provider before considering option B.
- XSS exposure of in-memory tokens becomes a concern: reconsider the BFF model with a stateless or externalised session, with its own ADR.
