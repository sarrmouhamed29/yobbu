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

À remplir quand le squelette existera (dev, test, lint, build).

## Structure du dépôt

À remplir. Déjà en place : `docs/PROJECT.md` (description), `docs/adr/` (décisions).
