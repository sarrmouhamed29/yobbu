# ADR 0006: Frontend

- **Status:** accepted
- **Date:** 2026-10-27
- **Deciders:** project owner

## Context

Yóbbu has three user interfaces in the first versions: customer (mobile), courier (mobile, offline-capable) and operator (desktop console). The merchant has no interface in the first version (scoping decision, `docs/PROJECT.md`). This ADR chooses the framework, state management and PWA approach.

Constraints:

- Strict TypeScript, controlled state management, accessibility, component and end-to-end tests (project requirements).
- Courier: offline operation, a local queue replayed in idempotent batches (ADR 0005), installable as a PWA.
- Modest devices and limited data: page weight and load time matter.
- API clients generated from OpenAPI; SSE client with authentication (ADR 0005 and 0007).
- An interactive map in the three interfaces.
- Multilingual interface later (French, English, then local languages).

## Options considered

### A. React with TypeScript and Vite

Server state with TanStack Query, a small client store (for example Zustand), a service worker with Workbox through a Vite plugin, IndexedDB for the offline queue.

- Pros: largest ecosystem, widely recognised, good libraries for maps (MapLibre), PWA, testing (Vitest, Testing Library, Playwright) and accessibility.
- Cons: building blocks (routing, forms, state) are assembled by hand, which needs discipline; bundle weight must be watched for modest devices.

### B. Angular

- Pros: complete out of the box (routing, forms, dependency injection, strict TypeScript, official service worker); structure close to what a Spring developer knows.
- Cons: heavier bundles, RxJS learning curve, less suited to modest devices.

### C. Vue 3 with Pinia and Vite

- Pros: gentle learning curve, light.
- Cons: smaller ecosystem and recognition than React; fewer map and PWA libraries.

### D. Svelte (SvelteKit)

- Pros: smallest bundles.
- Cons: thinner ecosystem, less common in job offers.

## Decision

**Option A: React with strict TypeScript and Vite.**

- **State:** three kinds of state kept apart. Server state (fetched data, cache, refresh) in TanStack Query; small local UI state in a light client store; the offline queue persisted in IndexedDB. Mixing them is the usual cause of unmanaged state.
- **Organisation:** one workspace (monorepo) with three applications (customer, courier, operator) sharing packages: generated API client, SSE client, UI components, translations. The courier app does not ship operator code and stays the lightest. Cost: three builds instead of one. Three applications confirmed by the owner at acceptance.
- **PWA:** service worker with Workbox for the customer and courier apps; the offline queue and its replay follow ADR 0005 (idempotent batches with client-generated identifiers). Background sync support varies by browser and is not assumed; replay on app foreground and on network return is the baseline.
- **Tests:** unit and component tests with Vitest and Testing Library, end-to-end tests with Playwright, automated accessibility checks in CI. Tool versions are pinned when the skeleton is created.
- **Internationalisation:** translation keys from the start (French first), even with a single language at launch.
- **Map:** MapLibre GL is the intended library. Base map tiles are an open point: public OpenStreetMap tile servers are not meant for heavy use, so a tile provider or self-hosting must be chosen separately (tracked in the plan, phase 1.4).

## Consequences

- Bundle size is a measured budget per application, checked in CI once the skeleton exists; no number is asserted here.
- Generated TypeScript clients from OpenAPI replace shared types with the backend (ADR 0002).
- Three applications mean three build and deployment units to describe in the pipeline and the infrastructure.
- Discipline is required on the assembled stack: a small set of agreed libraries, documented in the contributing guide.

## Revisit when

- Measured bundle weight or load time on a modest device cannot meet the budget with React after reasonable optimisation: reconsider a lighter framework for the courier app only.
- Maintaining three applications costs more than it saves: merge customer and courier into one application with role-based loading.
