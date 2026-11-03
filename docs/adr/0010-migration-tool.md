# ADR 0010 — Outil de migration de base de données

## Contexte

La phase 1 introduit des schémas relationnels réels dans quatre services (order, dispatch, tracking, payment). Les schémas doivent être versionnés, reproductibles et appliqués automatiquement au démarrage de chaque service.

## Options

| Critère | Flyway | Liquibase |
|---------|--------|-----------|
| Intégration Spring Boot | Native (`spring.flyway.*`) | Native (`spring.liquibase.*`) |
| Format des scripts | SQL pur (+ Java optionnel) | SQL, XML, YAML, JSON |
| Rollback | Manuel (script SQL) | Natif |
| Lisibilité des PR | Excellente (SQL brut) | Moyenne (XML/YAML verbeux) |
| Complexité de setup | Faible | Moyenne |

## Décision

**Flyway** — conventions de nommage `V{version}__{description}.sql`, un répertoire `src/main/resources/db/migration/` par service, validation automatique au démarrage via `spring.flyway.validate-on-migrate=true`.

## Conséquences

- Les migrations sont des fichiers SQL lisibles directement dans les PR.
- Le rollback est manuel : on écrit une nouvelle migration `V{n+1}__rollback_*.sql` plutôt que de modifier les existantes.
- Les scripts doivent être compatibles PostgreSQL 16 + PostGIS 3.4 (aucune compatibilité H2 requise en production).
- Les tests unitaires qui chargent le contexte Spring désactivent Flyway (`spring.flyway.enabled=false`) et utilisent H2 avec `ddl-auto=create-drop`.
