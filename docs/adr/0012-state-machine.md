# ADR 0012 — Machine d'états commande et livraison

## Contexte

Les entités `Order` et `Delivery` suivent des cycles de vie stricts. Une transition invalide (ex. passer d'`PENDING` à `DELIVERED` directement) ne doit jamais être possible, y compris sous charge ou en cas de rejeu.

## Options

| Critère | Spring State Machine | Enum + transitions explicites |
|---------|---------------------|-------------------------------|
| Déclaration des transitions | Déclarative (config builder) | Impérative (méthode `transitionTo`) |
| Validation automatique | Oui (event refusé si invalide) | À coder manuellement |
| Persistance de l'état | Via `StateMachinePersist` | Dans le champ `status` de l'entité |
| Visibilité dans la démo | Justifie une brique d'infra | Invisible |
| Dépendance | `spring-statemachine-starter` | Aucune |

## Décision

**Spring State Machine 4.0.0** — utilisé comme validateur de transitions dans order-svc. Le pattern adopté :

1. Charger l'entité depuis la base (état courant).
2. Créer une machine via `StateMachineFactory`, la démarrer dans l'état courant.
3. Envoyer l'événement ; si la machine rejette l'événement (état inchangé), lever une `InvalidTransitionException`.
4. Persister le nouvel état dans la base.

La machine d'états ne stocke pas l'état lui-même — la base de données fait foi. La machine sert uniquement à valider et à centraliser les règles de transition.

## Conséquences

- `order-svc` dépend de `spring-statemachine-starter:4.0.0`.
- Les transitions sont testables unitairement sans base de données.
- Si Spring State Machine introduit une incompatibilité avec une future version de Spring Boot, le remplacement par un enum explicite ne change pas le comportement observable (la machine est encapsulée dans `OrderStateMachineService`).
