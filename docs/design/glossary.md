# Domain glossary

Shared vocabulary of Yóbbu. One concept, one word: the English term is the one used in code, events, APIs and documentation; the French term is the one used in discussion and in the product copy.

**Status:** validated by the owner on 2026-10-28, including the five decisions listed in [Decisions](#decisions).

Scope: first version (one simulated country, Dakar; simulated merchant; simulated payment). Items marked *later* are part of the product description but not of the first version.

## Actors

| Term | French | Definition |
| --- | --- | --- |
| **Customer** | client | Person who places an order and follows its delivery. |
| **Courier** | livreur | Person who carries the goods from the pickup point to the drop-off point. In demos and load tests, couriers are simulated. |
| **Merchant** | marchand | Business that prepares a meal order and declares it ready. Simulated in the first version (no interface). |
| **Sender** | expéditeur | Person who hands over a parcel. For a parcel order, the sender plays the merchant's role (also simulated in the first version). |
| **Operator** | opérateur | Yóbbu staff member who watches all deliveries, handles blocked cases and follows indicators. |

## Orders and deliveries

| Term | French | Definition |
| --- | --- | --- |
| **Order** | commande | A customer's request for goods to be brought to them. Has a type, a pickup point, a drop-off point and a simulated amount. Owns the commercial life of the request (confirmation, readiness, payment, cancellation). |
| **Order type** | type de commande | `MEAL` (repas) or `PARCEL` (colis). A single order model with a type field; no behavior specific to a type in the first version. |
| **Delivery** | livraison, course | The transport job that fulfils an order: from pickup point to drop-off point, carried by one courier at a time. French uses *livraison* for the customer view and *course* for the courier view; both name the same concept. One order has one delivery in the first version. |
| **Order ready** | commande prête | State declared by the merchant when the goods can be collected. It triggers the dispatch. |
| **Pickup point** | point de retrait | Where the courier collects the goods (the merchant's location, or the sender's for a parcel). |
| **Drop-off point** | point de remise | Where the goods are handed over to the customer. |
| **Handover** | remise | Moment the courier gives the goods to the customer; closes the delivery. |
| **Cancellation** | annulation | Customer-initiated stop of an order, allowed until the courier has picked the goods up. |

## Dispatch

| Term | French | Definition |
| --- | --- | --- |
| **Dispatch** | dispatch, répartition | The process that decides which courier takes a delivery. Also the name of the service that runs it. |
| **Candidate** | candidat | An available courier near the pickup point who is considered for a delivery. |
| **Score** | score | Value computed for each candidate from distance, load and urgency (and possibly other factors), used to rank candidates. The formula is decided in ADR 0008. |
| **Offer** | proposition (de course) | A delivery proposed to one candidate courier, who may accept or refuse. |
| **Offer deadline** | délai de réponse | Time limit for the courier to answer an offer. Stored in the dispatch database (ADR 0004). |
| **Expiry** | expiration | An offer reaches its deadline without an answer; the delivery goes to the next candidate. |
| **Refusal** | refus | The courier declines an offer; the delivery goes to the next candidate. |
| **Assignment** | attribution | The accepted offer: the delivery is bound to a courier. |
| **Fallback** | repli | What the dispatch does when no candidate remains or none accepts (for example widen the search, then alert the operator). Exact rules: ADR 0008. |
| **Reassignment** | réaffectation | Operator action replacing the courier of a delivery in progress. *Later.* |
| **Forced assignment** | choix forcé | Operator action assigning a specific courier, bypassing the scoring. *Later.* |
| **Blocked delivery** | situation bloquée | A delivery that cannot progress: no courier available, courier unreachable, customer absent. Raises an alert for the operator. |
| **Alert** | alerte | Signal shown to the operator about a blocked delivery. *Later.* |

## Courier state

| Term | French | Definition |
| --- | --- | --- |
| **Availability** | disponibilité | Whether a courier accepts new offers: `AVAILABLE` or `UNAVAILABLE`, set by the courier. Owned by the dispatch service (ADR 0003). |
| **Load** | charge | Number of active deliveries a courier currently carries. Input of the score. |
| **Position** | position | A geographic point reported by a courier's device, with a timestamp. |
| **Last known position** | dernière position connue | Most recent position of a courier, kept by the dispatch service from sampled position events. |
| **Offline mode** | mode hors-ligne | Courier app behavior when the network is lost: it keeps working locally and keeps a queue of actions. *Later.* |
| **Action queue** | file d'actions | Ordered list of actions (state updates, positions) made while offline, replayed when the network returns. *Later.* |
| **Replay** | rejeu | Sending the queued actions again after a network loss, in batches, without duplicate effects. |
| **Idempotency key** | clé d'idempotence | Client-generated identifier attached to an action so the server can ignore a duplicate (ADR 0005). |

## Places and tracking

| Term | French | Definition |
| --- | --- | --- |
| **Location** | localisation, point | A place given by a point on the map, optionally with a written address. |
| **Landmark** | repère | Free-text indication to find a place ("in front of the pharmacy"), used because addresses are imprecise. |
| **Live tracking** | suivi en direct | Display of the courier's movement on a map for a delivery. |
| **Subscription** | abonnement | A browser's connection to the live stream for one delivery (customer) or one map area (operator), over SSE (ADR 0005). |
| **ETA** | heure d'arrivée estimée | Estimated arrival time at the drop-off point. In the first version computed from the straight-line distance and a speed factor; this is a declared simplification (ADR 0003). |
| **Zone** | zone | A geographic area of operation. Rules may differ per zone or country. *Later.* |

## Payment

| Term | French | Definition |
| --- | --- | --- |
| **Payment** | paiement | Simulated mobile money payment attached to an order. No real money (out of scope). |
| **Payment status** | statut de paiement | `PENDING`, `PAID` or `FAILED`. |
| **Callback** | rappel | Notification sent by the simulated provider to the payment service; handled idempotently. |

## Notifications and feedback

| Term | French | Definition |
| --- | --- | --- |
| **Notification** | notification | Message sent to a customer, courier or operator about a state change. Channels: in-app, SMS and email. SMS and email are simulated in the first version. |
| **Rating** | note | Customer's score of a completed delivery. *Later.* |
| **Report** | signalement | Customer's complaint about a delivery. *Later.* |

## Country rules

| Term | French | Definition |
| --- | --- | --- |
| **Country** | pays | Market with its own zones, hours, currency and rules. Only one simulated country in the first version. *Later: several.* |
| **Country rules** | règles par pays | Configuration that adapts the same journey to a country without rewriting it. *Later.* |
| **Currency** | devise | Money unit of a country (XOF/FCFA, NGN, GMD). Amounts are simulated. |

## Traceability and technical terms

| Term | French | Definition |
| --- | --- | --- |
| **Audit trail** | journal d'audit | Complete, ordered history of the state changes of a delivery, stored in the services' databases. |
| **Correlation id** | identifiant de corrélation | Identifier propagated across services so an order can be followed end to end in logs and traces. |
| **Event** | événement | Immutable fact published by a service (for example "order ready") through Kafka (ADR 0004). |
| **Outbox** | boîte d'envoi | Table in a service's database where events are written in the same transaction as the data, then relayed to Kafka (ADR 0004). |
| **Service** | service | One of the six deployable backend units: `gateway`, `order`, `dispatch`, `tracking`, `payment`, `notification` (ADR 0001). |

## Decisions

These decisions shape the models that follow. They were confirmed by the owner; the italic text states what remains to be defined later.

1. **Order and Delivery are two concepts.** The order is the commercial request; the delivery is the transport job with its assignment history. *One order has exactly one delivery in the first version; a reassignment changes the courier of the same delivery.* This also matches the four separate state diagrams planned (order, delivery, offer, courier availability).
2. **"Course" and "livraison" name the same concept (Delivery).** The English word *ride* is not a domain term. ADR 0003, 0004 and 0005 use it loosely ("ride offer"); *a later clean-up commit can align their wording to Delivery and Offer without changing any decision*.
3. **Courier load is a count of active deliveries,** not a weight or volume, in the first version. *Maximum number of simultaneous deliveries per courier to be set in ADR 0008.*
4. **A parcel has no merchant;** the sender plays that role and is simulated like the merchant. *If a parcel needs a real sender or a proof of handover, it moves to phase 2 (see `docs/PROJECT.md`, section 10).*
5. **Cancellation** is allowed until pickup, as in the product description. *What happens to a simulated payment already made is to be defined with the order state diagram.*
