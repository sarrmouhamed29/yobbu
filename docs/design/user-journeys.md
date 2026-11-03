# User journeys and low-fidelity wireframes

Parcours utilisateurs et maquettes textuelles basse fidélité pour les quatre interfaces
de Yóbbu V1. Les maquettes décrivent la structure des écrans, pas leur apparence finale.

**Status:** validated by the owner on 2026-11-03.

Sources: [use-cases.md](use-cases.md), [api-design.md](api-design.md),
[state-diagrams.md](state-diagrams.md), [offline-design.md](offline-design.md).

---

## 1. Application client (PWA mobile)

### Parcours nominal : passer et suivre une commande

```
[Accueil]
  ┌─────────────────────────────────────────────────────┐
  │ Yóbbu                                    [Profil ▾] │
  │                                                     │
  │  📍 Livrer à : [Votre adresse ou point sur carte]   │
  │                                                     │
  │  [Commander un repas]  [Envoyer un colis]           │
  │                                                     │
  │  ─── Commandes récentes ───────────────────────────  │
  │  Thiéboudienne · Livré · 12:34            [Détail]  │
  └─────────────────────────────────────────────────────┘
         ↓ [Commander un repas]

[Saisie de commande]
  ┌─────────────────────────────────────────────────────┐
  │ ← Nouvelle commande                                 │
  │                                                     │
  │  Description                                        │
  │  ┌─────────────────────────────────────────────┐   │
  │  │ Ex : Thiéboudienne + Jus de bissap           │   │
  │  └─────────────────────────────────────────────┘   │
  │                                                     │
  │  Retrait (marchand simulé)                          │
  │  📍 Marché Sandaga, Dakar                          │
  │                                                     │
  │  Livraison                                          │
  │  📍 [Adresse ou repère]  [Choisir sur carte]       │
  │                                                     │
  │  Paiement : Mobile Money (simulé)                   │
  │                                                     │
  │  Montant estimé : 2 500 FCFA                        │
  │                          [Confirmer la commande →]  │
  └─────────────────────────────────────────────────────┘
         ↓ [Confirmer]  → POST /api/v1/orders

[En attente d'un livreur]
  ┌─────────────────────────────────────────────────────┐
  │ ← Commande #A1B2                     RECHERCHE…    │
  │                                                     │
  │  ╔═══════════════════════════════════════════════╗  │
  │  ║              [Carte Dakar]                    ║  │
  │  ║    🏪                                         ║  │
  │  ║         ⟳ Recherche d'un livreur…            ║  │
  │  ╚═══════════════════════════════════════════════╝  │
  │                                                     │
  │  Thiéboudienne + Jus de bissap                      │
  │  Retrait : Marché Sandaga                           │
  │  Livraison : Plateau, près de la pharmacie          │
  │                                                     │
  │  [Annuler la commande]                              │
  └─────────────────────────────────────────────────────┘
         ↓  SSE reçoit DeliveryAssigned

[Livreur en route vers le marchand]
  ┌─────────────────────────────────────────────────────┐
  │ ← Commande #A1B2               EN COURS DE RETRAIT  │
  │                                                     │
  │  ╔═══════════════════════════════════════════════╗  │
  │  ║    🛵 → 🏪                                    ║  │
  │  ║         [Carte avec position en temps réel]   ║  │
  │  ╚═══════════════════════════════════════════════╝  │
  │                                                     │
  │  Livreur : Moussa D.                                │
  │  Arrivée estimée : 12 min                           │
  │                                                     │
  │  Étapes :                                           │
  │  ✓ Commande confirmée                               │
  │  ✓ Livreur trouvé                                   │
  │  ◉ Livreur en route vers le marchand               │
  │  ○ Commande récupérée                               │
  │  ○ En route vers vous                               │
  │  ○ Livré                                            │
  └─────────────────────────────────────────────────────┘

[Livraison terminée]
  ┌─────────────────────────────────────────────────────┐
  │ ✓ Commande livrée                                   │
  │                                                     │
  │  Votre commande a été livrée à 13:07.               │
  │                                                     │
  │  Notez cette livraison :                            │
  │  ★ ★ ★ ★ ☆  (hors périmètre V1 — affiché seulement) │
  │                                                     │
  │  [Voir le détail]    [Nouvelle commande]            │
  └─────────────────────────────────────────────────────┘
```

### Parcours : annulation avant retrait

1. Client appuie sur « Annuler la commande » (états `PENDING` ou `CONFIRMED`)
2. Confirmation : « Annuler cette commande ? Oui / Non »
3. `POST /api/v1/orders/{id}/cancel` → ordre passe en `CANCELLED`
4. Écran de confirmation d'annulation + retour à l'accueil

---

## 2. Application livreur (PWA mobile)

### Parcours nominal : accepter et effectuer une course

```
[Accueil livreur]
  ┌─────────────────────────────────────────────────────┐
  │ Yóbbu Livreur              Moussa D.  [Profil ▾]   │
  │                                                     │
  │  Statut : ● DISPONIBLE          [Basculer →]        │
  │                                                     │
  │  Aujourd'hui : 3 courses · 7 500 FCFA               │
  │                                                     │
  │  ─── Propositions ──────────────────────────────── │
  │  (aucune proposition en cours)                      │
  └─────────────────────────────────────────────────────┘
         ↓  Notification : nouvelle proposition (push / SSE)

[Proposition de course]
  ┌─────────────────────────────────────────────────────┐
  │ 🔔 Nouvelle course                     ⏱ 0:25      │
  │                                                     │
  │  Retrait  : Marché Sandaga             📍 800 m    │
  │  Livraison: Plateau, près pharmacie    📍 2,3 km   │
  │                                                     │
  │  ╔═══════════════════════════════════════════════╗  │
  │  ║  [Mini-carte : retrait → livraison]           ║  │
  │  ╚═══════════════════════════════════════════════╝  │
  │                                                     │
  │          [Refuser]         [Accepter ✓]             │
  └─────────────────────────────────────────────────────┘
  (Le compteur expire à 0 → offre EXPIRED, passage au suivant)
         ↓ [Accepter]  → POST /api/v1/offers/{id}/accept

[En route vers le marchand]
  ┌─────────────────────────────────────────────────────┐
  │ ← Course en cours                  EN ROUTE RETRAIT │
  │                                                     │
  │  📍 Retrait : Marché Sandaga                        │
  │     Thiéboudienne + Jus de bissap                   │
  │                                                     │
  │  ╔═══════════════════════════════════════════════╗  │
  │  ║      [Carte navigation jusqu'au marchand]     ║  │
  │  ╚═══════════════════════════════════════════════╝  │
  │                                                     │
  │              [J'ai récupéré la commande →]          │
  └─────────────────────────────────────────────────────┘
         ↓  → POST /api/v1/orders/{id}/pickup

[En route vers le client]
  ┌─────────────────────────────────────────────────────┐
  │ ← Course en cours               EN ROUTE LIVRAISON  │
  │                                                     │
  │  📍 Livraison : Plateau, près de la pharmacie       │
  │     Client : +221 77 000 00 00                      │
  │                                                     │
  │  ╔═══════════════════════════════════════════════╗  │
  │  ║      [Carte navigation jusqu'au client]       ║  │
  │  ╚═══════════════════════════════════════════════╝  │
  │                                                     │
  │                    [Commande livrée ✓]              │
  └─────────────────────────────────────────────────────┘
         ↓  → POST /api/v1/orders/{id}/deliver
```

### Parcours : basculement disponible / indisponible

- Bouton unique « Basculer » sur l'accueil
- `PUT /api/v1/couriers/{id}/availability` → `{ "availability": "UNAVAILABLE" }`
- Statut mis à jour en temps réel ; le livreur ne reçoit plus de propositions

### Écran de resynchronisation hors-ligne

```
[Reconnexion détectée]
  ┌─────────────────────────────────────────────────────┐
  │ 🔄 Synchronisation en cours…                        │
  │                                                     │
  │  12 positions en attente → envoi                    │
  │  ▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░ 60 %                       │
  │                                                     │
  │  (Ne pas fermer l'application)                      │
  └─────────────────────────────────────────────────────┘
```

Détail du fonctionnement hors-ligne : [offline-design.md](offline-design.md).

---

## 3. Espace marchand (simulé en V1)

En V1, le marchand est entièrement simulé par `MerchantSimulator`. L'espace marchand
réel est prévu en phase 2. Ce parcours décrit l'interface **cible** (phase 2) pour
guider la simulation en V1.

```
[Tableau de bord marchand]
  ┌─────────────────────────────────────────────────────┐
  │ Yóbbu Marchand — Sandaga Cuisine        [Profil ▾] │
  │                                                     │
  │  ─── Nouvelles commandes ──────────────────────── │
  │  #A1B2 · Thiéboudienne + Bissap · 2 500 FCFA        │
  │         [Confirmer]    [Refuser]                    │
  │                                                     │
  │  ─── En préparation ───────────────────────────── │
  │  #9F3A · Yassa poulet · 3 000 FCFA  12:45          │
  │         [Commande prête →]                          │
  │                                                     │
  │  ─── En livraison ─────────────────────────────── │
  │  #B7C1 · Livraison en cours · Livreur : 5 min      │
  └─────────────────────────────────────────────────────┘
```

En V1, `MerchantSimulator` confirme automatiquement les commandes et les déclare prêtes
après un délai configurable.

---

## 4. Console opérateur (PWA desktop / tablette)

```
[Vue principale — Carte et liste]
  ┌──────────────────────────────────────────────────────────────────┐
  │ Yóbbu Opérateur                     Dakar  11:52   [Alertes 2🔴] │
  ├───────────────────────────┬──────────────────────────────────────┤
  │  LIVRAISONS EN COURS (8)  │                                      │
  │                           │                                      │
  │  #A1B2 EN_COURS   Moussa  │    ╔══════════════════════════════╗  │
  │  12:03 · ETA 8 min        │    ║                              ║  │
  │  [Détail] [Réaffecter]    │    ║   [Carte Dakar temps réel]   ║  │
  │  ─────────────────────    │    ║   🛵 🛵 🛵 🛵               ║  │
  │  #9F3A EN_ATTENTE  ⚠️     │    ║       🔴 ⚠️                  ║  │
  │  Aucun livreur / 5 min    │    ╚══════════════════════════════╝  │
  │  [Forcer un livreur]      │                                      │
  │  ─────────────────────    │  Indicateurs                         │
  │  #B7C1 EN_COURS   Fatou   │  Délai moyen dispatch : 1,8 s        │
  │  12:17 · ETA 14 min       │  Taux d'acceptation  : 87 %          │
  │  [Détail]                 │  En cours / bloquées : 8 / 1         │
  └───────────────────────────┴──────────────────────────────────────┘

[Détail livraison — overlay]
  ┌─────────────────────────────────────────────────────┐
  │ Livraison #9F3A                  ⚠️ BLOQUÉE         │
  │                                                     │
  │  Commande : Thiéboudienne (Sandaga Cuisine)         │
  │  Client   : +221 77 XXX XXXX                        │
  │  Créée    : 11:47  ·  Aucun livreur depuis 5 min    │
  │                                                     │
  │  Livreurs disponibles à moins de 3 km :             │
  │  ┌────────────────────────────────────────────┐     │
  │  │ Moussa D.  · 850 m  · 1 course active      │     │
  │  │ Aminata S. · 1,2 km · disponible           │  ←  │
  │  └────────────────────────────────────────────┘     │
  │                                                     │
  │  [Annuler]      [Affecter Aminata S. →]             │
  └─────────────────────────────────────────────────────┘
```

L'affectation forcée par l'opérateur est hors périmètre V1 ; l'interface est décrite
pour guider la conception de la phase 2.
