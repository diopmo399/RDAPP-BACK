# API Sprint Global

## Vue d'ensemble

Cette API permet de récupérer les sprints actifs de toutes les équipes avec leurs tickets groupés par statut.

## Architecture

```
┌─────────────────┐
│  JiraSyncService │ ──> Synchronise depuis Jira DC
└────────┬────────┘
         │ Écrit dans
         ▼
┌─────────────────┐
│ sprint_sync     │ ──> Base de données
│ sprint_issue    │
└────────┬────────┘
         │ Lit depuis
         ▼
┌─────────────────┐
│SprintCacheService│ ──> Service avec cache
└────────┬────────┘
         │ Utilise
         ▼
┌─────────────────┐
│  SprintMapper   │ ──> Transforme entités → modèles
└────────┬────────┘
         │ Retourne
         ▼
┌─────────────────┐
│SprintController │ ──> API REST
└─────────────────┘
```

## Fichiers créés

### 📁 `/controller/`
- **SprintController.java** - Endpoints REST

### 📁 `/service/`
- **SprintCacheService.java** - Logique métier avec cache

### 📁 `/mapper/`
- **SprintMapper.java** - Conversion entités ↔ modèles API

### 📁 `/model/`
- **SprintGlobalResponse.java** - Réponse complète
- **SprintInfo.java** - Info du sprint
- **SprintTicket.java** - Modèle de ticket
- **AffectVersionInfo.java** - Info des versions (releases)

### 📁 `/repository/`
- **SprintSyncRepository.java** (modifié) - Requête pour sprints actifs

### 📁 `/config/`
- **CacheConfig.java** - Configuration du cache Spring

## Endpoints

### 1. GET `/api/sprint/global`

Récupère tous les sprints actifs avec leurs tickets groupés par statut.

**Réponse :**
```json
{
  "sprint": {
    "name": "Sprint 24",
    "startDate": "2025-01-15T00:00:00",
    "endDate": "2025-01-29T00:00:00",
    "state": "active"
  },
  "squads": {
    "squad-1": {
      "id": "squad-1",
      "name": "Team Alpha",
      "color": "#FF5733"
    },
    "squad-2": {
      "id": "squad-2",
      "name": "Team Beta",
      "color": "#33FF57"
    }
  },
  "versions": [
    {
      "id": "v2.5.0",
      "name": "v2.5.0",
      "status": "RELEASED",
      "releaseDate": "2025-01-15",
      "description": "Major release with new features"
    },
    {
      "id": "v2.6.0",
      "name": "v2.6.0",
      "status": "IN_PROGRESS",
      "releaseDate": "2025-02-01",
      "description": "Next sprint release"
    }
  ],
  "notStarted": [
    {
      "ticket": "PROJ-123",
      "title": "Implement new feature",
      "squad": "squad-1",
      "storyPoints": 5.0,
      "priority": "HIGH",
      "author": "John Doe",
      "status": "NOT_STARTED"
    }
  ],
  "inProgress": [
    {
      "ticket": "PROJ-124",
      "title": "Fix bug in authentication",
      "squad": "squad-2",
      "storyPoints": 3.0,
      "priority": "CRITICAL",
      "author": "Jane Smith",
      "status": "IN_PROGRESS",
      "progress": 50,
      "branch": "feature/auth-fix",
      "affectVersion": "v2.4.0"
    }
  ],
  "done": [
    {
      "ticket": "PROJ-122",
      "title": "Update documentation",
      "squad": "squad-1",
      "storyPoints": 2.0,
      "priority": "MEDIUM",
      "author": "Bob Wilson",
      "status": "DONE",
      "completedDate": "2025-01-22T14:30:00",
      "version": "v2.5.0"
    }
  ],
  "totalPoints": 150.0,
  "donePoints": 85.0,
  "lastSync": "2025-01-23T10:30:00Z"
}
```

### 2. POST `/api/sprint/refresh`

Force le rafraîchissement du cache (admin).

**Réponse :** `200 OK`

### 3. GET `/api/sprint/versions`

Récupère toutes les versions actives (PLANNED, IN_PROGRESS, RELEASED).

**Réponse :**
```json
[
  {
    "id": "v2.5.0",
    "name": "v2.5.0",
    "status": "RELEASED",
    "releaseDate": "2025-01-15",
    "description": "Major release with new features"
  },
  {
    "id": "v2.6.0",
    "name": "v2.6.0",
    "status": "IN_PROGRESS",
    "releaseDate": "2025-02-01",
    "description": "Next sprint release"
  }
]
```

## Modèles de données

### SprintTicket.Status
- `NOT_STARTED` - Ticket pas encore commencé (statusCategory: "new")
- `IN_PROGRESS` - Ticket en cours (statusCategory: "indeterminate")
- `DONE` - Ticket terminé (statusCategory: "done")

### SprintTicket.Priority
- `CRITICAL` - Priorité critique/highest
- `HIGH` - Priorité haute
- `MEDIUM` - Priorité moyenne (défaut)
- `LOW` - Priorité basse/lowest

### AffectVersionInfo.Status
- `PLANNED` - Version planifiée
- `IN_PROGRESS` - Version en cours de développement
- `RELEASED` - Version publiée
- `ARCHIVED` - Version archivée (non incluse dans l'API)

## Flux de données

### 1. Synchronisation Jira
```bash
POST /api/jira/sync/all-squads
```
1. JiraSyncService récupère les données de Jira
2. Données persistées dans `sprint_sync` et `sprint_issue`
3. Cache invalidé automatiquement

### 2. Récupération par le frontend
```bash
GET /api/sprint/global
```
1. SprintController appelle SprintCacheService
2. Si cache vide : lecture depuis la DB
3. SprintMapper transforme les entités
4. Tickets groupés par status
5. Points calculés
6. Réponse JSON retournée (et mise en cache)

### 3. Cache
- **Durée :** Invalidé automatiquement après chaque sync
- **Clé :** `globalSprint::current`
- **Type :** ConcurrentMapCacheManager (en mémoire)

## Mapping des données

### Jira → API

| Jira | API SprintTicket |
|------|------------------|
| `issueKey` | `ticket` |
| `summary` | `title` |
| `statusCategory` = "new" | `status` = `NOT_STARTED` |
| `statusCategory` = "indeterminate" | `status` = `IN_PROGRESS` |
| `statusCategory` = "done" | `status` = `DONE` |
| `priority` = "Highest/Critical" | `priority` = `CRITICAL` |
| `priority` = "High" | `priority` = `HIGH` |
| `priority` = "Medium" | `priority` = `MEDIUM` |
| `priority` = "Low/Lowest" | `priority` = `LOW` |
| `storyPoints` | `storyPoints` |
| `assigneeName` | `author` |
| `fixVersions` (premier) | `version` |
| `versions` (premier) | `affectVersion` |
| `resolutionDate` | `completedDate` |

## Configuration

### Activer le cache
Le cache est activé via `@EnableCaching` dans `DeployConfigApiApplication.java`.

### Configuration personnalisée
Modifier `CacheConfig.java` pour ajuster le cache (ex: TTL, taille max, etc.).

## Tests

### Test manuel avec curl

```bash
# Récupérer les sprints actifs
curl http://localhost:8080/api/sprint/global

# Forcer le refresh du cache
curl -X POST http://localhost:8080/api/sprint/refresh
```

### Synchroniser depuis Jira

```bash
# Sync toutes les squads
curl -X POST http://localhost:8080/api/jira/sync/all-squads

# Sync une squad spécifique
curl -X POST http://localhost:8080/api/jira/sync/squad/squad-1
```

## Notes techniques

1. **Performance** : Le cache réduit la charge sur la DB. Après le premier appel, les réponses sont instantanées.

2. **Transactions** : `@Transactional(readOnly = true)` optimise les lectures depuis la DB.

3. **Lazy Loading** : Les relations `SprintSync → Squad` et `SprintSync → SprintIssue` sont chargées avec `LEFT JOIN FETCH`.

4. **Invalidation automatique** : Le cache est vidé après chaque sync Jira pour garantir la fraîcheur des données.

5. **Sécurité** : Ajouter `@PreAuthorize` sur les endpoints selon les besoins.

## Améliorations futures

- [ ] Ajouter pagination pour les grands sprints
- [ ] Ajouter filtres (par squad, par priorité, etc.)
- [ ] Ajouter métriques de burndown
- [ ] Implémenter Redis pour cache distribué
- [ ] Ajouter champs `progress` et `branch` depuis Git/CI
- [ ] WebSocket pour notifications temps réel
