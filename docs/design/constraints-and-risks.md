# Constraints, assumptions and risks

What bounds the design (constraints), what the design takes for granted without proof yet (assumptions), and what could go wrong (risks) with how it is handled. Sources: `docs/PROJECT.md`, `CLAUDE.md`, ADR 0001 to 0009, [non-functional requirements](non-functional-requirements.md).

**Status:** draft, to be confirmed by the owner. The likelihood and impact ratings are an initial judgment, not measurements. Points that need a decision are listed in [Open points](#open-points).

## Constraints

Fixed by the owner, the context or earlier decisions. A design that breaks one of them is wrong.

| ID | Constraint | Source |
| --- | --- | --- |
| C-01 | Out of scope: real money, real personal data, real couriers (a simulator is used), and any claim of regulatory compliance. All of this must be stated plainly in the README. | `docs/PROJECT.md` section 5 |
| C-02 | One-command startup (`docker compose up` or `make dev`) must never break. | `CLAUDE.md` |
| C-03 | Few technologies, each justified: a brick is added only if removing it makes the product less reliable, less safe or non-compliant. | `CLAUDE.md` |
| C-04 | One developer, no calendar. One iteration at a time, at most three open cases; the first version stays narrow. | `PLAN.md` |
| C-05 | Unstable networks: a courier can lose the connection mid-delivery and no delivery may be lost. | `docs/PROJECT.md` section 4 |
| C-06 | Imprecise addresses: accept landmarks and a map point in addition to a written address. | `docs/PROJECT.md` section 4 |
| C-07 | Modest devices and limited data: interfaces stay light (see NFR-09). | `docs/PROJECT.md` section 4 |
| C-08 | Several languages over time (French first, then English and local languages). | `docs/PROJECT.md` section 4 |
| C-09 | Several countries over time, with their own rules, without rewriting the journey. | `docs/PROJECT.md` section 4 |
| C-10 | Open source (MIT license). No secret in the repository, ever. | `CLAUDE.md` |
| C-11 | Anything simulated, untested or not done is stated in the code, the docs and the README. | `CLAUDE.md` |
| C-12 | Development happens on Windows with Docker Desktop and Git Bash: scripts must work there (LF line endings, path handling). | Observed during ADR 0009 |
| C-13 | Repository language: code, commits and technical documentation in English. | `CLAUDE.md` |

## Assumptions

Taken as true for now. Each has a way to check it and a consequence if it turns out false.

| ID | Assumption | How to check, and when | If false |
| --- | --- | --- | --- |
| A-01 | The developer machine can run the full stack (six JVM services, Kafka, PostgreSQL, Keycloak). | Measure memory and startup with the skeleton, phase 0.4 (NFR-10). | Compose profiles, then the fallbacks of ADR 0001, 0004 and 0007. |
| A-02 | Positions sampled every 15 s are fresh enough for dispatch decisions. | Dispatch tests and load tests, phases 1 and 3. | Sample more often, or tolerate staleness explicitly in ADR 0008. |
| A-03 | Simulated couriers moving along straight lines or predefined paths look acceptable on the Dakar map (no routing engine, ADR 0003). | First demo, phase 1. | Predefined realistic paths, or revisit the routing engine decision. |
| A-04 | Base map tiles can be obtained at demo scale without breaking usage rules or cost limits. | Decide the tile source (open ADR in `PLAN.md`, phase 1.4). | Tile provider with a free tier, or self-hosting. |
| A-05 | The online demo can be hosted at a low cost with a reduced set of components. | Deployment ADR, phase 3. | Smaller demo profile, or a documented reduction of the demo. |
| A-06 | Username and password sign-in with fictitious accounts is enough for the demo (ADR 0007). | Review of the first version. | Add simulated OTP sign-in. |
| A-07 | Target browsers are current ones on mobile (Chromium and Safari); background synchronisation is not assumed to be available. | Offline design and tests. | Replay on foreground and on network return (ADR 0006). |

## Risk register

Likelihood and impact: **H** high, **M** medium, **L** low. Ordered by priority (likelihood and impact together).

| ID | Risk | L | I | Handling | Early signal | Tracked in |
| --- | --- | --- | --- | --- | --- | --- |
| R-01 | **Scope too large for a solo, calendar-free project:** first version includes two order types, simulated payment and six services. The first version never reaches a working delivery. | H | H | Order type is a single field; payment can fall back to simulated cash on delivery; thin vertical slice first; phase gates; one iteration at a time. | An iteration drifts, or more than three cases stay open. | `PLAN.md`, `docs/PROJECT.md` section 10 |
| R-02 | **Local stack too heavy:** six JVM services, Kafka and Keycloak exceed the machine, so one-command startup becomes unusable. | H | H | Measure at the skeleton; Compose profiles; fallbacks written in ADR 0001 (merge services), ADR 0004 (RabbitMQ), ADR 0007 (lighter identity provider). | Startup takes several minutes or the machine swaps. | NFR-10, A-01 |
| R-03 | **Distributed complexity slows delivery:** sagas, contracts and tracing absorb the effort that should go to dispatch. | M | H | Common service template; observability from the first inter-service call; merge services if two always change together (ADR 0001). | Most of an iteration goes to plumbing. | ADR 0001 |
| R-04 | **Security flaw in authentication or rights:** wrong role check, token mishandling, data of another user exposed. | M | H | Identity provider instead of custom code; validation and rights in every service; negative tests; threat model and dependency/image scans (cases in `PLAN.md`). | A negative test is missing or fails. | ADR 0007, phase 0.3 and 2 |
| R-05 | **Lost or duplicated courier actions after a network loss.** | M | H | Idempotency keys, batched replay, snapshot then stream, resilience tests (NFR-06 to NFR-08). | A replay test creates a duplicate. | ADR 0005, phase 2 |
| R-06 | **Hosting the online demo costs too much or cannot run the full stack.** | M | H | Deployment ADR early in phase 3; demo profile with fewer components, documented honestly. | Estimated cost exceeds the budget. | A-05 |
| R-07 | **Choices made partly to learn (microservices, Kafka) are judged as over-engineering.** | M | M | Each ADR states the trade-off and its revisit criteria; the README says so openly. | Reviewer feedback, or revisit criteria met. | ADR 0001, ADR 0004 |
| R-08 | **Stale positions degrade dispatch decisions** (eventual consistency between `tracking` and `dispatch`). | M | M | Timestamps on positions; staleness tolerated explicitly in ADR 0008; sampling rate adjustable. | Offers sent to couriers who are no longer nearby. | ADR 0003, A-02 |
| R-09 | **Live streams do not pass the gateway** (buffering, timeouts). | M | M | Test streaming through the gateway early in phase 1. | Updates arrive in bursts or connections drop. | ADR 0005 |
| R-10 | **Map tiles: usage rules or cost.** Public OpenStreetMap servers are not meant for heavy use. | M | M | Choose a provider or self-host (ADR in `PLAN.md`, phase 1.4). | Tiles blocked or rate-limited. | A-04 |
| R-11 | **Documentation drifts from the code** (ADR, diagrams, glossary). | M | M | End-of-phase cross review (criterion of phase 0.3); diagram freshness check in CI (phase 0.4); revisit criteria in ADRs. | A diagram contradicts the code. | `PLAN.md` phase 0.3 |
| R-12 | **Personal data handled carelessly,** now or when countries are added (local hosting rules). | L | M | Only fictitious data (C-01); data classification case in phase 0.3; no compliance claim. | Real-looking personal data appears in seeds or logs. | `PLAN.md` phase 0.3 and 4 |
| R-13 | **PostgreSQL cannot sustain position writes.** | L | M | Sampling and batching; early load test; introduce Redis with measurements (ADR 0003 trigger). | Write latency grows under the stress profile. | ADR 0003, NFR-04 |
| R-14 | **Windows tooling quirks** break scripts or builds (line endings, paths, Docker Desktop availability). | M | L | `.gitattributes`, scripts tested in Git Bash, CI on Linux as a second check. | A script fails only on one platform. | C-12 |
| R-15 | **Simulator looks artificial,** weakening the demo. | L | M | Realistic speeds and predefined paths; keep it simple and honest in the README. | Couriers move through obstacles on the map. | A-03 |
| R-16 | **Vulnerable dependencies or images** (supply chain). | L | M | Dependency and image scanning in CI (phase 1.7 and 2); pinned versions. | A scan reports a high severity finding. | `PLAN.md` phase 1.7, 2 |

## Open points

Proposed answers are in italics.

1. **Ratings are an initial judgment.** *Please adjust any likelihood or impact that feels wrong; the order of the register follows from them.*
2. **First risks to retire.** R-02 (heavy local stack) and A-01 can only be checked with the skeleton. *They are the first things to measure in phase 0.4, before building more.*
3. **Review cadence.** *Review this register when each phase closes, and when a revisit criterion of an ADR is met.* If adopted, this is a change to the method and must be added to the Methodology section of `README.md` and to `PLAN.md` in the same pull request.
