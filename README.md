# Yóbbu

> From Wolof *yóbbu*: "to carry, to take away".

Yóbbu is an open source, real-time **delivery dispatch platform**: it decides which courier takes which delivery, then lets everyone follow the delivery live on a map. It targets African markets where meal and parcel delivery is booming (Dakar, Abidjan, Lagos, Douala, Lomé), with unstable networks and rules that change from one country to another.

This is a personal portfolio project meant to demonstrate senior-level full stack **and** DevOps engineering. It is not a commercial product.

## Status

**Phase 0: foundations (design and decisions).** There is no runnable code yet. The first iterations produce the design documents and architecture decisions; the application skeleton comes after.

Progress is tracked in [PLAN.md](PLAN.md).

## Scope of the first version

One complete delivery, from order to hand-over, in a single simulated country:

- Demo city: **Dakar**
- Deliveries: **meals and parcels** (one order model with a `type` field)
- Merchant side: **simulated**
- Payment: **simulated mobile money** (isolated module)
- Couriers: **simulated** (a simulator moves them on the map)

## Honest limits

- No real money, no real personal data: everything is simulated or fictitious.
- No real couriers.
- **No claim of regulatory compliance** (data protection, payments, local hosting rules). Design choices take these constraints into account, but nothing here is certified or audited.
- Anything simulated or untested is stated as such in the code and the documentation.

## Methodology

The project is run as a lightweight **Scrum without calendar dates**. This section is the reference; keep it in sync with [PLAN.md](PLAN.md) and `CLAUDE.md`.

- **Iterations, not dates.** An iteration has one demonstrable goal and ends when that goal is reached and verified. One active iteration at a time, at most three open backlog items.
- **Backlog.** The checklist in [PLAN.md](PLAN.md): five phases (foundations, first version, reliability and operations, production, extension). A phase starts only when the previous one is finished and verified.
- **Architecture Decision Records.** Every structuring decision gets an ADR in [docs/adr/](docs/adr/) (context, options considered, decision, consequences), written and validated **before** implementation. The stack is not assumed; it is chosen through ADRs.
- **Definition of Done** (every item): code, tests and docs in the same PR; green CI; no secret in the repository; one-command startup still works; anything simulated or untested is documented; ADR written when required.
- **Iteration review and retrospective.** Each iteration ends with a demo, then a three-line retrospective in the iteration log of [PLAN.md](PLAN.md).
- **Quality gates.** Conventional Commits, small coherent commits, changes through a branch and a well-described PR, tests written with the code, CI kept green.

## Documentation

| Document | Content |
| --- | --- |
| [PLAN.md](PLAN.md) | Phased backlog, methodology, ADR registry, iteration log |
| [docs/PROJECT.md](docs/PROJECT.md) | Product description, actors, constraints, quality bar (French) |
| [docs/adr/](docs/adr/) | Architecture decision records |
| [CLAUDE.md](CLAUDE.md) | Working rules for the AI assistant used on this project |

## Getting started

Not available yet. The goal is a single command (`docker compose up` or `make dev`) to run everything locally; this section will be filled in once the skeleton exists.

## License

Released under the [MIT License](LICENSE).
