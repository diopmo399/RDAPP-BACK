# Setup Guide - Cypress E2E + Cloud Foundry Cross-Repo

## Vue d'ensemble

Cette solution permet de :
1. **Repo B** déclenche le déploiement de **Repo A** sur Cloud Foundry
2. Attendre la fin du déploiement Cloud Foundry
3. Récupérer l'URL de l'application déployée
4. Exécuter les tests Cypress contre cette URL

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Repo B (Tests Cypress)                    │
│                                                              │
│  1. Trigger deployment (GitHub API)                         │
│     └─> POST /repos/OWNER/REPO-A/actions/workflows/...      │
│                                                              │
│  2. Wait for completion (Polling)                           │
│     └─> GET /repos/OWNER/REPO-A/actions/runs/{run_id}       │
│                                                              │
│  3. Download artifact                                        │
│     └─> GET /repos/.../artifacts/{id}/zip                   │
│     └─> Extract deployment.json → baseUrl                   │
│                                                              │
│  4. Run Cypress tests                                        │
│     └─> npx cypress run --config baseUrl=${BASE_URL}        │
└─────────────────────────────────────────────────────────────┘
                          ↓ GitHub API
┌─────────────────────────────────────────────────────────────┐
│                  Repo A (Application CF)                     │
│                                                              │
│  1. Deploy to Cloud Foundry                                 │
│     ├─> cf api $CF_API                                      │
│     ├─> cf auth $CF_USERNAME $CF_PASSWORD                   │
│     ├─> cf target -o $CF_ORG -s $CF_SPACE                   │
│     └─> cf push $CF_APP_NAME -f manifest.yml               │
│                                                              │
│  2. Get application route                                   │
│     └─> cf app $CF_APP_NAME → extract route                │
│                                                              │
│  3. Health check                                            │
│     └─> curl https://<route> (retry 60x)                   │
│                                                              │
│  4. Upload artifact                                          │
│     └─> deployment.json (baseUrl, appName, env, ...)       │
└─────────────────────────────────────────────────────────────┘
```

---

## Étape 1 : Créer un Personal Access Token (PAT)

Le token permet à Repo B de déclencher des workflows dans Repo A.

### 1.1 Générer le token

1. Allez sur GitHub : https://github.com/settings/tokens
2. Cliquez sur **Generate new token (classic)**
3. Nom : `CYPRESS_CF_CROSS_REPO_TOKEN`
4. Expiration : **90 jours** (ou No expiration)
5. **Permissions minimales requises** :
   - ✅ `repo` (Full control of private repositories)
   - ✅ `actions:write` (Déclencher workflows)
   - ✅ `actions:read` (Lire status des runs)
6. Cliquez sur **Generate token**
7. **Copiez le token** immédiatement (ex: `ghp_abc123...`)

---

## Étape 2 : Configurer les secrets dans Repo A (Application)

Allez dans **Repo A > Settings > Secrets and variables > Actions**

Cliquez sur **New repository secret** pour chaque secret :

| Secret Name | Valeur | Description | Exemple |
|-------------|--------|-------------|---------|
| `CF_API` | URL de l'API Cloud Foundry | API endpoint | `https://api.cf.example.com` |
| `CF_USERNAME` | Nom d'utilisateur CF | Login Cloud Foundry | `user@example.com` |
| `CF_PASSWORD` | Mot de passe CF | Password Cloud Foundry | `mypassword123` |
| `CF_ORG` | Organisation Cloud Foundry | Org name | `my-org` |
| `CF_SPACE` | Space Cloud Foundry | Space name (staging/prod) | `development` |
| `CF_APP_NAME_STAGING` | Nom de l'app staging | Nom dans CF | `my-app-staging` |
| `CF_APP_NAME_PREPROD` | Nom de l'app preprod | Nom dans CF | `my-app-preprod` |
| `CF_SKIP_SSL_VALIDATION` | true/false | Skip SSL validation | `true` (optionnel) |

### 2.1 Récupérer les informations Cloud Foundry

Si vous ne connaissez pas vos informations CF :

```bash
# Se connecter à Cloud Foundry
cf login -a https://api.cf.example.com -u user@example.com

# Voir votre org et space actuel
cf target

# Lister les apps
cf apps

# Voir les détails d'une app
cf app my-app-staging
```

---

## Étape 3 : Configurer les secrets dans Repo B (Tests)

Allez dans **Repo B > Settings > Secrets and variables > Actions**

| Secret Name | Valeur | Description |
|-------------|--------|-------------|
| `DEPLOY_REPO_PAT` | Token créé à l'étape 1 | Token pour déclencher workflows cross-repo |

**Optionnels** (si vous utilisez Cypress Dashboard) :

| Secret Name | Valeur | Description |
|-------------|--------|-------------|
| `CYPRESS_RECORD_KEY` | Clé Cypress Dashboard | Pour enregistrer les tests |

---

## Étape 4 : Modifier les variables dans Repo B

Éditez `.github/workflows/cypress-e2e-cloudfoundry.yml` dans Repo B :

```yaml
env:
  DEPLOY_REPO_OWNER: 'VotrePseudoGitHub'  # ← À REMPLACER !
  DEPLOY_REPO_NAME: 'nom-de-repo-a'       # ← À REMPLACER !
  DEPLOY_WORKFLOW_FILE: 'deploy-cloudfoundry.yml'  # OK par défaut
```

**Exemple** :
```yaml
env:
  DEPLOY_REPO_OWNER: 'mohamed'
  DEPLOY_REPO_NAME: 'my-cf-backend'
  DEPLOY_WORKFLOW_FILE: 'deploy-cloudfoundry.yml'
```

---

## Étape 5 : Créer le manifest.yml Cloud Foundry

Dans **Repo A**, créez un fichier `manifest.yml` à la racine :

```yaml
---
applications:
  - name: ((app-name))  # Sera remplacé par CF_APP_NAME_STAGING ou CF_APP_NAME_PREPROD
    memory: 1G
    instances: 2
    buildpacks:
      - nodejs_buildpack
    path: .
    env:
      NODE_ENV: production
    routes:
      - route: ((app-name)).cfapps.example.com
```

**OU** créez des manifests séparés :

```yaml
# manifest-staging.yml
---
applications:
  - name: my-app-staging
    memory: 512M
    instances: 1
    buildpacks:
      - nodejs_buildpack
    path: .
    env:
      NODE_ENV: staging
    routes:
      - route: my-app-staging.cfapps.example.com
```

```yaml
# manifest-preprod.yml
---
applications:
  - name: my-app-preprod
    memory: 1G
    instances: 2
    buildpacks:
      - nodejs_buildpack
    path: .
    env:
      NODE_ENV: production
    routes:
      - route: my-app-preprod.cfapps.example.com
```

**Si vous utilisez des manifests séparés**, modifiez le workflow `deploy-cloudfoundry.yml` :

```yaml
- name: Deploy application to Cloud Foundry
  run: |
    ENVIRONMENT="${{ github.event.inputs.environment }}"

    case "$ENVIRONMENT" in
      staging)
        cf push -f manifest-staging.yml
        ;;
      preprod)
        cf push -f manifest-preprod.yml
        ;;
    esac
```

---

## Étape 6 : Activer les permissions GitHub Actions

### Repo A (Application)
1. Allez dans **Settings > Actions > General**
2. Sous **Workflow permissions** :
   - ✅ Sélectionnez **Read and write permissions**
3. **Save**

### Repo B (Tests)
1. Allez dans **Settings > Actions > General**
2. Sous **Workflow permissions** :
   - ✅ Sélectionnez **Read and write permissions**
3. **Save**

---

## Étape 7 : Tester le workflow

### Test manuel via GitHub UI

1. Allez dans **Repo B > Actions > Cypress E2E Tests (Cloud Foundry)**
2. Cliquez sur **Run workflow**
3. Paramètres :
   - **Environment** : `staging`
   - **Deploy ref** : (vide = utilise branche actuelle)
   - **Skip deployment** : `false`
4. Cliquez sur **Run workflow**
5. Attendez que le workflow se termine (5-10 minutes)

### Vérifier les résultats

#### Repo A
- Actions > Deploy to Cloud Foundry
- Vérifiez le dernier run
- Vérifiez l'artifact `deployment-info-xxx`
- Téléchargez l'artifact et inspectez `deployment.json`

#### Repo B
- Actions > Cypress E2E Tests (Cloud Foundry)
- Vérifiez les 3 jobs :
  1. `trigger-deployment` ✅
  2. `wait-for-deployment` ✅
  3. `cypress-tests` ✅
- Téléchargez les artifacts (videos, screenshots)

---

## Commandes de debug

### Debug Cloud Foundry (Repo A)

```bash
# Se connecter
cf login -a https://api.cf.example.com -u user@example.com

# Voir les apps déployées
cf apps

# Voir les détails d'une app
cf app my-app-staging

# Voir les logs en temps réel
cf logs my-app-staging --recent
cf logs my-app-staging  # Suivre en temps réel

# Voir les routes
cf routes

# Voir les variables d'environnement
cf env my-app-staging

# Redémarrer une app
cf restart my-app-staging

# Scaler une app
cf scale my-app-staging -i 2 -m 1G

# SSH dans le container
cf ssh my-app-staging
```

### Debug Health Check (Local)

```bash
# Tester l'URL de l'app déployée
curl -I https://my-app-staging.cfapps.example.com

# Avec détails
curl -v https://my-app-staging.cfapps.example.com

# Ignorer SSL (si CF_SKIP_SSL_VALIDATION=true)
curl -k https://my-app-staging.cfapps.example.com

# Suivre les redirects
curl -L https://my-app-staging.cfapps.example.com

# Timeout
curl --max-time 10 https://my-app-staging.cfapps.example.com
```

### Debug GitHub API (Repo B)

```bash
# Installer GitHub CLI
brew install gh  # macOS
# ou https://cli.github.com/

# Se connecter
gh auth login

# Déclencher manuellement le workflow
gh workflow run cypress-e2e-cloudfoundry.yml \
  -f environment=staging \
  -f deploy_ref=main

# Lister les runs du workflow
gh run list --workflow=cypress-e2e-cloudfoundry.yml

# Voir les logs d'un run
gh run view RUN_ID --log

# Télécharger les artifacts
gh run download RUN_ID

# Voir le status en temps réel
gh run watch RUN_ID
```

### Debug Artifact (deployment.json)

```bash
# Télécharger l'artifact
gh run download RUN_ID -n deployment-info-RUN_ID

# Lire le JSON
cat deployment.json | jq .

# Extraire baseUrl
cat deployment.json | jq -r '.baseUrl'

# Tester l'URL
BASE_URL=$(cat deployment.json | jq -r '.baseUrl')
curl -I $BASE_URL
```

---

## Troubleshooting

### Erreur : "Failed to trigger Cloud Foundry deployment"

**Cause** : Token PAT invalide ou permissions insuffisantes

**Solution** :
1. Vérifiez que `DEPLOY_REPO_PAT` est configuré dans Repo B
2. Vérifiez que le token a les permissions `repo` + `actions:write`
3. Testez manuellement :
```bash
export GH_TOKEN=your_token
gh api /repos/OWNER/REPO-A/actions/workflows
```

### Erreur : "Could not find triggered workflow run"

**Cause** : Le workflow met du temps à démarrer

**Solution** :
- Augmentez le délai d'attente dans `trigger-deployment` (ligne ~117) :
```yaml
sleep 15  # Au lieu de 10
```

### Erreur : "Deployment artifact not found"

**Cause** : Le workflow Repo A n'a pas uploadé l'artifact

**Solution** :
1. Vérifiez les logs du workflow Repo A
2. Cherchez le step "Upload deployment artifact"
3. Vérifiez qu'il ne retourne pas d'erreur

### Erreur : "cf push failed"

**Cause** : Problème avec le déploiement Cloud Foundry

**Solution** :
```bash
# Vérifier les logs CF
cf logs my-app-staging --recent

# Vérifier le manifest.yml
cat manifest.yml

# Tester le déploiement localement
cf push my-app-staging -f manifest.yml

# Vérifier les buildpacks disponibles
cf buildpacks
```

### Erreur : "Health check failed"

**Cause** : L'application ne répond pas après déploiement

**Solution** :
1. Vérifiez que l'app est bien déployée :
```bash
cf app my-app-staging
```
2. Vérifiez les logs :
```bash
cf logs my-app-staging --recent
```
3. Testez manuellement :
```bash
curl https://my-app-staging.cfapps.example.com
```
4. Augmentez le nombre de tentatives (ligne ~220 de `deploy-cloudfoundry.yml`) :
```yaml
MAX_ATTEMPTS=120  # Au lieu de 60
```

### Erreur : "Authentication failed" (Cloud Foundry)

**Cause** : Credentials CF incorrects

**Solution** :
1. Vérifiez les secrets :
   - `CF_API`
   - `CF_USERNAME`
   - `CF_PASSWORD`
   - `CF_ORG`
   - `CF_SPACE`
2. Testez localement :
```bash
cf login -a $CF_API -u $CF_USERNAME -p $CF_PASSWORD
cf target -o $CF_ORG -s $CF_SPACE
```

---

## Exemple de déploiement complet

### 1. Déploiement manuel local (pour tester)

```bash
# Se connecter
cf login -a https://api.cf.example.com

# Target org/space
cf target -o my-org -s development

# Build l'application
npm ci
npm run build

# Déployer
cf push my-app-staging -f manifest.yml

# Vérifier
cf app my-app-staging

# Tester
curl https://my-app-staging.cfapps.example.com
```

### 2. Déploiement via GitHub Actions (Repo B)

```bash
# Via GitHub CLI
gh workflow run cypress-e2e-cloudfoundry.yml \
  -f environment=staging \
  -f deploy_ref=main \
  -f skip_deployment=false

# Suivre l'exécution
gh run watch
```

---

## Checklist de validation

### Configuration

- [ ] Token PAT créé avec permissions `repo` + `actions:write`
- [ ] Secret `DEPLOY_REPO_PAT` configuré dans Repo B
- [ ] Secrets Cloud Foundry configurés dans Repo A :
  - [ ] `CF_API`
  - [ ] `CF_USERNAME`
  - [ ] `CF_PASSWORD`
  - [ ] `CF_ORG`
  - [ ] `CF_SPACE`
  - [ ] `CF_APP_NAME_STAGING`
  - [ ] `CF_APP_NAME_PREPROD`
- [ ] Variables `DEPLOY_REPO_OWNER` et `DEPLOY_REPO_NAME` modifiées dans `cypress-e2e-cloudfoundry.yml`
- [ ] Permissions GitHub Actions activées (read+write) dans les 2 repos

### Fichiers

- [ ] Workflow `deploy-cloudfoundry.yml` dans Repo A (`.github/workflows/`)
- [ ] Workflow `cypress-e2e-cloudfoundry.yml` dans Repo B (`.github/workflows/`)
- [ ] Fichier `manifest.yml` dans Repo A (racine)
- [ ] Tests Cypress dans Repo B (`cypress/e2e/`)

### Tests

- [ ] Test manuel : Déclencher workflow via GitHub UI
- [ ] Vérifier : Workflow Repo A exécuté avec succès
- [ ] Vérifier : Application déployée sur Cloud Foundry
- [ ] Vérifier : Artifact `deployment-info-xxx` créé
- [ ] Vérifier : Workflow Repo B complété
- [ ] Vérifier : Tests Cypress exécutés avec succès
- [ ] Vérifier : Artifacts (videos, screenshots) disponibles

---

## Ressources

### Documentation Cloud Foundry

- [CF CLI Reference](https://cli.cloudfoundry.org/en-US/v8/)
- [CF Push Documentation](https://docs.cloudfoundry.org/devguide/deploy-apps/deploy-app.html)
- [CF Manifest](https://docs.cloudfoundry.org/devguide/deploy-apps/manifest.html)
- [CF Routes](https://docs.cloudfoundry.org/devguide/deploy-apps/routes-domains.html)

### Documentation GitHub Actions

- [workflow_dispatch](https://docs.github.com/en/actions/using-workflows/events-that-trigger-workflows#workflow_dispatch)
- [GitHub REST API - Actions](https://docs.github.com/en/rest/actions)
- [Artifacts](https://docs.github.com/en/actions/using-workflows/storing-workflow-data-as-artifacts)

### Documentation Cypress

- [Cypress Configuration](https://docs.cypress.io/guides/references/configuration)
- [cypress-io/github-action](https://github.com/cypress-io/github-action)

---

**Félicitations !** Votre système de tests E2E cross-repo avec Cloud Foundry est opérationnel. 🚀

**Prochaine étape** : Adapter le `manifest.yml` à votre application et tester le premier déploiement.
