# Yóbbu : description du projet

Description produit, objectif et périmètre. Les règles de travail sont dans `CLAUDE.md` ; les décisions techniques dans `docs/adr/`.

## 1. Le projet en deux phrases

**Yóbbu** (du wolof *yóbbu*, « porter, emporter ») est une plateforme open source de **dispatch de livraison en temps réel** : elle décide quel livreur prend quelle course, puis permet à tous de suivre la livraison en direct.

Elle vise les marchés africains où la livraison de repas et de colis explose (Dakar, Abidjan, Lagos, Douala, Lomé), avec des réseaux instables et des règles qui changent d'un pays à l'autre.

## 2. L'objectif réel : un projet vitrine de niveau senior

Ce projet n'est pas un produit à vendre. C'est un **projet de portfolio** destiné à démontrer, à un recruteur, des compétences d'ingénieur **senior en full stack ET en DevOps**, à parts égales de sérieux (avec un léger accent sur le DevOps).

Conséquences pour toi :

- **Le jugement compte plus que la quantité de technologies.** Un projet avec 6 technologies bien justifiées vaut mieux que 15 empilées.
- **Chaque brique d'infrastructure doit être nécessaire à cause du domaine.** Test à appliquer : « si je retire cette brique, le produit devient-il moins fiable, moins sûr ou non conforme ? » Si la réponse est non, ne l'ajoute pas.
- **Les décisions doivent être documentées et défendables** (voir la section ADR).
- **Le projet doit marcher pour de vrai** : démo en ligne, démarrage en une commande, CI verte.
- **Aucun côté ne doit être sacrifié.** Ne réduis ni le full stack à une façade, ni le DevOps à de la décoration.

## 3. Ce que fait Yóbbu

### Acteurs

| Acteur | Besoin principal | Interface |
| --- | --- | --- |
| **Client** | Commander, suivre sa livraison sur une carte, être notifié | Application web mobile (PWA) |
| **Livreur** | Recevoir des courses adaptées, accepter ou refuser vite, mettre à jour l'état, travailler même avec un réseau instable | Application web mobile (PWA) |
| **Marchand** | Déclarer une commande prête, voir l'arrivée du livreur | Espace marchand |
| **Opérateur** | Voir toutes les livraisons, intervenir sur un cas bloqué, suivre les indicateurs | Console d'administration |

### Parcours type

1. Le client passe une commande, le marchand la confirme puis la déclare prête
2. Yóbbu repère les livreurs disponibles autour du marchand
3. Il choisit le meilleur selon la distance, la charge du livreur et l'urgence
4. Il propose la course au livreur, qui accepte ou refuse dans un délai court (sinon, passage au suivant)
5. Le livreur récupère la commande, le client suit sa progression sur une carte avec une heure d'arrivée estimée
6. Le livreur confirme la remise, le client est notifié et peut noter
7. Si quelque chose bloque (aucun livreur, livreur injoignable, client absent), l'opérateur est alerté et peut intervenir

### Fonctionnalités par acteur

**Client :** commander et voir l'état, suivre en direct sur carte, notifications à chaque étape, annulation avant récupération, notation et signalement, historique.

**Livreur :** disponible/indisponible, propositions de course avec délai court, itinéraire et infos utiles, mise à jour d'état en un geste, **fonctionnement hors-ligne avec synchronisation au retour du réseau**, courses et gains du jour.

**Marchand :** réception et confirmation des commandes, « commande prête », arrivée prochaine du livreur, historique.

**Opérateur :** carte et liste des livraisons en cours, réaffectation ou choix forcé d'un livreur, alertes sur les situations bloquées, indicateurs (délai moyen, taux d'acceptation, retards), configuration des règles par pays.

**Communes :** comptes avec droits par rôle, notifications (SMS, application, courriel), interface multilingue, historique complet de chaque livraison.

## 4. Contraintes du contexte africain

Ces contraintes justifient les choix techniques. Garde-les en tête à chaque décision.

- **Plusieurs pays :** zones, horaires, devises (FCFA, naira, dalasi) et règles propres. Le même parcours doit s'adapter sans être réécrit.
- **Réseau instable :** un livreur peut perdre la connexion en pleine course. Aucune livraison ne doit être perdue.
- **Adresses imprécises :** accepter des repères (« devant la pharmacie ») et un point sur la carte en plus d'une adresse écrite.
- **Plusieurs langues :** français, anglais, langues locales à terme.
- **Appareils modestes et données limitées :** les interfaces doivent rester légères.
- **Données personnelles :** certains pays imposent un hébergement local et la traçabilité des opérations. À prendre en compte dès la conception.

## 5. Périmètre et phases

La première version reste **étroite** : une livraison complète, de la commande à la remise, dans **un seul pays simulé**.

| Étape | Contenu |
| --- | --- |
| **Première version** | Commande (repas ou colis), marchand simulé, paiement mobile money simulé, choix automatique du livreur, proposition acceptée ou refusée, suivi en direct sur carte (Dakar), notifications de base, console opérateur minimale, livreurs simulés pour la démonstration |
| **Ensuite** | Espace marchand, réaffectation par l'opérateur, alertes sur les situations bloquées, mode hors-ligne du livreur, notation et signalement |
| **Plus tard** | Plusieurs pays avec règles propres, plusieurs langues, indicateurs avancés, gains et historique du livreur |

**Hors périmètre :**

- Pas d'argent réel ni de vraies données personnelles : tout est simulé ou fictif
- Pas de vrais livreurs : un **simulateur** de livreurs (qui bougent sur une carte) sert à la démonstration et aux tests de charge
- Pas de paiement réel, et aucune prétention de conformité réglementaire (à écrire clairement dans le README)
- Pas de migration monolithe vers microservices (pas de « Strangler Pattern »)

## 6. Le niveau de qualité attendu

Le projet doit exhiber les compétences ci-dessous. C'est la grille d'après laquelle le travail sera jugé.

**Architecture :** découpage clair et justifié (monolithe modulaire ou microservices, au choix mais argumenté), modélisation de données soignée avec migrations versionnées, diagrammes dans le dépôt.

**Backend :** API versionnée et documentée (OpenAPI), authentification et autorisation sérieuses (OAuth2/OIDC, droits par rôle), traitement asynchrone avec idempotence et reprise après panne, gestion des erreurs, validation, limitation de débit.

**Frontend :** TypeScript strict, gestion d'état maîtrisée, performance, accessibilité, mode hors-ligne pour le livreur, tests composants et de bout en bout.

**Données :** SQL maîtrisé (index, transactions, requêtes géographiques), un deuxième type de stockage uniquement si justifié, stratégie de sauvegarde et de migration sans interruption.

**DevOps :** conteneurs propres (multi-étapes, images légères, utilisateur non root), CI/CD complet (lint, tests, build, scans de sécurité, déploiement), infrastructure décrite comme du code, orchestration et déploiements sans interruption, plusieurs environnements.

**Observabilité :** logs structurés, métriques, traces distribuées d'une commande de bout en bout, alertes et tableaux de bord, notions de SLO.

**Sécurité :** scan des dépendances et des images, analyse statique, gestion propre des secrets (jamais dans le code ni dans les images, rotation possible sans reconstruire), chiffrement des communications, traitement concret des risques OWASP.

**Tests :** unitaires, intégration, contrats d'API, bout en bout, tests de charge avec résultats chiffrés (par exemple débit et latence sous charge).

**Pratiques open source :** README excellent, CONTRIBUTING, licence, modèles d'issues et de PR, Conventional Commits, versions sémantiques, changelog automatisé.

## 7. Les difficultés techniques qui font l'intérêt du projet

Ce sont les endroits où il faut réfléchir sérieusement, pas improviser :

- **Choix du livreur :** un vrai problème d'algorithme (distance, charge, priorité, refus, délais), pas un simple tri
- **Temps réel :** de nombreuses positions arrivent chaque seconde, à traiter et diffuser vers les cartes
- **Fiabilité :** une commande ne doit jamais être perdue, même si un livreur perd le réseau, refuse, ou si un service tombe (nouvelles tentatives, idempotence, files de messages)
- **Pics de charge :** midi et soir, la charge est multipliée (montée en charge automatique, tests de charge)
- **Hors-ligne :** l'application livreur doit continuer à fonctionner et se resynchroniser sans perdre d'information
- **Règles par pays :** le comportement varie selon le marché, sans réécrire le parcours

## 8. Décisions techniques : aucune n'est encore prise

**Important : la pile technique n'est pas choisie.** Ne suppose rien et n'impose pas de technologie sans discussion.

Pistes évoquées pendant la réflexion, à titre indicatif et non engageant : PostgreSQL avec PostGIS pour les données et le géographique, un bus de messages (Kafka, NATS ou RabbitMQ) pour le temps réel, Redis pour le cache, WebSocket ou SSE pour la diffusion, Kubernetes avec Helm ou Kustomize, GitOps, Keycloak pour l'identité, Prometheus, Grafana et OpenTelemetry pour l'observabilité, Vault ou équivalent pour les secrets, GitHub Actions pour la CI/CD.

**Pour chaque décision structurante :** voir `CLAUDE.md`.


## 10. Décisions de cadrage (phase 0.1)

| Sujet | Décision |
| --- | --- |
| Marchand en V1 | **Simulé** : il confirme et déclare la commande « prête » sans interface dédiée. L'espace marchand réel arrive en phase 2. |
| Type de livraison | **Repas et colis**, via un seul modèle de commande avec un champ `type`. Pas de logique propre à chaque type en V1 ; pour un colis, l'expéditeur joue le rôle du marchand (simulé). Si le colis exige un parcours distinct (preuve de remise, expéditeur réel), il passe en phase 2. |
| Ville de démonstration | **Dakar** (carte et livreurs simulés). |
| Paiement | **Mobile money simulé**, en petit module isolé : faux fournisseur, un seul rappel idempotent, statut « payé ». Aucun argent réel. Si l'itération 1 déborde, repli sur un paiement à la livraison simulé. |
| Nom Yóbbu | Validé par le propriétaire (orthographe et sens wolof). Dépôt : `sarrmouhamed29/yobbu`. Le compte `yobbu` et l'organisation `Yobbu-App` existent déjà sur GitHub, et quelques dépôts homonymes (`yobbul-*`, etc.) sans traction. Domaines et marques non vérifiés. |

Question technique restante : monolithe modulaire ou microservices, traitée dans l'ADR 0001 (voir `PLAN.md`).

