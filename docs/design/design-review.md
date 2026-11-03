# Design review — cross-check and consistency

Relecture croisée de l'ensemble de la conception (phase 0.3) avant de démarrer
l'implémentation. Ce document sert de critère de sortie de la phase de conception.

**Status:** completed on 2026-11-03.

---

## 1. Couverture exigences → cas d'utilisation

Chaque exigence non fonctionnelle est tracée jusqu'au cas d'utilisation et au composant
qui la satisfait.

| Exigence | Cas d'utilisation | Composant | Vérifié |
|----------|------------------|-----------|---------|
| R-01 Dispatch p95 ≤ 3 s | UC-D01 Lancer le dispatch | dispatch-svc, PostGIS, Kafka | ✅ |
| R-02 Position p95 ≤ 5 s | UC-T01 Recevoir une position | tracking-svc → Kafka → dispatch-svc | ✅ |
| R-03 Reconnexion hors-ligne | UC-T02 Synchroniser les positions | `POST /positions/batch` + `position_queue` | ✅ |
| R-04 Aucune position perdue | UC-T02 | IndexedDB + idempotence serveur | ✅ |
| R-05 Aucune commande perdue | UC-O01, UC-D01 | `@TransactionalEventListener` + Kafka retry | ✅ |
| NFR-01 Disponibilité 99,5 % | Tous les parcours | Gateway + health checks + SLO-01 | ✅ |
| NFR-05 Légèreté mobile | UC-C01 à UC-C06 | PWA, Vite bundle, lazy loading prévu en V2 | ✅ partiel |
| NFR-10 Démarrage en une commande | — | `docker compose up` (hors périmètre phase 0.3) | ⏳ phase 0.4 |
| SEC-01 Aucun secret dans le dépôt | — | Fichiers `.env.example`, pas de valeurs | ✅ |
| SEC-02 Chiffrement des communications | — | HTTPS / TLS à la frontière (phase 3) | ⏳ phase 3 |

---

## 2. Couverture cas d'utilisation → séquences

Chaque cas d'utilisation a au moins un diagramme de séquence nominal. Les cas alternatifs
(refus, expiration, panne) sont couverts pour les flux critiques.

| Cas d'utilisation | Séquence nominale | Cas alternatifs |
|------------------|------------------|----------------|
| UC-O01 Passer une commande | `sequence-diagrams.md` §1 | — |
| UC-D01 Dispatch et proposition | `sequence-diagrams.md` §2 | Refus (§3), Expiration (§3) |
| UC-D02 Accepter une offre | `sequence-diagrams.md` §2 | Offre expirée (§3) |
| UC-T01 Réception de position | `event-catalogue.md` §CourierPositionSampled | — |
| UC-T02 Sync hors-ligne | `sequence-diagrams.md` §5, `offline-design.md` §5 | Conflit (§6) |
| UC-P01 Paiement simulé | `sequence-diagrams.md` §4 | Doublon callback (§4) |
| UC-C01 Annulation client | `sequence-diagrams.md` §5 | — |
| UC-OP01 Vue opérateur | `user-journeys.md` §4 | Réaffectation (hors V1) |

---

## 3. Couverture données : chaque entité a un propriétaire

Aucune donnée n'est partagée entre services via une clé étrangère en base de données.
Les références croisées sont des UUID copiés (pas des FK inter-schémas).

| Entité | Service propriétaire | Schéma DB | Référencée par |
|--------|---------------------|-----------|----------------|
| Order | order-svc | `order` | delivery (order_id), payment (order_id) |
| Delivery | order-svc | `order` | dispatch (delivery_id), tracking (delivery_id) |
| Offer | dispatch-svc | `dispatch` | — |
| CourierState | dispatch-svc | `dispatch` | — |
| CourierSnapshot | dispatch-svc | `dispatch` | tracking (courierId — UUID copié) |
| PositionSample | tracking-svc | `tracking` | — |
| TrackingRecord | tracking-svc | `tracking` | — |
| Payment | payment-svc | `payment` | — |
| Notification | notification-svc | — (pas de DB en V1) | — |

Règle vérifiée : aucun `FOREIGN KEY` ne traverse les frontières de schéma dans le DDL
de `data-model.md`.

---

## 4. Cohérence entre modèles

### 4.1 Diagramme de classes ↔ modèle de données

| Classe (class-diagram.md) | Table(s) (data-model.md) | Cohérence |
|--------------------------|--------------------------|-----------|
| `Order` | `order.orders` | ✅ Champs identiques |
| `Delivery` | `order.deliveries` | ✅ |
| `Offer` | `dispatch.offers` | ✅ Contrainte unique PENDING vérifiée |
| `CourierState` | `dispatch.courier_states` | ✅ |
| `CourierSnapshot` | `dispatch.courier_snapshots` | ✅ UPSERT sur `courier_id` |
| `PositionSample` | `tracking.position_samples` | ✅ Index unique `idempotency_key` |
| `TrackingRecord` | `tracking.tracking_records` | ✅ |
| `Payment` | `payment.payments` | ✅ |

### 4.2 Diagrammes d'états ↔ catalogue d'événements

Chaque transition dans les diagrammes d'états (`state-diagrams.md`) est déclenchée par
un événement Kafka documenté dans `event-catalogue.md`.

| Transition | Déclencheur (événement Kafka) | Cohérence |
|-----------|-------------------------------|-----------|
| Order: `PENDING` → `CONFIRMED` | `OrderPlaced` → MerchantSimulator → `OrderConfirmed` | ✅ |
| Order: `CONFIRMED` → `READY` | `OrderReady` (publié par MerchantSimulator) | ✅ |
| Order: `READY` → `ASSIGNED` | `DeliveryAssigned` (consommé par order-svc) | ✅ |
| Order: `ASSIGNED` → `PICKED_UP` | HTTP `POST /orders/{id}/pickup` | ✅ |
| Order: `PICKED_UP` → `DELIVERED` | HTTP `POST /orders/{id}/deliver` | ✅ |
| Order: tout → `CANCELLED` | HTTP `POST /orders/{id}/cancel` | ✅ |
| Delivery: `PENDING` → `ASSIGNED` | `DeliveryAssigned` | ✅ |
| Offer: `PENDING` → `ACCEPTED` | HTTP `POST /offers/{id}/accept` | ✅ |
| Offer: `PENDING` → `REFUSED` | HTTP `POST /offers/{id}/refuse` | ✅ |
| Offer: `PENDING` → `EXPIRED` | Scheduler dispatch-svc (deadline dépassée) | ✅ |
| CourierState: `AVAILABLE` ↔ `UNAVAILABLE` | HTTP `PUT /couriers/{id}/availability` | ✅ |

### 4.3 Diagramme de classes ↔ OpenAPI

Les schémas de réponse des specs OpenAPI correspondent aux agrégats du diagramme de
classes.

| Agrégat (classe) | DTO OpenAPI | Champs manquants ou divergents |
|-----------------|------------|-------------------------------|
| `Order` | `OrderResponse` (order-api.yaml) | ✅ Aucun |
| `Delivery` | `DeliveryResponse` (order-api.yaml) | ✅ |
| `Offer` | `OfferResponse` (dispatch-api.yaml) | ✅ |
| `PositionSample` | `PositionResponse` (tracking-api.yaml) | ✅ lat/lon/reportedAt |
| `Payment` | `PaymentResponse` (payment-api.yaml) | ✅ |

### 4.4 Catalogue d'événements ↔ stratégie de cohérence

Chaque événement Kafka dans `event-catalogue.md` a un consommateur documenté dans
`consistency-strategy.md`. Tous les consommateurs ont une règle d'idempotence.

| Événement | Consommateurs documentés | Idempotence vérifiée |
|-----------|--------------------------|---------------------|
| `OrderPlaced` | dispatch-svc, notification-svc | ✅ `ORDER_ID` comme clé |
| `OrderReady` | dispatch-svc | ✅ |
| `DeliveryAssigned` | order-svc, tracking-svc, notification-svc | ✅ |
| `OfferCreated` | notification-svc | ✅ |
| `OfferExpired` | order-svc | ✅ |
| `CourierPositionSampled` | dispatch-svc | ✅ UPSERT snapshot |
| `PaymentCompleted` | order-svc | ✅ état idempotent |

---

## 5. Points ouverts et limitations documentées

Ces éléments sont connus et acceptés pour la V1 :

| Point | Décision V1 | Phase cible |
|-------|------------|-------------|
| Outbox pattern (garantie d'exactly-once publish) | `@TransactionalEventListener(AFTER_COMMIT)` — risque de perte sur crash après COMMIT | Phase 2 |
| Espace marchand réel | Simulateur ; l'interface cible est dans `user-journeys.md` §3 | Phase 2 |
| Réaffectation par l'opérateur | Non implémentée ; interface cible dans `user-journeys.md` §4 | Phase 2 |
| Notation et signalement | Affichage uniquement en V1, sans persistance | Phase 2 |
| Plusieurs pays / règles | Un seul pays simulé (Dakar) | Phase 4 |
| Conformité réglementaire | Hors périmètre ; écrit explicitement dans `docs/PROJECT.md` §5 | — |

---

## 6. Résultat de la relecture

**La conception de la phase 0.3 est complète et cohérente.**

- Toutes les exigences non fonctionnelles V1 sont tracées jusqu'à un composant.
- Chaque cas d'utilisation a une séquence nominale.
- Chaque entité a un service propriétaire sans FK inter-schémas.
- Les diagrammes de classes, d'états, le modèle de données et les événements sont
  mutuellement cohérents.
- Les points ouverts sont explicitement documentés et assignés à une phase.

La phase 0.4 (squelette technique) peut démarrer.
