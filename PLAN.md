# Yóbbu : plan de mise en place

Plan global, sans dates. Il sert de backlog de haut niveau et de tableau de suivi. Le détail du produit est dans `docs/PROJECT.md`, les règles de travail dans `CLAUDE.md`, les décisions dans `docs/adr/`.

## 1. Méthode de travail

Scrum allégé, sans calendrier : on mesure l'avancement en **itérations**, pas en jours.

- **Itération** : un objectif unique et démontrable (« le livreur reçoit une proposition et l'accepte »), de la taille qu'il faut. Elle se termine quand l'objectif est atteint et vérifié, pas à une date.
- **Backlog** : les cases de ce fichier. Une case = une tâche livrable (idéalement une PR). Une case trop grosse se découpe avant d'être prise.
- **Limite de travail en cours** : une itération active à la fois, trois cases ouvertes au maximum.
- **Revue d'itération** : on démontre le résultat (commande lancée, capture, test vert) puis on coche.
- **Rétro** : 3 lignes dans la section « Journal » en fin d'itération (ce qui a marché, ce qui a frotté, ce qu'on change).
- **Décisions structurantes** : toute case marquée `ADR` produit un `docs/adr/NNNN-titre.md` (contexte, options, décision, conséquences) **avant** l'implémentation, avec validation du propriétaire.

### Definition of Done (toute case)

- [ ] Code, tests et doc ajoutés dans la même PR
- [ ] CI verte
- [ ] Aucun secret dans le dépôt
- [ ] `docker compose up` (ou `make dev`) fonctionne toujours
- [ ] Ce qui est simulé ou non testé est écrit noir sur blanc
- [ ] ADR rédigé si la case était marquée `ADR`

### Légende

`ADR` décision à documenter · `DEV` full stack · `OPS` DevOps · `SEC` sécurité · `OBS` observabilité · `TEST` tests

---

## 2. Vue d'ensemble

| Phase | Objectif | Résultat démontrable |
| --- | --- | --- |
| **0. Fondations** | Cadrer, décider, poser le squelette | Dépôt propre, CI verte, un flux « hello » de bout en bout |
| **1. Première version** | Une livraison complète dans un pays simulé | Commande → dispatch → livraison, suivie sur carte |
| **2. Fiabilité et opérations** | Rendre le système robuste et exploitable | Pannes tolérées, alertes, mode hors-ligne, espace marchand |
| **3. Production** | Déployer pour de vrai et mesurer | Démo en ligne, déploiement sans interruption, tests de charge chiffrés |
| **4. Extension** | Multi-pays, multilingue, indicateurs | Règles par pays sans réécrire le parcours |

On ne démarre pas une phase avant d'avoir terminé et vérifié la précédente. Les trois pistes transverses (sécurité, observabilité, tests) avancent **avec** chaque phase, pas à la fin.

---

## 3. Phase 0 : Fondations

Objectif : se mettre d'accord sur les points bloquants et obtenir un squelette qui tourne.

### 0.1 Cadrage (questions ouvertes de `docs/PROJECT.md`)

- [x] Fixer le périmètre de la V1 : marchand **simulé**
- [x] Choisir le type de livraison : **repas et colis** (un modèle de commande, champ `type`)
- [x] Choisir la ville de démonstration : **Dakar**
- [x] Trancher le paiement : **mobile money simulé**, module isolé (repli sur paiement à la livraison simulé si l'itération déborde)
- [x] Vérifier le nom Yóbbu : validé par le propriétaire ; aucune collision bloquante sur GitHub

Détail des décisions : `docs/PROJECT.md`, section 10.

### 0.2 Décisions d'architecture

- [x] `ADR` Monolithe modulaire ou microservices : **microservices** (ADR 0001)
- [ ] `ADR` Langage et framework backend
- [ ] `ADR` Base de données et gestion géographique
- [ ] `ADR` Messagerie et traitement asynchrone (justifier le besoin avant de choisir)
- [ ] `ADR` Temps réel vers les cartes (WebSocket ou SSE)
- [ ] `ADR` Frontend (framework, gestion d'état, PWA)
- [ ] `ADR` Authentification et identité (OIDC, rôles)

### 0.3 Conception

Objectif : une conception complète, relue et versionnée **avant** d'écrire le code métier. Tout vit dans `docs/design/` et `docs/architecture/`. Les diagrammes sont écrits « en code » (texte versionné, relisible en PR) ; le format est tranché par l'ADR 0009.

**Cadrage fonctionnel**

- [ ] `ADR` Format des diagrammes (Mermaid, PlantUML ou autre) et emplacement
- [ ] `DEV` Glossaire du domaine (langage commun : course, proposition, dispatch, point de retrait, etc.)
- [ ] `DEV` Cas d'utilisation par acteur (client, livreur, marchand, opérateur) + diagramme UML de cas d'utilisation
- [ ] `DEV` Exigences non fonctionnelles chiffrées : objectifs de latence du dispatch, positions par seconde, disponibilité visée (premiers SLO), volumétrie estimée
- [ ] `DEV` Contraintes, hypothèses et registre des risques (réseau instable, données personnelles, pics de charge)
- [ ] `DEV` Découpage en domaines et modules (contextes délimités) + carte des contextes : qui possède quelle donnée, qui parle à qui

**Modélisation UML**

- [ ] `DEV` Diagramme de classes du domaine (entités, objets valeur, agrégats, invariants)
- [ ] `DEV` Diagrammes d'états : commande, livraison, proposition de course, disponibilité du livreur (transitions autorisées et événements déclencheurs)
- [ ] `DEV` Diagrammes de séquence : flux nominal, refus et expiration d'une proposition, perte de réseau du livreur puis resynchronisation, paiement simulé et rappel idempotent, annulation
- [ ] `DEV` Diagramme d'activité de l'algorithme de dispatch (candidats, score, repli)
- [ ] `DEV` Diagramme de composants et diagramme de déploiement (nœuds, conteneurs, réseaux)

**Modèle de données**

- [ ] `DEV` Modèle conceptuel puis modèle logique (diagramme entité-association) avec règles d'intégrité
- [ ] `DEV` Modèle physique : types, clés, contraintes, index (dont index géographiques), partitionnement éventuel des positions
- [ ] `DEV` Stratégie de migration, de volumétrie et de rétention (surtout les positions, volumineuses)
- [ ] `DEV` Traçabilité : journal d'audit de chaque livraison, historisation des changements d'état
- [ ] `SEC` Classification des données personnelles (ce qu'on stocke, pourquoi, combien de temps, qui y accède)

**Architecture (vues complémentaires)**

- [ ] `DEV` Modèle C4 : niveau 1 (contexte), niveau 2 (conteneurs), niveau 3 (composants des parties critiques)
- [ ] `DEV` Vue fonctionnelle : capacités par acteur et leurs dépendances
- [ ] `OPS` Vue technique et d'infrastructure : environnements, réseau, stockage, flux entrants et sortants
- [ ] `DEV` Vue des flux de données et d'événements : catalogue des événements (nom, schéma, producteur, consommateurs, version)
- [ ] `SEC` Frontières de confiance et modèle de menaces (STRIDE) sur les flux sensibles
- [ ] `DEV` Stratégie de cohérence : ce qui est transactionnel, ce qui est asynchrone, où l'idempotence est requise

**Contrats et conventions**

- [ ] `DEV` Conception de l'API d'abord (OpenAPI rédigé avant le code) : ressources, erreurs, pagination, versionnement, idempotence
- [ ] `DEV` Conventions : nommage, structure des modules, gestion des erreurs, journalisation
- [ ] `OBS` Plan d'observabilité : logs, métriques, traces, identifiant de corrélation d'une commande, premiers SLO
- [ ] `TEST` Stratégie de test (pyramide, données de test, contrats, charge) et plan de résilience (modes de panne et comportement attendu de chaque composant)

**Interfaces**

- [ ] `DEV` Parcours utilisateurs et maquettes basse fidélité des quatre interfaces (client, livreur, marchand, opérateur)
- [ ] `DEV` Conception du mode hors-ligne du livreur : données gardées en local, file des actions, règles de résolution de conflits

**Revue de conception (critère de sortie)**

- [ ] Relecture croisée : chaque exigence est couverte par un cas d'utilisation, chaque cas par une séquence, chaque donnée a un propriétaire
- [ ] Cohérence vérifiée entre diagramme de classes, états, modèle de données et événements
- [ ] Documents indexés dans `docs/README.md`, ADR liés aux diagrammes qu'ils justifient

### 0.4 Dépôt et squelette

- [ ] `DEV` Structure du dépôt (dossiers, CONTRIBUTING, modèles d'issues et de PR ; README et licence MIT déjà en place)
- [ ] `DEV` Squelette backend + frontend + base, un flux trivial de bout en bout
- [ ] `OPS` Démarrage en une commande (`docker compose up` ou `make dev`)
- [ ] `OPS` CI minimale : lint, tests, build
- [ ] `OPS` Conteneurs propres (multi-étapes, non root, images légères)
- [ ] `SEC` Gestion des secrets dès le départ (fichiers d'exemple, aucune valeur dans le dépôt)
- [ ] `DEV` Conventional Commits appliqués (vérification en CI)
- [ ] `DEV` Remplir les sections « Commandes » et « Structure » de `CLAUDE.md`

**Sortie de phase :** la conception est relue et versionnée ; un inconnu clone le dépôt, lance une commande, voit l'application répondre ; la CI est verte.

---

## 4. Phase 1 : Première version

Objectif : **une livraison complète, de la commande à la remise, dans un pays simulé.**

### 1.1 Modèle et API

- [ ] `DEV` Modèle de données (commandes avec champ `type` repas/colis, livreurs, courses, positions) avec migrations versionnées
- [ ] `DEV` Paiement mobile money simulé : faux fournisseur, rappel idempotent, statut « payé » (module isolé)
- [ ] `DEV` Machine d'états de la commande et de la livraison (transitions valides uniquement)
- [ ] `DEV` API versionnée + spécification OpenAPI
- [ ] `DEV` Validation, gestion d'erreurs, limitation de débit

### 1.2 Identité

- [ ] `SEC` Comptes et authentification
- [ ] `SEC` Droits par rôle (client, livreur, opérateur ; marchand simulé)

### 1.3 Dispatch

- [ ] `ADR` Algorithme de choix du livreur (distance, charge, urgence, refus, délais)
- [ ] `DEV` Recherche des livreurs disponibles autour du marchand (requête géographique indexée)
- [ ] `DEV` Proposition au livreur avec délai court, puis passage au suivant
- [ ] `TEST` Tests unitaires de l'algorithme (refus, expiration, aucun livreur)
- [ ] `DEV` Idempotence des opérations de dispatch (rejeu sans doublon)

### 1.4 Temps réel

- [ ] `DEV` Réception des positions des livreurs
- [ ] `DEV` Diffusion vers les cartes
- [ ] `DEV` Heure d'arrivée estimée

### 1.5 Simulateur

- [ ] `DEV` Simulateur de livreurs qui bougent sur la carte (sert à la démo et plus tard à la charge)

### 1.6 Interfaces

- [ ] `DEV` Client : passer une commande, suivre sur carte, voir l'état
- [ ] `DEV` Livreur : disponible/indisponible, accepter/refuser, mettre à jour l'état
- [ ] `DEV` Opérateur : liste et carte des livraisons en cours (minimale)
- [ ] `DEV` Notifications de base
- [ ] `DEV` Accessibilité et légèreté (mobile, données limitées)

### 1.7 Qualité de la phase

- [ ] `TEST` Test de bout en bout : commande → livraison
- [ ] `TEST` Tests d'intégration de l'API
- [ ] `OPS` CI complète : lint, tests, build, scan des dépendances, analyse statique
- [ ] `OBS` Logs structurés
- [ ] `DEV` README avec démo, limites (tout est simulé) et mention « pas de conformité réglementaire »

**Sortie de phase :** démo locale d'une livraison complète avec livreurs simulés.

---

## 5. Phase 2 : Fiabilité et opérations

Objectif : une commande n'est jamais perdue ; l'opérateur peut agir.

- [ ] `DEV` Reprise après panne : nouvelles tentatives, files de messages, jeux de test de pannes
- [ ] `DEV` Mode hors-ligne du livreur avec synchronisation sans perte
- [ ] `DEV` Réaffectation et choix forcé d'un livreur par l'opérateur
- [ ] `DEV` Alertes sur les situations bloquées (aucun livreur, injoignable, client absent)
- [ ] `DEV` Espace marchand : confirmation, « commande prête », arrivée du livreur
- [ ] `DEV` Notation et signalement
- [ ] `DEV` Historique complet et traçable de chaque livraison
- [ ] `OBS` Métriques, traces distribuées d'une commande de bout en bout
- [ ] `OBS` Tableaux de bord et alertes, définition de SLO
- [ ] `TEST` Tests de contrat d'API
- [ ] `TEST` Tests composants et de bout en bout du frontend
- [ ] `SEC` Traitement concret des risques OWASP (documenté)
- [ ] `SEC` Scan des images de conteneurs

**Sortie de phase :** on coupe un composant en pleine course et aucune livraison n'est perdue ; un blocage déclenche une alerte visible.

---

## 6. Phase 3 : Production

Objectif : le projet tourne vraiment, en ligne.

- [ ] `ADR` Cible de déploiement et orchestration
- [ ] `OPS` Infrastructure décrite comme du code
- [ ] `OPS` Plusieurs environnements (dev, staging, prod)
- [ ] `OPS` CD : déploiement automatisé et sans interruption
- [ ] `OPS` Montée en charge automatique
- [ ] `SEC` Secrets gérés hors des images, rotation sans reconstruction
- [ ] `SEC` Chiffrement des communications
- [ ] `OPS` Sauvegarde, restauration testée, migrations sans interruption
- [ ] `TEST` Tests de charge (pic de midi) avec **résultats chiffrés** : débit, latence
- [ ] `DEV` Démo en ligne publique
- [ ] `DEV` Versions sémantiques et changelog automatisé

**Sortie de phase :** une URL de démo, une CI/CD qui déploie, des chiffres de charge dans le README.

---

## 7. Phase 4 : Extension

- [ ] `ADR` Modèle de règles par pays (configuration plutôt que code dupliqué)
- [ ] `DEV` Plusieurs pays : zones, horaires, devises, règles
- [ ] `DEV` Interface multilingue (français, anglais, puis langues locales)
- [ ] `DEV` Indicateurs avancés pour l'opérateur (délai moyen, taux d'acceptation, retards)
- [ ] `DEV` Gains et historique du livreur
- [ ] `SEC` Hébergement local des données par pays (étude, sans prétention de conformité)

---

## 8. Registre des décisions (ADR)

| N° | Sujet | Statut |
| --- | --- | --- |
| 0001 | Monolithe modulaire ou microservices | accepté (microservices) |
| 0002 | Backend | à faire |
| 0003 | Base de données et géographique | à faire |
| 0004 | Messagerie et asynchrone | à faire |
| 0005 | Temps réel | à faire |
| 0006 | Frontend | à faire |
| 0007 | Authentification | à faire |
| 0008 | Algorithme de dispatch | à faire |
| 0009 | Format des diagrammes | à faire |

Statuts : à faire · proposé · accepté · remplacé.

---

## 9. Journal des itérations

À remplir en fin d'itération.

| Itération | Objectif | Résultat | Rétro (marché / frotté / on change) |
| --- | --- | --- | --- |
| 1 | Cadrage + ADR 0001 | 0.1 terminé (5 décisions de cadrage) ; ADR 0001 accepté (microservices) | |
