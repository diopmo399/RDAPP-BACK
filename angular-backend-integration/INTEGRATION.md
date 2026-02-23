# 🔗 Intégration Angular ↔ Backend — Pipeline & Sprint

## Architecture

```
┌─────────────────────────────────────────────┐
│  Angular Frontend (port 4200)               │
│                                             │
│  DeployApiService ──→ HttpClient            │
│       │                    │                │
│       ▼                    ▼                │
│  DeployService        /api (proxy)          │
│  (state + logic)       │                    │
│       │                │                    │
│       ▼                ▼                    │
│  Components     ───────────────────────┐    │
│  - Pipeline    │  Spring Boot (8080)   │    │
│  - Sprint      │                       │    │
│  - Dashboard   │  GET /v1/apps/full    │    │
│  - Header      │  GET /v1/apps/{id}    │    │
│                │  GET /v1/jira/sprints  │    │
│                │  POST /v1/jira/sync    │    │
│                └───────────────────────┘    │
└─────────────────────────────────────────────┘
```

## Endpoints Backend utilisés

### Pipeline (nouveau)
| Méthode | Route | Description |
|---------|-------|-------------|
| `GET` | `/api/v1/apps` | Liste résumée (sans commits) |
| `GET` | `/api/v1/apps/full` | Liste complète (avec commits) — **dashboard** |
| `GET` | `/api/v1/apps/{id}` | Détail complet d'une app |
| `GET` | `/api/v1/apps/{id}/commits` | Commits groupés par version |
| `PUT` | `/api/v1/apps/{id}/envs/{envKey}` | MAJ déploiement (webhook CI/CD) |
| `POST` | `/api/v1/apps/{id}/commits` | Ajouter un commit (webhook CI/CD) |

### Sprint / Jira (existant)
| Méthode | Route | Description |
|---------|-------|-------------|
| `GET` | `/api/v1/squads` | Liste des escouades |
| `GET` | `/api/v1/jira/sprints/squad/{squadId}/active` | Sprint actif |
| `POST` | `/api/v1/jira/sync/squad/{squadId}` | Sync Jira pour une escouade |
| `POST` | `/api/v1/jira/sync/all` | Sync toutes les escouades |
| `GET` | `/api/v1/jira/status` | Statut connexion Jira |

## Fichiers Angular modifiés

### Nouveaux fichiers
| Fichier | Rôle |
|---------|------|
| `services/deploy-api.service.ts` | **HttpClient** — tous les appels API |
| `models/api.models.ts` | Interfaces des réponses API |
| `environments/environment.ts` | Config `apiBaseUrl` |
| `app.config.ts` | `provideHttpClient()` |
| `proxy.conf.json` | Proxy dev `/api → localhost:8080` |

### Fichiers modifiés
| Fichier | Changement |
|---------|------------|
| `services/deploy.service.ts` | Appelle `DeployApiService` au lieu des données mock |
| `components/deploy-dashboard/` | `ngOnInit()` → `svc.init()`, barre source, loader |
| `components/app-card/` | Sprint loading, bouton refresh, mode API |
| `components/header/` | Compteur apps dynamique |

### Fichiers inchangés
- `components/pipeline/` — template/scss/ts identiques
- `components/sprint-panel/` — template/scss/ts identiques
- `models/deploy.models.ts` — interfaces inchangées
- `data/apps.data.ts` — conservé comme **fallback mock**

## Fichiers Backend ajoutés

### Entités
| Fichier | Table |
|---------|-------|
| `entity/Application.java` | `application` |
| `entity/EnvironmentDeployment.java` | `environment_deployment` |
| `entity/CommitRecord.java` | `commit_record` |

### Service & Controller
| Fichier | Rôle |
|---------|------|
| `service/ApplicationService.java` | CRUD + mapping DTO |
| `controller/ApplicationController.java` | REST endpoints `/v1/apps` |
| `dto/AppDto.java` | DTOs request/response |
| `repository/ApplicationRepository.java` | JPA + queries custom |
| `repository/CommitRecordRepository.java` | JPA commits |

### Liquibase
| Fichier | Contenu |
|---------|---------|
| `003-pipeline-tables.yaml` | Tables: `application`, `environment_deployment`, `commit_record` |
| `seed-pipeline-data.sql` | Données initiales (6 apps, déploiements, commits) |

## Installation

### 1. Backend

Ajouter le changelog dans `db.changelog-master.yaml` :
```yaml
  - include:
      file: db/changelog/003-pipeline-tables.yaml
  - changeSet:
      id: 006-seed-pipeline-data
      author: deploy-dashboard
      changes:
        - sqlFile:
            path: db/changelog/seed-pipeline-data.sql
```

Copier les fichiers Java dans le projet existant.

### 2. Angular

```bash
# Ajouter HttpClient dans app.config.ts
provideHttpClient(withInterceptorsFromDi())

# Proxy dev
ng serve --proxy-config proxy.conf.json

# Ou dans angular.json
"serve": {
  "options": {
    "proxyConfig": "proxy.conf.json"
  }
}
```

## Comportement

### Mode API (backend disponible)
1. Au démarrage → `GET /api/v1/apps/full`
2. Barre verte : "✓ Connecté au backend API"
3. Pipeline affiche données live
4. Sprint → charge via `GET /api/v1/jira/sprints/squad/{id}/active`
5. Issues Jira mappées par `statusCategory`:
   - `new` → Non débutés
   - `indeterminate` → En cours / En PR
   - `done` → (pas affiché)

### Mode Mock (backend indisponible)
1. Appel API échoue → fallback `apps.data.ts`
2. Barre orange : "⚠ Données locales (backend indisponible)"
3. Données statiques inchangées
4. Bouton "↻ Rafraîchir" pour retenter

### Sprint — mapping des issues Jira

```
statusCategory "new"           → Panel "Non débutés"
statusCategory "indeterminate" → Panel "En cours" (défaut)
  + statusName contains "review/PR" → Panel "En PR"
statusCategory "done"          → (filtré, pas affiché)
```

## Webhooks CI/CD

Le backend expose des endpoints pour recevoir des events de CI/CD :

```bash
# Mettre à jour un déploiement
curl -X PUT http://localhost:8080/api/v1/apps/orch-paiements/envs/dev \
  -H "Content-Type: application/json" \
  -d '{"version":"2.6.0-dev.1","status":"deployed","branch":"feat/new","instances":2}'

# Ajouter un commit
curl -X POST http://localhost:8080/api/v1/apps/orch-paiements/commits \
  -H "Content-Type: application/json" \
  -d '{"versionTag":"2.6.0-dev.1","sha":"abc123","message":"feat: new feature","ticket":"ORC-400","commitType":"feat","author":"Dev"}'
```
