# Non-functional requirements

Measurable targets for latency, throughput, availability and durability, with the way each one is verified. They guide the load tests and the first SLOs (see the observability plan, later in phase 0.3).

**Status:** draft, to be confirmed by the owner.

## Nature of these numbers

- They are **design targets** for the demo and the load tests. There is no market data behind them and they are not measurements.
- Nothing has been measured yet: the system does not exist. Each target says when and how it will be verified. Results will be published, whether they meet the target or not.
- The scale is deliberately small so that a demo is understandable: a hundred couriers can be followed on a map, a thousand cannot. The difficulty of the project is correctness under failure (no lost order, idempotency, offline replay), not raw throughput.

## Load profiles

Two profiles are used everywhere in the design and in the tests.

| Parameter | Demo profile (nominal) | Stress profile (5 times) | Basis |
| --- | --- | --- | --- |
| Active simulated couriers | 100 | 500 | Demo size, readable on a map |
| Orders at the lunch peak | 200 per hour (about 1 every 18 s) | 1,000 per hour (about 1 every 3.6 s) | About 2 deliveries per courier per hour |
| Position reports | 1 per courier every 5 s: about 20 per second | about 100 per second | Smooth tracking without excess data |
| Positions sampled to `dispatch` | 1 per courier every 15 s: about 7 per second | about 33 per second | Sampling decided in ADR 0003 |
| Live SSE streams | about 100 customers and up to 5 operators | about 500 customers and 5 operators | One stream per active delivery (ADR 0005) |
| Pushes to browsers | about 50 per second (1 update every 2 s per stream) | about 250 per second | Coalescing decided in ADR 0005 |

The stress profile exists to find the limits and to show a measured margin. If results show a lot of headroom, the stress profile can be raised later; it is not a promise about real traffic.

## Requirements

| ID | Requirement | Target | How and when verified |
| --- | --- | --- | --- |
| NFR-01 | Order creation latency (API, customer to acknowledgement) | p95 below 500 ms at demo profile | Load test, phase 3 |
| NFR-02 | Time from "order ready" to the first offer sent to a courier | p95 below 3 s at demo profile, when candidates exist | Load test and end-to-end test, phases 1 and 3 |
| NFR-03 | Time from a courier position received to the map update in a browser | p95 below 2 s at demo profile | Load test, phase 3 |
| NFR-04 | Behavior under the stress profile | No lost or duplicated order; latencies reported, degradation documented | Load test, phase 3 |
| NFR-05 | Availability of order placement and live tracking | 99.5 % per month on the online demo (about 3.6 hours of tolerated downtime) | SLO measurement once the demo is online, phase 3 |
| NFR-06 | Durability | No confirmed order lost (no data loss for committed orders), including when a service is killed mid-flow | Resilience tests, phase 2 |
| NFR-07 | Recovery of a failed service | Restarted automatically in under one minute; flows resume without manual action | Resilience tests, phase 2 |
| NFR-08 | Idempotency | Replaying the same command, event or offline batch has no additional effect | Unit, integration and contract tests, from phase 1 |
| NFR-09 | Initial JavaScript weight of the courier app | Below 250 KB compressed | Size budget checked in CI, from the first frontend skeleton |
| NFR-10 | One-command startup | The full stack starts with one command on a reference developer machine; startup time and memory are measured and recorded | Measured when the skeleton exists, phase 0.4 |

## Not set here

- **Memory and startup time per service,** and the reference machine for NFR-10: to be measured once the skeleton exists (ADR 0002, 0004 and 0007 say the same).
- **Offer response deadline for couriers:** decided with the dispatch algorithm (ADR 0008).
- **Data retention periods,** in particular for positions: decided in the data model work of phase 0.3.
- **Production autoscaling thresholds:** phase 3.

## Link with the decisions

- Keeping Kafka (ADR 0004) and PostgreSQL for positions (ADR 0003) is checked against NFR-03 and NFR-04: the revisit criteria of those ADRs depend on these results.
- NFR-05 to NFR-07 define the first SLOs for the observability plan.
