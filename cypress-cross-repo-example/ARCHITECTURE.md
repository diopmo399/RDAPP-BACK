# Architecture Technique - Cypress Cross-Repo E2E Testing

## Vue d'ensemble

Cette solution impl\u00e9mente une orchestration de tests E2E entre deux repositories GitHub ind\u00e9pendants en utilisant l'API GitHub Actions et les artifacts pour la communication inter-workflow.

## Architecture globale

```
┌────────────────────────────────────────────────────────────────┐
│                    GitHub Actions Ecosystem                     │
│                                                                 │
│  ┌──────────────────────┐         ┌──────────────────────┐    │
│  │   Repo B (Tests)     │         │  Repo A (Application) │    │
│  │                      │         │                       │    │
│  │  cypress-e2e.yml     │         │    deploy.yml         │    │
│  │                      │         │                       │    │
│  │  ┌─────────────┐     │         │  ┌─────────────┐     │    │
│  │  │ Job 1:      │     │   (1)   │  │ Deploy      │     │    │
│  │  │ Trigger     ├─────┼────────>│  │ Application │     │    │
│  │  └─────────────┘     │  GitHub │  └──────┬──────┘     │    │
│  │                      │   API   │         │            │    │
│  │  ┌─────────────┐     │         │         │(2)         │    │
│  │  │ Job 2:      │     │  Poll   │         v            │    │
│  │  │ Wait for    │<────┼─────────┤  ┌─────────────┐     │    │
│  │  │ Deployment  │     │  Status │  │ Upload      │     │    │
│  │  └──────┬──────┘     │         │  │ Artifact    │     │    │
│  │         │            │         │  └─────────────┘     │    │
│  │         │(3)         │         │                      │    │
│  │         v            │         │                      │    │
│  │  ┌─────────────┐     │         │                      │    │
│  │  │ Download    │<────┼─────────┼──────────────────────┤    │
│  │  │ Artifact    │     │  GitHub │    deployment.json   │    │
│  │  └──────┬──────┘     │ Artifact│                      │    │
│  │         │            │   API   │                      │    │
│  │         │(4)         │         │                      │    │
│  │         v            │         │                      │    │
│  │  ┌─────────────┐     │         │                      │    │
│  │  │ Job 3:      │     │         │                      │    │
│  │  │ Run Cypress │     │         │                      │    │
│  │  │ Tests       │     │         │                      │    │
│  │  └─────────────┘     │         │                      │    │
│  └──────────────────────┘         └──────────────────────┘    │
└────────────────────────────────────────────────────────────────┘
```

## Flux de donn\u00e9es d\u00e9taill\u00e9

### Phase 1 : D\u00e9clenchement du d\u00e9ploiement

**Repo B > Job: trigger-deployment**

1. **D\u00e9terminer les param\u00e8tres de d\u00e9ploiement**
   ```yaml
   # Environnement : staging/preprod/production
   # D\u00e9ployer quelle branche/tag ?

   if workflow_dispatch:
     ENVIRONMENT = input.environment
     DEPLOY_REF = input.deploy_ref
   elif push to main:
     ENVIRONMENT = staging
     DEPLOY_REF = main
   else:
     ENVIRONMENT = staging
     DEPLOY_REF = current_branch
   ```

2. **D\u00e9clencher le workflow via GitHub API**
   ```bash
   gh api POST /repos/OWNER/REPO-A/actions/workflows/deploy.yml/dispatches \
     -f ref="${DEPLOY_REF}" \
     -f inputs[environment]="${ENVIRONMENT}" \
     -f inputs[triggered_by_repo]="${GITHUB_REPOSITORY}"
   ```

3. **R\u00e9cup\u00e9rer le Run ID**
   ```bash
   # Attendre 5s que le workflow soit cr\u00e9\u00e9
   sleep 5

   # Chercher le run le plus r\u00e9cent (in_progress ou queued)
   RUN_ID=$(gh api /repos/OWNER/REPO-A/actions/workflows/deploy.yml/runs?status=in_progress \
     --jq '.workflow_runs[0].id')
   ```

4. **Outputs**
   - `run-id` : ID du workflow d\u00e9clench\u00e9
   - `run-url` : URL du workflow sur GitHub

### Phase 2 : Attente de compl\u00e9tion (Polling)

**Repo B > Job: wait-for-deployment**

1. **Boucle de polling**
   ```bash
   MAX_WAIT_TIME=1800  # 30 minutes
   POLL_INTERVAL=15    # 15 secondes

   while true; do
     STATUS=$(gh api /repos/OWNER/REPO-A/actions/runs/${RUN_ID} --jq '.status')
     CONCLUSION=$(gh api /repos/OWNER/REPO-A/actions/runs/${RUN_ID} --jq '.conclusion')

     if [ "$STATUS" = "completed" ]; then
       if [ "$CONCLUSION" = "success" ]; then
         break
       else
         exit 1  # D\u00e9ploiement \u00e9chou\u00e9
       fi
     fi

     sleep $POLL_INTERVAL
   done
   ```

2. **T\u00e9l\u00e9charger l'artifact**
   ```bash
   # Lister les artifacts du run
   ARTIFACT_ID=$(gh api /repos/OWNER/REPO-A/actions/runs/${RUN_ID}/artifacts \
     --jq '.artifacts[] | select(.name | startswith("deployment-info")) | .id' \
     | head -1)

   # T\u00e9l\u00e9charger le ZIP
   gh api /repos/OWNER/REPO-A/actions/artifacts/${ARTIFACT_ID}/zip > deployment-artifact.zip

   # Extraire deployment.json
   unzip -q deployment-artifact.zip
   ```

3. **Extraire l'URL de d\u00e9ploiement**
   ```bash
   BASE_URL=$(jq -r '.baseUrl' deployment.json)
   ```

4. **Outputs**
   - `base-url` : URL de l'application d\u00e9ploy\u00e9e
   - `deployment-info` : JSON complet avec m\u00e9tadonn\u00e9es

### Phase 3 : Ex\u00e9cution des tests Cypress

**Repo B > Job: cypress-tests**

1. **D\u00e9terminer l'URL cible**
   ```yaml
   if skip_deployment:
     BASE_URL = input.base_url
   else:
     BASE_URL = needs.wait-for-deployment.outputs.base-url
   ```

2. **Ex\u00e9cuter Cypress**
   ```yaml
   - uses: cypress-io/github-action@v6
     with:
       browser: chrome
       parallel: true
       config: baseUrl=${BASE_URL}
     env:
       CYPRESS_BASE_URL: ${BASE_URL}
   ```

3. **Upload artifacts**
   - Screenshots (si \u00e9chec)
   - Videos (tous les tests)
   - R\u00e9sultats JSON/XML

### Phase 4 : Rapport final

**Repo B > Job: report**

1. **T\u00e9l\u00e9charger tous les artifacts**
2. **G\u00e9n\u00e9rer le r\u00e9sum\u00e9** dans `$GITHUB_STEP_SUMMARY`
3. **Commenter la PR** (si applicable)

## D\u00e9tails techniques

### Authentification cross-repo

**Probl\u00e8me** : `GITHUB_TOKEN` par d\u00e9faut ne peut pas d\u00e9clencher des workflows dans d'autres repos.

**Solution** : Personal Access Token (PAT) avec permissions `repo` + `actions:write`

```yaml
# Repo B
env:
  GH_TOKEN: ${{ secrets.DEPLOY_REPO_PAT }}

# Utilisation
gh api POST /repos/OWNER/REPO-A/actions/workflows/deploy.yml/dispatches ...
```

**S\u00e9curit\u00e9** :
- Token stock\u00e9 dans GitHub Secrets (chiffr\u00e9)
- Jamais expos\u00e9 dans les logs
- Permissions minimales (scope limit\u00e9)
- Expiration recommand\u00e9e : 90 jours

### Communication inter-workflow

**Probl\u00e8me** : Comment passer des donn\u00e9es (URL de d\u00e9ploiement) de Repo A \u00e0 Repo B ?

**Approches envisag\u00e9es** :

| Approche | Avantages | Inconv\u00e9nients | Retenu ? |
|----------|-----------|----------------|----------|
| **GitHub Artifacts** | Simple, natif, s\u00e9curis\u00e9 | N\u00e9cessite t\u00e9l\u00e9chargement | ✅ OUI |
| Workflow Outputs | Direct | Ne fonctionne pas cross-repo | ❌ NON |
| GitHub Deployments API | Natif | Complexe, meta-donn\u00e9es limit\u00e9es | ❌ NON |
| Fichier dans repo | Persistant | N\u00e9cessite commit, pollue l'historique | ❌ NON |
| Service externe (S3, etc.) | Flexible | D\u00e9pendance externe, co\u00fbt | ❌ NON |

**Solution retenue : Artifacts**

```yaml
# Repo A : Upload
- uses: actions/upload-artifact@v4
  with:
    name: deployment-info-${{ github.run_id }}
    path: deployment/deployment.json
    retention-days: 7

# Repo B : Download
gh api /repos/OWNER/REPO-A/actions/artifacts/${ARTIFACT_ID}/zip > artifact.zip
unzip -q artifact.zip
BASE_URL=$(jq -r '.baseUrl' deployment.json)
```

**Format de deployment.json** :
```json
{
  "baseUrl": "https://staging-app-123.example.com",
  "environment": "staging",
  "ref": "main",
  "deployedAt": "2025-12-30T10:30:00Z",
  "runId": "12345678",
  "runNumber": "42",
  "triggeredBy": "owner/repo-b"
}
```

### Gestion de la concurrence

**Probl\u00e8me** : Plusieurs d\u00e9ploiements simultan\u00e9s peuvent causer des conflits.

**Solution : Concurrency groups**

```yaml
# Repo A : Annuler d\u00e9ploiements pr\u00e9c\u00e9dents pour le MÊME environnement
concurrency:
  group: deploy-${{ github.event.inputs.environment }}
  cancel-in-progress: true

# Repo B : Annuler tests pr\u00e9c\u00e9dents pour la MÊME branche
concurrency:
  group: cypress-${{ github.ref }}
  cancel-in-progress: true
```

**Comportement** :
- Push 1 sur `develop` → D\u00e9ploiement staging + Tests d\u00e9marrent
- Push 2 sur `develop` (30s apr\u00e8s) → Annule Push 1, d\u00e9marre Push 2
- Push sur `feature/a` + Push sur `feature/b` → Parall\u00e8le (diff\u00e9rents groupes)

### Polling GitHub API

**Probl\u00e8me** : Comment savoir quand le d\u00e9ploiement est termin\u00e9 ?

**Solution : Polling avec timeout**

```bash
MAX_WAIT_TIME=1800  # 30 minutes
POLL_INTERVAL=15    # 15 secondes
START_TIME=$(date +%s)

while true; do
  CURRENT_TIME=$(date +%s)
  ELAPSED=$((CURRENT_TIME - START_TIME))

  if [ $ELAPSED -ge $MAX_WAIT_TIME ]; then
    echo "::error::Timeout"
    exit 1
  fi

  STATUS=$(gh api /repos/.../actions/runs/${RUN_ID} --jq '.status')
  CONCLUSION=$(gh api /repos/.../actions/runs/${RUN_ID} --jq '.conclusion')

  if [ "$STATUS" = "completed" ]; then
    if [ "$CONCLUSION" = "success" ]; then
      break
    else
      exit 1
    fi
  fi

  sleep $POLL_INTERVAL
done
```

**Optimisations** :
- Utilise `gh api` (GitHub CLI) pour g\u00e9rer l'auth automatiquement
- Utilise `jq` pour parser JSON
- Timeout configurable
- Intervalle de polling ajustable (trade-off latence vs rate limit)

**Rate limiting** :
- Limite : 1000 req/h avec `GITHUB_TOKEN`, 5000 req/h avec PAT
- Avec intervalle de 15s : 240 req/h (bien en dessous de la limite)

### Parall\u00e9lisation Cypress

**Strat\u00e9gie matrix**

```yaml
strategy:
  fail-fast: false
  matrix:
    containers: [1, 2]  # 2 workers en parall\u00e8le
```

**R\u00e9partition automatique** :
- Cypress r\u00e9partit les tests entre les containers
- Chaque container ex\u00e9cute un sous-ensemble de tests
- R\u00e9sultats combin\u00e9s \u00e0 la fin

**Artifacts uniques** :
```yaml
- uses: actions/upload-artifact@v4
  with:
    name: cypress-videos-${{ matrix.containers }}  # videos-1, videos-2
    path: cypress/videos
```

### Gestion des erreurs

**Cha\u00eene de d\u00e9pendances** :

```yaml
jobs:
  trigger-deployment:
    # Si \u00e9choue → tout s'arr\u00eate

  wait-for-deployment:
    needs: trigger-deployment
    # Si \u00e9choue → cypress-tests ne s'ex\u00e9cute pas

  cypress-tests:
    needs: wait-for-deployment
    if: always() && needs.wait-for-deployment.result == 'success'
    # S'ex\u00e9cute seulement si wait-for-deployment r\u00e9ussit

  report:
    needs: cypress-tests
    if: always()
    # S'ex\u00e9cute toujours (m\u00eame si tests \u00e9chouent)
```

**Codes de sortie** :
- `exit 0` : Succ\u00e8s
- `exit 1` : \u00c9chec
- GitHub Actions interpr\u00e8te automatiquement les codes de sortie

### Mode skip_deployment

**Use case** : Tester contre un d\u00e9ploiement existant sans red\u00e9ployer

**Impl\u00e9mentation** :

```yaml
jobs:
  trigger-deployment:
    if: github.event.inputs.skip_deployment != 'true'
    # Ne s'ex\u00e9cute pas si skip_deployment=true

  wait-for-deployment:
    if: github.event.inputs.skip_deployment != 'true'
    # Ne s'ex\u00e9cute pas si skip_deployment=true

  cypress-tests:
    needs: [wait-for-deployment]
    if: |
      always() && (
        needs.wait-for-deployment.result == 'success' ||
        github.event.inputs.skip_deployment == 'true'
      )
    # S'ex\u00e9cute si :
    # - D\u00e9ploiement r\u00e9ussi OU
    # - Skip deployment activ\u00e9
```

**D\u00e9termination de l'URL** :

```yaml
- id: url
  run: |
    if [ "${{ github.event.inputs.skip_deployment }}" = "true" ]; then
      BASE_URL="${{ github.event.inputs.base_url }}"
    else
      BASE_URL="${{ needs.wait-for-deployment.outputs.base-url }}"
    fi
    echo "base-url=${BASE_URL}" >> $GITHUB_OUTPUT
```

## Sch\u00e9ma de s\u00e9quence d\u00e9taill\u00e9

```
┌─────────┐          ┌─────────────┐         ┌────────────┐         ┌─────────┐
│ User/PR │          │  Repo B     │         │ GitHub API │         │ Repo A  │
└────┬────┘          └──────┬──────┘         └─────┬──────┘         └────┬────┘
     │                      │                      │                     │
     │ Push/PR/Manual       │                      │                     │
     ├─────────────────────>│                      │                     │
     │                      │                      │                     │
     │                      │ POST /workflows/     │                     │
     │                      │   deploy.yml/        │                     │
     │                      │   dispatches         │                     │
     │                      ├─────────────────────>│                     │
     │                      │                      │                     │
     │                      │                      │ Trigger workflow    │
     │                      │                      ├────────────────────>│
     │                      │                      │                     │
     │                      │                      │   deploy.yml runs   │
     │                      │                      │                     ├──┐
     │                      │                      │                     │  │ Build
     │                      │                      │                     │<─┘
     │                      │                      │                     │
     │                      │                      │                     ├──┐
     │                      │                      │                     │  │ Deploy
     │                      │ GET /runs/${RUN_ID}  │                     │<─┘
     │                      ├─────────────────────>│                     │
     │                      │<─────────────────────┤                     │
     │                      │  {status:in_progress}│                     │
     │                      │                      │                     │
     │                      │ (sleep 15s)          │                     │
     │                      ├──┐                   │                     │
     │                      │<─┘                   │                     │
     │                      │                      │                     │
     │                      │ GET /runs/${RUN_ID}  │                     │
     │                      ├─────────────────────>│                     │
     │                      │<─────────────────────┤                     │
     │                      │  {status:completed,  │                     │
     │                      │   conclusion:success}│                     │
     │                      │                      │                     │
     │                      │ GET /artifacts       │                     │
     │                      ├─────────────────────>│                     │
     │                      │<─────────────────────┤                     │
     │                      │  {artifact_id: 123}  │                     │
     │                      │                      │                     │
     │                      │ GET /artifacts/123/  │                     │
     │                      │     zip              │                     │
     │                      ├─────────────────────>│                     │
     │                      │<─────────────────────┤                     │
     │                      │  deployment.json     │                     │
     │                      │                      │                     │
     │                      │ Run Cypress          │                     │
     │                      ├──┐                   │                     │
     │                      │  │ baseUrl from      │                     │
     │                      │  │ deployment.json   │                     │
     │                      │<─┘                   │                     │
     │                      │                      │                     │
     │<─────────────────────┤                      │                     │
     │   Test results       │                      │                     │
     │   (PR comment)       │                      │                     │
     │                      │                      │                     │
```

## Performance et co\u00fbts

### Temps d'ex\u00e9cution typique

| Phase | Dur\u00e9e | D\u00e9tails |
|-------|--------|----------|
| Trigger deployment | 5-10s | API call + r\u00e9cup\u00e9ration run ID |
| D\u00e9ploiement (mock) | 30s | Simulation |
| D\u00e9ploiement (r\u00e9el) | 2-5 min | Vercel/Netlify/AWS |
| Polling overhead | 15-30s | Intervalle de 15s |
| Download artifact | 5-10s | Petit fichier JSON |
| Cypress tests | 1-5 min | D\u00e9pend du nombre de tests |
| **Total (mock)** | **2-3 min** | |
| **Total (r\u00e9el)** | **5-10 min** | |

### Consommation GitHub Actions minutes

**Exemple : 1 workflow complet**

| Repo | Job | Dur\u00e9e | Minutes |
|------|-----|--------|---------|
| Repo B | trigger-deployment | 10s | 0.17 |
| Repo A | deploy | 3 min | 3 |
| Repo B | wait-for-deployment | 30s | 0.5 |
| Repo B | cypress-tests (x2 containers) | 2 min x2 | 4 |
| Repo B | report | 10s | 0.17 |
| **Total** | | | **7.84 min** |

**Co\u00fbt** (GitHub Free : 2000 min/mois) :
- 1 workflow = ~8 minutes
- 250 ex\u00e9cutions/mois possibles
- Largement suffisant pour la plupart des projets

### Optimisations possibles

1. **R\u00e9duire le polling interval** : 15s → 30s (r\u00e9duit overhead mais augmente latence)
2. **D\u00e9sactiver vid\u00e9os Cypress** : `video: false` dans cypress.config.js
3. **Cache npm agressif** : `cache: 'npm'` dans setup-node
4. **Parall\u00e9lisation limit\u00e9e** : 2 containers au lieu de 4+

## S\u00e9curit\u00e9

### Mod\u00e8le de menace

**Attaquant potentiel** : Contributeur malveillant avec acc\u00e8s \u00e0 Repo B

**Attaques possibles** :
1. Exfiltration du token PAT via logs
2. D\u00e9clenchement de d\u00e9ploiements non autoris\u00e9s
3. Acc\u00e8s \u00e0 l'artifact de d\u00e9ploiement

**Mitigations** :

| Risque | Mitigation | Impl\u00e9ment\u00e9 |
|--------|------------|-------------|
| Token expos\u00e9 | GitHub masque automatiquement les secrets dans logs | ✅ |
| D\u00e9ploiement non autoris\u00e9 | Utiliser `environment` protection rules | ⚠️  Optionnel |
| Artifact accessible | Artifacts expir\u00e9s apr\u00e8s 7 jours | ✅ |
| Token compromis | Expiration du token (90 jours), scope minimal | ✅ |

### Permissions minimales

**Repo A** :
```yaml
permissions:
  contents: read        # Lire le code
  deployments: write    # Cr\u00e9er deployments GitHub
  actions: write        # Upload artifacts
```

**Repo B** :
```yaml
permissions:
  contents: read        # Lire le code
  actions: read         # Lire artifacts (via PAT)
  pull-requests: write  # Commenter PR (optionnel)
```

**PAT Token** :
- `repo` : N\u00e9cessaire pour d\u00e9clencher workflows
- `actions:write` : N\u00e9cessaire pour d\u00e9clencher workflows
- Pas de `admin:org`, `delete_repo`, etc.

## Extension et customisation

### Ajouter un environnement

1. Modifier `deploy.yml` (Repo A) :
   ```yaml
   inputs:
     environment:
       options:
         - staging
         - preprod
         - production
         - qa  # NOUVEAU
   ```

2. Ajouter le case dans le d\u00e9ploiement :
   ```bash
   case "$ENVIRONMENT" in
     qa)
       DEPLOY_URL="https://qa-app.example.com"
       ;;
   esac
   ```

### Int\u00e9grer un vrai d\u00e9ploiement

Voir exemples dans README.md (Vercel, Netlify, AWS, Firebase).

**Important** : Toujours conserver la g\u00e9n\u00e9ration de `deployment.json` avec `baseUrl`.

### Ajouter des notifications

**Slack** :
```yaml
- name: Notify Slack
  if: failure()
  uses: slackapi/slack-github-action@v1
  with:
    payload: |
      {
        "text": "Tests Cypress \u00e9chou\u00e9s sur ${{ github.ref }}"
      }
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

**Email** :
```yaml
- name: Send email
  if: failure()
  uses: dawidd6/action-send-mail@v3
  with:
    server_address: smtp.gmail.com
    server_port: 465
    username: ${{ secrets.EMAIL_USERNAME }}
    password: ${{ secrets.EMAIL_PASSWORD }}
    subject: Tests Cypress \u00e9chou\u00e9s
    to: team@example.com
    from: github-actions@example.com
```

## Limitations connues

1. **Latency** : Polling introduit un d\u00e9lai minimum de 15-30s
2. **Rate limiting** : GitHub API limit\u00e9 \u00e0 5000 req/h (rarement atteint)
3. **Artifacts** : Taille max 2GB (largement suffisant pour deployment.json)
4. **Cross-org** : N\u00e9cessite un PAT avec acc\u00e8s aux deux orgs
5. **Private repos** : N\u00e9cessite un PAT avec scope `repo` (pas `public_repo`)

## Alternatives envisag\u00e9es

| Solution | Avantages | Inconv\u00e9nients | Retenu ? |
|----------|-----------|----------------|----------|
| **Artifacts (actuel)** | Simple, s\u00e9curis\u00e9, natif | Polling n\u00e9cessaire | ✅ |
| Repository Dispatch | \u00c9v\u00e9nementiel | Pas de r\u00e9ponse sync | ❌ |
| GitHub Deployments API | Natif | M\u00e9tadonn\u00e9es limit\u00e9es | ❌ |
| Webhook externe | Temps r\u00e9el | Infrastructure externe | ❌ |
| Monorepo | Pas de cross-repo | Impose l'architecture | ❌ |

---

**Document\u00e9 le** : 2025-12-30
**Version** : 1.0
