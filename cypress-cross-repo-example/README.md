# Cypress Cross-Repo E2E Testing - Guide Complet

## Vue d'ensemble

Cette solution permet d'orchestrer des tests E2E Cypress entre deux repositories GitHub :
- **Repo A** : Application \u00e0 d\u00e9ployer (frontend, backend, full-stack)
- **Repo B** : Tests Cypress E2E

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Repo B (Tests)                       │
│                                                             │
│  1. Trigger Deployment                                      │
│     ├─> GitHub API: workflow_dispatch sur Repo A           │
│     └─> R\u00e9cup\u00e8re run ID                                     │
│                                                             │
│  2. Wait for Deployment                                     │
│     ├─> Poll GitHub API (status)                           │
│     ├─> Attend completion (max 30min)                      │
│     └─> T\u00e9l\u00e9charge artifact (deployment.json)               │
│                                                             │
│  3. Run Cypress Tests                                       │
│     ├─> Utilise baseUrl de l'artifact                      │
│     ├─> Ex\u00e9cute tests en parall\u00e8le                          │
│     └─> Upload screenshots/videos                          │
└─────────────────────────────────────────────────────────────┘
                            ↓ GitHub API
┌─────────────────────────────────────────────────────────────┐
│                      Repo A (Application)                   │
│                                                             │
│  1. Deploy Application                                      │
│     ├─> Build (npm run build)                              │
│     ├─> Deploy (Vercel/Netlify/AWS/etc.)                   │
│     └─> G\u00e9n\u00e8re deployment.json                            │
│                                                             │
│  2. Upload Artifact                                         │
│     └─> deployment-info-{run_id}                           │
│         └─> deployment.json (baseUrl, env, metadata)       │
│                                                             │
│  3. Health Check                                            │
│     └─> V\u00e9rifie que l'URL est accessible                    │
└─────────────────────────────────────────────────────────────┘
```

## Fichiers fournis

```
cypress-cross-repo-example/
├── repo-a/
│   └── .github/workflows/
│       └── deploy.yml              # Workflow de d\u00e9ploiement
│
├── repo-b/
│   ├── .github/workflows/
│   │   └── cypress-e2e.yml         # Workflow Cypress E2E
│   ├── cypress/
│   │   ├── e2e/                    # Tests Cypress (exemples)
│   │   ├── fixtures/               # Donn\u00e9es de test
│   │   └── support/                # Commandes custom
│   ├── cypress.config.js           # Configuration Cypress
│   └── package.json                # D\u00e9pendances
│
└── README.md                       # Ce fichier
```

## Setup - Mise en place compl\u00e8te

### \u00c9tape 1 : Cr\u00e9er un Personal Access Token (PAT)

Le token permet \u00e0 Repo B de d\u00e9clencher des workflows dans Repo A.

1. Allez sur GitHub : **Settings > Developer settings > Personal access tokens > Tokens (classic)**
2. Cliquez sur **Generate new token (classic)**
3. Nommez le token : `CYPRESS_CROSS_REPO_TOKEN`
4. S\u00e9lectionnez l'expiration : 90 jours ou No expiration
5. **Permissions minimales requises** :
   - `repo` (Full control of private repositories)
     - OU si repos publics : `public_repo` uniquement
   - `actions:write` (d\u00e9clencher workflows)
   - `actions:read` (lire status des runs)
6. Cliquez sur **Generate token**
7. **Copiez le token imm\u00e9diatement** (il ne sera plus visible)

### \u00c9tape 2 : Configurer les secrets dans Repo B (Tests)

Allez dans **Repo B > Settings > Secrets and variables > Actions**

| Secret | Valeur | Description |
|--------|--------|-------------|
| `DEPLOY_REPO_PAT` | `ghp_xxxxxxxxxxxxx` | Personal Access Token cr\u00e9\u00e9 \u00e0 l'\u00e9tape 1 |

**Pourquoi dans Repo B uniquement ?**
- C'est Repo B qui d\u00e9clenche les workflows dans Repo A
- Repo A n'a pas besoin de secrets suppl\u00e9mentaires (utilise `GITHUB_TOKEN` par d\u00e9faut)

### \u00c9tape 3 : Configurer les variables d'environnement dans Repo B

Dans le fichier `.github/workflows/cypress-e2e.yml` de Repo B, **modifiez ces variables** :

```yaml
env:
  DEPLOY_REPO_OWNER: 'YOUR_GITHUB_USERNAME'  # \u00c0 REMPLACER !
  DEPLOY_REPO_NAME: 'repo-a'                # Nom de votre Repo A
  DEPLOY_WORKFLOW_FILE: 'deploy.yml'        # Nom du workflow dans Repo A
```

**Exemple** :
```yaml
env:
  DEPLOY_REPO_OWNER: 'mohamed'
  DEPLOY_REPO_NAME: 'my-app-backend'
  DEPLOY_WORKFLOW_FILE: 'deploy.yml'
```

### \u00c9tape 4 : Adapter le d\u00e9ploiement dans Repo A

Le workflow `deploy.yml` fourni contient un **mock deployment** par d\u00e9faut.

#### Option A : Utiliser le mock (pour tester)

Aucune modification n\u00e9cessaire. Le workflow simule un d\u00e9ploiement et g\u00e9n\u00e8re une URL factice.

#### Option B : Int\u00e9grer votre vrai d\u00e9ploiement

Remplacez la section **Deploy application** (lignes 79-127 de `deploy.yml`) par votre m\u00e9thode de d\u00e9ploiement :

##### Vercel
```yaml
- name: Deploy to Vercel
  id: deploy
  env:
    VERCEL_TOKEN: ${{ secrets.VERCEL_TOKEN }}
    VERCEL_ORG_ID: ${{ secrets.VERCEL_ORG_ID }}
    VERCEL_PROJECT_ID: ${{ secrets.VERCEL_PROJECT_ID }}
  run: |
    npm i -g vercel
    DEPLOY_URL=$(vercel deploy --prod --token=$VERCEL_TOKEN 2>&1 | grep -o 'https://[^ ]*')

    echo "url=${DEPLOY_URL}" >> $GITHUB_OUTPUT

    mkdir -p deployment
    cat > deployment/deployment.json <<EOF
    {
      "baseUrl": "${DEPLOY_URL}",
      "environment": "${{ github.event.inputs.environment }}",
      "deployedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    }
    EOF
```

##### Netlify
```yaml
- name: Deploy to Netlify
  id: deploy
  env:
    NETLIFY_AUTH_TOKEN: ${{ secrets.NETLIFY_TOKEN }}
    NETLIFY_SITE_ID: ${{ secrets.NETLIFY_SITE_ID }}
  run: |
    npm i -g netlify-cli
    netlify deploy --prod --site=$NETLIFY_SITE_ID --auth=$NETLIFY_AUTH_TOKEN
    DEPLOY_URL=$(netlify status --json | jq -r '.site_url')

    echo "url=${DEPLOY_URL}" >> $GITHUB_OUTPUT

    mkdir -p deployment
    cat > deployment/deployment.json <<EOF
    {
      "baseUrl": "${DEPLOY_URL}",
      "environment": "${{ github.event.inputs.environment }}",
      "deployedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    }
    EOF
```

##### AWS S3 + CloudFront
```yaml
- name: Deploy to AWS
  id: deploy
  env:
    AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
    AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
    AWS_REGION: us-east-1
  run: |
    aws s3 sync ./build s3://${{ secrets.S3_BUCKET }} --delete
    aws cloudfront create-invalidation --distribution-id ${{ secrets.CF_DIST_ID }} --paths "/*"

    DEPLOY_URL="https://${{ secrets.CF_DOMAIN }}"
    echo "url=${DEPLOY_URL}" >> $GITHUB_OUTPUT

    mkdir -p deployment
    cat > deployment/deployment.json <<EOF
    {
      "baseUrl": "${DEPLOY_URL}",
      "environment": "${{ github.event.inputs.environment }}",
      "deployedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    }
    EOF
```

**Important** : Conservez toujours la g\u00e9n\u00e9ration de `deployment.json` avec le champ `baseUrl`.

### \u00c9tape 5 : Installer Cypress dans Repo B

```bash
cd repo-b

# Initialiser npm si n\u00e9cessaire
npm init -y

# Installer Cypress
npm install --save-dev cypress

# Initialiser Cypress (cr\u00e9e cypress/, cypress.config.js)
npx cypress open
```

Fermez la fen\u00eatre Cypress apr\u00e8s l'initialisation.

### \u00c9tape 6 : V\u00e9rifier les permissions GitHub Actions

#### Repo A (Application)
Allez dans **Settings > Actions > General > Workflow permissions** :
- ✅ **Read and write permissions**
- ✅ **Allow GitHub Actions to create and approve pull requests**

#### Repo B (Tests)
Allez dans **Settings > Actions > General > Workflow permissions** :
- ✅ **Read and write permissions** (pour commenter les PRs)

## Utilisation

### Lancement automatique (Push/PR)

Le workflow Cypress se d\u00e9clenche automatiquement sur :
- Push vers `main` ou `develop`
- Pull Request vers `main` ou `develop`

Comportement :
- D\u00e9ploie automatiquement vers `staging`
- Utilise la m\u00eame branche que Repo B (synchronisation automatique)
- Ex\u00e9cute les tests Cypress

### Lancement manuel (workflow_dispatch)

#### Via GitHub UI

1. Allez dans **Repo B > Actions > Cypress E2E Tests**
2. Cliquez sur **Run workflow**
3. Remplissez les param\u00e8tres :
   - **Target environment** : `staging` ou `preprod`
   - **Ref to deploy in Repo A** : branche/tag/SHA (ex: `main`, `v1.0.0`, `abc1234`)
   - **Skip deployment** : `false` (d\u00e9ployer) ou `true` (utiliser URL existante)
   - **Base URL** : si `skip_deployment=true`, fournir l'URL (ex: `https://staging.example.com`)
4. Cliquez sur **Run workflow**

#### Via GitHub CLI

```bash
# D\u00e9ployer staging et tester
gh workflow run cypress-e2e.yml \
  -f environment=staging \
  -f deploy_ref=main \
  -f skip_deployment=false

# Tester contre une URL existante (sans d\u00e9ployer)
gh workflow run cypress-e2e.yml \
  -f skip_deployment=true \
  -f base_url=https://staging-app-123.example.com

# D\u00e9ployer preprod depuis une branche sp\u00e9cifique
gh workflow run cypress-e2e.yml \
  -f environment=preprod \
  -f deploy_ref=feature/new-ui \
  -f skip_deployment=false
```

## Sc\u00e9narios d'usage

### Sc\u00e9nario 1 : Tests automatiques sur PR

```yaml
# Repo B - Pull Request sur develop
1. D\u00e9veloppeur cr\u00e9e PR: feature/login -> develop
2. Workflow Cypress se d\u00e9clenche automatiquement
3. D\u00e9clenche d\u00e9ploiement Repo A vers staging (branche: feature/login)
4. Attend compl\u00e9tion du d\u00e9ploiement
5. Ex\u00e9cute tests Cypress contre l'URL d\u00e9ploy\u00e9e
6. Commente le r\u00e9sultat sur la PR
```

### Sc\u00e9nario 2 : Tests manuels sur preprod

```yaml
1. QA/DevOps lance workflow manuellement
2. S\u00e9lectionne environment=preprod, deploy_ref=main
3. D\u00e9ploie Repo A vers preprod
4. Ex\u00e9cute suite compl\u00e8te de tests
5. R\u00e9sultats disponibles dans Actions
```

### Sc\u00e9nario 3 : Tests de non-r\u00e9gression

```yaml
1. D\u00e9ploiement d\u00e9j\u00e0 en place (ex: production)
2. Lance workflow avec skip_deployment=true
3. Fournit base_url=https://app.example.com
4. Ex\u00e9cute tests sans red\u00e9ployer
5. V\u00e9rifie que tout fonctionne toujours
```

## Synchronisation des branches

Le workflow g\u00e8re automatiquement la synchronisation entre Repo A et Repo B :

```yaml
# Dans trigger-deployment job (cypress-e2e.yml)
if [ -z "$DEPLOY_REF" ]; then
  DEPLOY_REF="${{ github.ref_name }}"  # Utilise la m\u00eame branche que Repo B
fi
```

**Exemples** :

| Repo B (Tests) | Repo A (App) | Comportement |
|----------------|--------------|--------------|
| PR sur `feature/login` | D\u00e9ploie `feature/login` | Teste la m\u00eame feature |
| Push sur `main` | D\u00e9ploie `main` | Teste main |
| Manuel : `deploy_ref=v1.0.0` | D\u00e9ploie `v1.0.0` | Teste version sp\u00e9cifique |

**Cas d'usage** : Si Repo A et Repo B ont des branches miroir (m\u00eame nom), les tests s'ex\u00e9cutent automatiquement sur la bonne version.

## Multi-environnement

Le workflow supporte 3 environnements :

| Environnement | URL exemple | Usage |
|---------------|-------------|-------|
| `staging` | `https://staging-app-123.example.com` | Tests automatiques (PR/push) |
| `preprod` | `https://preprod-app-123.example.com` | Validation QA avant prod |
| `production` | `https://app.example.com` | Tests de non-r\u00e9gression |

Modifier les URLs dans `repo-a/.github/workflows/deploy.yml` :

```yaml
case "$ENVIRONMENT" in
  staging)
    DEPLOY_URL="https://staging-app-${{ github.run_number }}.example.com"
    ;;
  preprod)
    DEPLOY_URL="https://preprod-app-${{ github.run_number }}.example.com"
    ;;
  production)
    DEPLOY_URL="https://app.example.com"
    ;;
esac
```

## Concurrency Control

Les deux workflows g\u00e8rent la concurrence pour \u00e9viter les conflits :

### Repo A (deploy.yml)
```yaml
concurrency:
  group: deploy-${{ github.event.inputs.environment }}
  cancel-in-progress: true
```
Annule les d\u00e9ploiements pr\u00e9c\u00e9dents pour le **m\u00eame environnement**.

### Repo B (cypress-e2e.yml)
```yaml
concurrency:
  group: cypress-${{ github.ref }}
  cancel-in-progress: true
```
Annule les tests pr\u00e9c\u00e9dents pour la **m\u00eame branche**.

**Exemple** :
- Push 1 sur `develop` → D\u00e9ploiement staging + Cypress d\u00e9marrent
- Push 2 sur `develop` (30s apr\u00e8s) → Annule Push 1, d\u00e9marre Push 2

## Artifacts

### Repo A produit

| Artifact | Contenu | Retention |
|----------|---------|-----------|
| `deployment-info-{run_id}` | `deployment.json` avec baseUrl, environment, metadata | 7 jours |

### Repo B produit

| Artifact | Contenu | Retention |
|----------|---------|-----------|
| `cypress-screenshots-{container}` | Screenshots des tests \u00e9chou\u00e9s | 7 jours |
| `cypress-videos-{container}` | Vid\u00e9os de tous les tests | 7 jours |
| `cypress-results-{container}` | R\u00e9sultats JSON/XML | 30 jours |

## Troubleshooting

### Erreur : "Failed to trigger deployment"

**Cause** : Token PAT invalide ou permissions insuffisantes

**Solution** :
1. V\u00e9rifiez que `DEPLOY_REPO_PAT` est bien configur\u00e9 dans Repo B
2. V\u00e9rifiez que le token a les permissions `repo` et `actions:write`
3. V\u00e9rifiez que le token n'est pas expir\u00e9
4. Testez manuellement :
```bash
export GH_TOKEN=your_token
gh api /repos/OWNER/REPO/actions/workflows
```

### Erreur : "Could not find triggered workflow run"

**Cause** : Le workflow dans Repo A met du temps \u00e0 d\u00e9marrer, ou n'existe pas

**Solution** :
1. V\u00e9rifiez que `deploy.yml` existe dans Repo A
2. Augmentez le d\u00e9lai d'attente (ligne 117 de cypress-e2e.yml) :
```yaml
sleep 10  # Au lieu de 5
```
3. V\u00e9rifiez que le workflow est activ\u00e9 dans Repo A (Actions > Enable workflows)

### Erreur : "Deployment artifact not found"

**Cause** : Repo A n'a pas upload\u00e9 l'artifact, ou mauvais nom

**Solution** :
1. V\u00e9rifiez les logs du workflow Repo A
2. V\u00e9rifiez que le step "Upload deployment artifact" s'ex\u00e9cute
3. V\u00e9rifiez le nom de l'artifact :
```yaml
name: deployment-info-${{ github.run_id }}
```

### Erreur : "Timeout waiting for deployment"

**Cause** : Le d\u00e9ploiement prend plus de 30 minutes

**Solution** :
1. Augmentez `MAX_WAIT_TIME` (ligne 165 de cypress-e2e.yml) :
```yaml
MAX_WAIT_TIME=3600  # 1 heure
```
2. V\u00e9rifiez les logs du d\u00e9ploiement Repo A pour voir o\u00f9 il bloque

### Erreur : Cypress tests fail with "baseUrl is empty"

**Cause** : L'artifact ne contient pas de baseUrl valide

**Solution** :
1. V\u00e9rifiez le contenu de `deployment.json` dans les artifacts Repo A
2. V\u00e9rifiez que le step "Deploy application" d\u00e9finit bien `DEPLOY_URL`
3. T\u00e9l\u00e9chargez l'artifact manuellement et inspectez-le :
```bash
gh run download RUN_ID -n deployment-info-RUN_ID
cat deployment.json
```

### Tests Cypress \u00e9chouent syst\u00e9matiquement

**D\u00e9buggage** :

1. **T\u00e9l\u00e9chargez les artifacts** (screenshots + videos)
2. **V\u00e9rifiez l'URL** dans les logs :
```
::notice::Testing against: https://staging-app-123.example.com
```
3. **Testez l'URL manuellement** :
```bash
curl -I https://staging-app-123.example.com
```
4. **Ex\u00e9cutez Cypress localement** :
```bash
cd repo-b
CYPRESS_BASE_URL=https://staging-app-123.example.com npx cypress run
```

### GitHub API rate limit

**Erreur** : `API rate limit exceeded`

**Solution** :
- Le PAT token augmente la limite \u00e0 5000 req/h
- Si vous atteignez la limite, r\u00e9duisez `POLL_INTERVAL` (ligne 166) :
```yaml
POLL_INTERVAL=30  # Au lieu de 15
```

## S\u00e9curit\u00e9

### Secrets

- ❌ **NE JAMAIS** committer le PAT token dans le code
- ✅ Toujours utiliser GitHub Secrets
- ✅ D\u00e9finir une expiration sur les tokens (90 jours recommand\u00e9)
- ✅ Utiliser des tokens avec permissions minimales (scope `repo` uniquement)

### Permissions

Les workflows utilisent des permissions minimales :

```yaml
permissions:
  contents: read        # Lire le code
  deployments: write    # Cr\u00e9er des deployments GitHub
  actions: write        # D\u00e9clencher workflows
```

### Logs

Les workflows masquent automatiquement les secrets dans les logs. V\u00e9rifiez que :
- Le token n'appara\u00eet jamais dans les outputs
- L'URL de d\u00e9ploiement peut contenir des tokens (\u00e0 masquer si n\u00e9cessaire)

## Optimisations

### Parall\u00e9lisation Cypress

Augmentez le nombre de containers pour acc\u00e9l\u00e9rer les tests :

```yaml
strategy:
  matrix:
    containers: [1, 2, 3, 4]  # 4 workers au lieu de 2
```

**Attention** : Consomme plus de minutes GitHub Actions.

### Cache npm

Les workflows utilisent d\u00e9j\u00e0 le cache npm :

```yaml
- uses: actions/setup-node@v4
  with:
    cache: 'npm'  # Cache node_modules
```

### Skip deployment

Pour tester rapidement sans red\u00e9ployer :

```bash
gh workflow run cypress-e2e.yml \
  -f skip_deployment=true \
  -f base_url=https://existing-deployment.com
```

## Ressources

### Documentation officielle

- [GitHub Actions - workflow_dispatch](https://docs.github.com/en/actions/using-workflows/events-that-trigger-workflows#workflow_dispatch)
- [GitHub REST API - Actions](https://docs.github.com/en/rest/actions)
- [Cypress GitHub Action](https://github.com/cypress-io/github-action)
- [GitHub CLI - gh workflow](https://cli.github.com/manual/gh_workflow)

### Exemples avanc\u00e9s

- **Cypress Dashboard** : Enregistrer les tests sur Cypress Cloud
  ```yaml
  - uses: cypress-io/github-action@v6
    with:
      record: true
    env:
      CYPRESS_RECORD_KEY: ${{ secrets.CYPRESS_RECORD_KEY }}
  ```

- **Notifications Slack** : Alertes sur \u00e9chec de tests
  ```yaml
  - name: Slack notification
    if: failure()
    uses: slackapi/slack-github-action@v1
    with:
      payload: |
        {
          "text": "Cypress tests failed on ${{ github.ref }}"
        }
    env:
      SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
  ```

## Checklist de validation finale

- [ ] PAT token cr\u00e9\u00e9 avec permissions `repo` + `actions:write`
- [ ] Secret `DEPLOY_REPO_PAT` configur\u00e9 dans Repo B
- [ ] Variables `DEPLOY_REPO_OWNER` et `DEPLOY_REPO_NAME` modifi\u00e9es dans cypress-e2e.yml
- [ ] Workflow `deploy.yml` copi\u00e9 dans Repo A (.github/workflows/)
- [ ] Workflow `cypress-e2e.yml` copi\u00e9 dans Repo B (.github/workflows/)
- [ ] D\u00e9ploiement Repo A configur\u00e9 (mock ou vrai)
- [ ] Cypress install\u00e9 dans Repo B (`npm install cypress`)
- [ ] Tests Cypress cr\u00e9\u00e9s dans `repo-b/cypress/e2e/`
- [ ] Permissions GitHub Actions activ\u00e9es (read+write)
- [ ] Test manuel : d\u00e9clencher workflow via UI et v\u00e9rifier les logs
- [ ] Test automatique : cr\u00e9er PR et v\u00e9rifier d\u00e9clenchement

## Support

Pour toute question ou probl\u00e8me :

1. **V\u00e9rifiez les logs** dans Actions > Workflow run > Job logs
2. **T\u00e9l\u00e9chargez les artifacts** pour inspecter deployment.json
3. **Testez localement** : `npx cypress run --config baseUrl=...`
4. **Consultez** la section Troubleshooting ci-dessus

---

**F\u00e9licitations !** Vous avez maintenant un syst\u00e8me complet de tests E2E cross-repo avec orchestration automatique. 🚀
