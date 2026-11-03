# ADR 0011 — Couche d'accès aux données

## Contexte

Chaque service a besoin de persister et requêter ses entités. Deux candidats principaux pour le stack Spring Boot 3 + PostgreSQL.

## Options

| Critère | Spring Data JPA (Hibernate 6) | jOOQ |
|---------|-------------------------------|------|
| Intégration Spring Boot | `spring-boot-starter-data-jpa` | Manuel (codegen) |
| Requêtes simples | Dérivées automatiquement | SQL typé manuel |
| Requêtes géospatiales | `@Query(nativeQuery)` + `hibernate-spatial` | SQL natif direct |
| Courbe d'apprentissage | Faible (standard Java EE) | Moyenne |
| Génération de code | Non | Oui (pré-build) |

## Décision

**Spring Data JPA avec Hibernate Spatial** — interfaces `JpaRepository`, requêtes SQL natives annotées `@Query(nativeQuery = true)` pour les requêtes géospatiales (dispatch PostGIS). Aucune génération de code requise.

## Conséquences

- Les services dispatch et tracking utilisent `hibernate-spatial` pour les colonnes `geometry(Point, 4326)` via `org.locationtech.jts.geom.Point`.
- Les requêtes de proximité (algorithme de dispatch) sont des SQL natifs PostgreSQL/PostGIS ; Hibernate ne les génère pas.
- Si les requêtes géospatiales deviennent complexes en phase 2, jOOQ peut être introduit dans dispatch-svc uniquement (décision par service).
