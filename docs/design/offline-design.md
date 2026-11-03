# Offline design — courier PWA

Conception du mode hors-ligne pour l'application livreur : données conservées localement,
file d'actions, règles de synchronisation et de résolution de conflits.

**Status:** validated by the owner on 2026-11-03.

Sources: [user-journeys.md](user-journeys.md), [api-design.md](api-design.md),
[tracking-api OpenAPI](openapi/tracking-api.yaml),
[non-functional-requirements.md](non-functional-requirements.md) (R-02, R-04).

---

## 1. Contrainte fondamentale

> Un livreur peut perdre la connexion en pleine course. **Aucune position et aucune
> mise à jour d'état ne doit être perdue.**

Les marchés africains cibles ont des réseaux instables : coupures fréquentes, passages
en 2G, zones de faible couverture. L'application doit fonctionner en mode dégradé et
se resynchroniser sans intervention de l'utilisateur.

---

## 2. Ce qui fonctionne hors-ligne

| Fonctionnalité | Hors-ligne | Note |
|---------------|-----------|------|
| Voir la course en cours (données mises en cache) | ✅ | Données de l'offre acceptée stockées localement |
| Voir l'itinéraire (si les tuiles sont en cache) | ✅ partiel | Dépend du cache des tuiles de carte |
| Mettre en file : position GPS | ✅ | Stockée dans IndexedDB |
| Mettre en file : « J'ai récupéré la commande » (pickup) | ✅ | Action mise en file |
| Mettre en file : « Commande livrée » (deliver) | ✅ | Action mise en file |
| Basculer disponible / indisponible | ❌ | Requiert la connexion |
| Accepter / refuser une nouvelle proposition | ❌ | Requiert la connexion |
| Voir les nouvelles propositions entrantes | ❌ | Push / SSE requiert la connexion |

---

## 3. Données conservées localement (IndexedDB)

Trois magasins IndexedDB dans l'application livreur :

### `active_delivery`

Objet unique mis à jour à chaque changement d'état de la course active.

```typescript
interface ActiveDelivery {
  deliveryId: string;          // UUID
  offerId: string;             // UUID
  status: DeliveryStatus;      // ASSIGNED | PICKED_UP | ...
  pickupPoint: { lat: number; lon: number };
  pickupAddress: string;
  dropoffPoint: { lat: number; lon: number };
  dropoffAddress: string;
  customerPhone: string;
  acceptedAt: string;          // ISO 8601
  cachedAt: string;            // quand le cache a été mis à jour
}
```

Effacé quand la livraison passe en `DELIVERED` ou `CANCELLED` et que la sync est
confirmée.

### `position_queue`

File des positions GPS capturées hors-ligne.

```typescript
interface QueuedPosition {
  idempotencyKey: string;   // UUID généré par le device
  lat: number;
  lon: number;
  reportedAt: string;       // horodatage du device (UTC)
  syncStatus: 'pending' | 'synced' | 'failed';
}
```

Entrées `synced` purgées après 24 h. Entrées `failed` après 3 tentatives → marquées
`failed` et conservées pour diagnostic, sans bloquer la suite.

### `action_queue`

File des actions d'état (pickup, deliver) en attente de synchronisation.

```typescript
interface QueuedAction {
  id: string;               // UUID local
  type: 'pickup' | 'deliver';
  orderId: string;
  idempotencyKey: string;   // UUID généré par le device
  queuedAt: string;
  syncStatus: 'pending' | 'synced' | 'failed';
  attempts: number;
}
```

---

## 4. Capture de position GPS

Le service worker capture la position GPS toutes les **10 secondes** (configurable)
lorsque la course est active, indépendamment de la connectivité :

```
┌──────────────────────────────────────────────────────────┐
│  Service Worker — position loop (10 s)                   │
│                                                          │
│  1. navigator.geolocation.getCurrentPosition()           │
│  2. Générer idempotencyKey = crypto.randomUUID()         │
│  3. Écrire dans position_queue (syncStatus: 'pending')   │
│  4. Si connecté → tenter POST /positions immédiatement   │
│     → succès : syncStatus = 'synced'                     │
│     → échec  : garder 'pending' pour la sync en lot      │
└──────────────────────────────────────────────────────────┘
```

En mode connecté, la plupart des positions sont envoyées immédiatement (endpoint
`POST /positions` avec `Idempotency-Key`). En mode hors-ligne, elles s'accumulent
dans `position_queue`.

---

## 5. Synchronisation au retour de la connexion

L'application écoute l'événement `online` du navigateur et déclenche la séquence
suivante :

```
Connexion rétablie
        │
        ▼
┌───────────────────────┐
│ 1. Sync positions     │  POST /positions/batch (tout le pending, max 500 par lot)
│    en lot             │  → idempotent : les doublons sont comptés, pas rejetés
└──────────┬────────────┘
           │ succès
           ▼
┌───────────────────────┐
│ 2. Sync actions       │  Pour chaque action pending dans action_queue :
│    d'état             │  POST /orders/{id}/pickup  ou  /orders/{id}/deliver
│                       │  avec l'Idempotency-Key stocké
└──────────┬────────────┘
           │ succès
           ▼
┌───────────────────────┐
│ 3. Rafraîchir l'état  │  GET /orders/{id}/delivery → mettre à jour active_delivery
│    depuis le serveur  │  (vérifier que l'état côté serveur est cohérent)
└───────────────────────┘
```

La synchronisation est **séquentielle** : les positions d'abord, puis les actions d'état.
Cet ordre garantit que le serveur a la trace GPS complète avant de traiter le changement
d'état.

---

## 6. Règles de résolution de conflits

### Cas 1 : action déjà traitée côté serveur (409 Conflict)

Si le serveur répond `409` à un `pickup` ou `deliver`, c'est que l'action a déjà été
appliquée (l'`Idempotency-Key` correspondant est connu). L'application :
1. Marque l'action `synced` dans `action_queue`
2. Rafraîchit l'état depuis le serveur

Pas d'erreur affichée à l'utilisateur ; le résultat final est le même.

### Cas 2 : course annulée ou réaffectée pendant la déconnexion

Si au retour de la connexion le serveur renvoie un état inattendu (`CANCELLED` ou
livreur différent pour cette livraison) :
1. Vider `active_delivery`
2. Afficher : « Cette course a été annulée ou réaffectée. »
3. Retour à l'écran d'accueil

### Cas 3 : action `deliver` mais état serveur déjà `DELIVERED`

Même comportement que Cas 1 : idempotent, pas d'erreur.

### Cas 4 : positions avec horodatage dans le passé

Le serveur accepte toutes les positions quelle que soit leur ancienneté (`reportedAt`
est l'horodatage du device). Les doublons sont filtrés par `idempotency_key`. Les
positions hors-ligne apparaissent dans le journal de la livraison avec leur vrai
horodatage, permettant une reconstruction fidèle du trajet.

---

## 7. Limites et comportements non supportés en V1

| Situation | Comportement V1 |
|-----------|----------------|
| Coupure > 30 min avec > 500 positions accumulées | Envoyées en plusieurs lots de 500 au retour |
| Livreur change de device en cours de route | Non supporté : `active_delivery` est local au device |
| Deux onglets ouverts simultanément | Non supporté : un seul Service Worker actif par origin |
| Proposition reçue pendant la déconnexion | Perdue ; le délai de l'offre expire côté serveur, passage au livreur suivant |

---

## 8. Banière de connectivité (UI)

Une banière discrète en haut de l'application indique l'état de connexion :

```
┌─────────────────────────────────────────────────────────┐
│  🔴 Hors-ligne · 12 positions en attente               │  ← mode hors-ligne
│  🟢 Synchronisation terminée                           │  ← après sync
└─────────────────────────────────────────────────────────┘
```

Elle disparaît automatiquement 3 secondes après la confirmation de la synchronisation.
