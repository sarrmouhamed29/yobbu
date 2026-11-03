# Yóbbu

Plateforme open source de dispatch de livraison en temps réel (choix du livreur, suivi en direct) pour les marchés africains. **Projet de portfolio de niveau senior, full stack et DevOps à parts égales.**

Description complète, acteurs, périmètre et phases : @docs/PROJECT.md

## Règles de travail

- **Communication :** réponds en français. Code, noms de variables, commits et documentation technique du dépôt en **anglais**.
- **Pile technique non choisie.** Ne suppose aucune technologie. Pour chaque décision structurante :
  1. expose le problème et les contraintes ;
  2. présente au moins deux options avec leurs compromis ;
  3. recommande-en une et explique pourquoi ;
  4. **attends la validation** avant d'implémenter ;
  5. consigne le résultat dans un ADR (`docs/adr/NNNN-titre.md` : contexte, options, décision, conséquences).
- **Justification des briques :** n'ajoute une brique d'infra que si, en la retirant, le produit devient moins fiable, moins sûr ou non conforme. Peu de technologies, bien justifiées.
- **Phases :** une phase à la fois, toujours démontrable. Ne commence pas la suivante sans avoir terminé et vérifié la précédente. Périmètre réduit qui marche > grand périmètre à moitié fini.
- **Démarrage en une commande** (`docker compose up` ou `make dev`) : ne jamais casser cette propriété.
- **Honnêteté :** si une chose est simulée, non testée ou non faite, le dire dans le code, la doc et le README. Ne jamais affirmer qu'une fonction marche sans l'avoir vérifiée.
- **Méthodologie documentée :** la méthodologie suivie (Scrum allégé, itérations, Definition of Done, ADR, phases) doit être décrite dans `README.md` (section « Methodology »). Tout changement de méthode met à jour `README.md` et `PLAN.md` dans la même PR.
- **Ambiguïté :** pose une seule question claire plutôt que de deviner. Pour une grosse tâche, propose d'abord un plan court.

## Qualité du dépôt

- Commits Conventional Commits, petits et cohérents ; changements notables via branche + PR bien décrite.
- Tests écrits avec le code, pas après. La CI reste verte.
- Aucun secret dans le dépôt, jamais, même pour « tester vite ».
- Hors périmètre : argent réel, vraies données personnelles, vrais livreurs (simulateur), prétention de conformité réglementaire.

## Commandes

```bash
make dev          # Démarre la pile complète (build + run)
make dev-infra    # Démarre l'infra seule (postgres, kafka, keycloak)
make dev-down     # Arrête tout
make build        # Construit toutes les images Docker sans démarrer
make test         # Lance les tests (Gradle + vitest)
make lint         # Lance les linters (checkstyle + eslint)
make logs         # Suit les logs de tous les services
make clean        # Supprime les volumes Docker (détruit les données)
make init         # Génère gradle-wrapper.jar et installe les dépendances pnpm
                  # (nécessite Gradle 8.10.2 et pnpm installés localement)
```

**Prérequis :** Docker Desktop (ou Docker Engine + Compose v2). Copier `.env.example` → `.env` et renseigner les mots de passe avant le premier `make dev`.

**Flux de validation bout en bout (skeleton) :**

```bash
curl http://localhost:8080/api/v1/orders/ping
# → {"service":"order-svc","status":"ok"}
```

## Structure du dépôt

```
.
├── apps/                    # Trois PWA React/TypeScript/Vite
│   ├── customer-app/        # Client (port 3001)
│   ├── courier-app/         # Livreur (port 3002)
│   └── operator-app/        # Opérateur (port 3003)
├── services/                # Build Gradle multi-projet, 6 services Spring Boot
│   ├── gateway/             # Spring Cloud Gateway (port 8080)
│   ├── order-svc/           # Commandes (port 8081)
│   ├── dispatch-svc/        # Dispatch / algorithme (port 8082)
│   ├── tracking-svc/        # Positions en temps réel (port 8083)
│   ├── payment-svc/         # Paiement simulé (port 8084)
│   └── notification-svc/    # Notifications (Kafka consumer, port 8085)
├── docker/
│   ├── postgres/init.sh     # Crée les utilisateurs et schémas par service
│   └── prometheus/          # Configuration Prometheus
├── docs/
│   ├── adr/                 # Décisions d'architecture (ADR 0001–0009)
│   ├── design/              # Conception complète (UML, OpenAPI, conventions…)
│   └── README.md            # Index de tous les documents de conception
├── .github/
│   ├── workflows/ci.yml     # CI : build + tests Java, build + typecheck frontend, build Docker
│   ├── workflows/commitlint.yml  # Vérification Conventional Commits sur chaque PR
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── ISSUE_TEMPLATE/      # bug_report.yml, feature_request.yml
├── docker-compose.yml       # Stack complète (infra + services + frontends)
├── Makefile                 # Raccourcis dev
├── .env.example             # Variables d'environnement à copier dans .env
└── CONTRIBUTING.md          # Guide du contributeur
```
