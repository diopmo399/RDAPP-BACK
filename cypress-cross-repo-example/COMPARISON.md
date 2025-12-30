# Comparaison : Mock Deployment vs Cloud Foundry

## Vue d'ensemble

Ce projet fournit **2 approches** pour l'orchestration de tests E2E cross-repo :

1. **Mock Deployment** : Simulation de déploiement (pour tester le workflow)
2. **Cloud Foundry** : Déploiement réel sur Cloud Foundry

---

## Tableau comparatif

| Critère | Mock Deployment | Cloud Foundry |
|---------|----------------|---------------|
| **Workflow Repo A** | `deploy.yml` | `deploy-cloudfoundry.yml` |
| **Workflow Repo B** | `cypress-e2e.yml` | `cypress-e2e-cloudfoundry.yml` |
| **Déploiement** | Simulé (sleep 3s) | Réel (`cf push`) |
| **URL générée** | Factice (`staging-app-123.example.com`) | Réelle (route Cloud Foundry) |
| **Health check** | Simulé (3 tentatives) | Réel (curl HTTP) |
| **Durée totale** | 2-3 min | 5-10 min |
| **Secrets requis** | Aucun (mock) | 7-8 secrets Cloud Foundry |
| **Setup complexity** | Faible | Moyenne |
| **Use case** | Tests du workflow, CI/CD sans infra | Production, staging réel |

---

## Détails des workflows

### Mock Deployment

#### Repo A : `.github/workflows/deploy.yml`

**Étapes principales** :
1. Checkout code
2. Build application (`npm run build`)
3. **Mock deployment** :
   ```yaml
   sleep 3
   DEPLOY_URL="https://staging-app-123.example.com"
   ```
4. **Mock health check** :
   ```yaml
   if [ $i -ge 3 ]; then
     echo "ready"
   fi
   ```
5. Upload artifact (`deployment.json`)

**Secrets requis** : Aucun

**Durée** : ~30 secondes

**Quand l'utiliser** :
- Tester le workflow sans déploiement réel
- CI/CD sur branches de développement
- Développement local du workflow
- Budget GitHub Actions limité

---

### Cloud Foundry Deployment

#### Repo A : `.github/workflows/deploy-cloudfoundry.yml`

**Étapes principales** :
1. Checkout code
2. Build application (`npm run build`)
3. **Installer CF CLI** :
   ```bash
   sudo apt-get install cf8-cli
   ```
4. **Authentification Cloud Foundry** :
   ```bash
   cf api $CF_API --skip-ssl-validation
   cf auth $CF_USERNAME $CF_PASSWORD
   cf target -o $CF_ORG -s $CF_SPACE
   ```
5. **Déploiement réel** :
   ```bash
   cf push $CF_APP_NAME -f manifest.yml
   ```
6. **Récupération de la route** :
   ```bash
   cf app $CF_APP_NAME | grep routes
   ```
7. **Health check réel** :
   ```bash
   curl https://my-app.cfapps.io
   ```
8. Upload artifact (`deployment.json`)

**Secrets requis** :
- `CF_API`
- `CF_USERNAME`
- `CF_PASSWORD`
- `CF_ORG`
- `CF_SPACE`
- `CF_APP_NAME_STAGING`
- `CF_APP_NAME_PREPROD`
- `CF_SKIP_SSL_VALIDATION` (optionnel)

**Durée** : ~5-10 minutes (selon taille app)

**Quand l'utiliser** :
- Déploiement réel sur staging/preprod
- Tests E2E contre environnement réel
- Validation avant production
- Pipeline de déploiement complet

---

## Différences dans Repo B

### Repo B : Workflows Cypress

Les 2 workflows Cypress sont **quasi-identiques**, seules différences :

| Élément | Mock | Cloud Foundry |
|---------|------|---------------|
| **Nom du workflow** | `cypress-e2e.yml` | `cypress-e2e-cloudfoundry.yml` |
| **Workflow déclenché** | `deploy.yml` | `deploy-cloudfoundry.yml` |
| **Timeout** | 30 min | 30 min (peut être augmenté) |
| **Durée attente** | ~30s | ~5-10 min |

**Code identique** :
- Déclenchement via GitHub API
- Polling pour attendre completion
- Téléchargement d'artifact
- Extraction de `baseUrl`
- Exécution Cypress

---

## Fichiers deployment.json

### Mock Deployment

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

### Cloud Foundry

```json
{
  "baseUrl": "https://my-app-staging.cfapps.io",
  "appName": "my-app-staging",
  "environment": "staging",
  "ref": "main",
  "deployedAt": "2025-12-30T10:30:00Z",
  "runId": "12345678",
  "runNumber": "42",
  "triggeredBy": "owner/repo-b",
  "cfOrg": "my-org",
  "cfSpace": "development"
}
```

**Différences** :
- Cloud Foundry ajoute : `appName`, `cfOrg`, `cfSpace`
- Mock : URL factice
- Cloud Foundry : URL réelle de l'app déployée

---

## Migration de Mock vers Cloud Foundry

### Étape 1 : Ajouter les secrets Cloud Foundry dans Repo A

```bash
# Via GitHub CLI
gh secret set CF_API -b "https://api.cf.example.com" -R owner/repo-a
gh secret set CF_USERNAME -b "user@example.com" -R owner/repo-a
gh secret set CF_PASSWORD -b "mypassword" -R owner/repo-a
gh secret set CF_ORG -b "my-org" -R owner/repo-a
gh secret set CF_SPACE -b "development" -R owner/repo-a
gh secret set CF_APP_NAME_STAGING -b "my-app-staging" -R owner/repo-a
gh secret set CF_APP_NAME_PREPROD -b "my-app-preprod" -R owner/repo-a
```

### Étape 2 : Créer manifest.yml dans Repo A

Copiez le fichier `manifest.yml` fourni et adaptez-le à votre app.

### Étape 3 : Remplacer les workflows

**Repo A** :
```bash
# Backup de l'ancien workflow
mv .github/workflows/deploy.yml .github/workflows/deploy-mock.yml.bak

# Copier le nouveau
cp deploy-cloudfoundry.yml .github/workflows/deploy.yml
```

**Repo B** :
```bash
# Backup
mv .github/workflows/cypress-e2e.yml .github/workflows/cypress-e2e-mock.yml.bak

# Copier
cp cypress-e2e-cloudfoundry.yml .github/workflows/cypress-e2e.yml

# Modifier les variables
vim .github/workflows/cypress-e2e.yml
# DEPLOY_WORKFLOW_FILE: 'deploy.yml'  # Nouveau nom
```

### Étape 4 : Tester

```bash
# Tester le déploiement CF localement
cd repo-a
cf login
cf push my-app-staging -f manifest.yml

# Vérifier
cf app my-app-staging
curl https://my-app-staging.cfapps.io

# Tester le workflow
gh workflow run deploy.yml -f environment=staging -f ref=main
```

---

## Utilisation hybride

Vous pouvez **garder les 2 approches** dans le même repo :

### Repo A : 2 workflows

```
.github/workflows/
├── deploy-mock.yml           # Pour développement
└── deploy-cloudfoundry.yml   # Pour staging/preprod
```

### Repo B : Choisir le workflow à déclencher

```yaml
# cypress-e2e.yml
env:
  # Choisir selon l'environnement
  DEPLOY_WORKFLOW_FILE: |
    ${{ github.event.inputs.environment == 'staging' && 'deploy-cloudfoundry.yml' || 'deploy-mock.yml' }}
```

**OU** utiliser des workflows séparés :

```
.github/workflows/
├── cypress-e2e-dev.yml        # Déclenche deploy-mock.yml
└── cypress-e2e-staging.yml    # Déclenche deploy-cloudfoundry.yml
```

---

## Recommandations

### Développement

- ✅ **Mock Deployment**
- Raison : Rapide, pas de secrets, pas de coût infra
- Branches : `feature/*`, `develop`

### Staging

- ✅ **Cloud Foundry Deployment**
- Raison : Tests contre environnement réel
- Branches : `main`, `staging`
- Trigger : PR vers `main`

### Preprod

- ✅ **Cloud Foundry Deployment**
- Raison : Validation finale avant production
- Branches : `release/*`
- Trigger : Manuel (`workflow_dispatch`)

### Production

- ✅ **Cloud Foundry Deployment** (sans tests Cypress auto)
- Raison : Déploiement contrôlé
- Trigger : Manuel uniquement
- Cypress : Optionnel (tests de non-régression)

---

## Tableau de décision

| Scenario | Mock | Cloud Foundry |
|----------|------|---------------|
| Développer le workflow | ✅ | ❌ |
| Tester localement | ✅ | ❌ |
| CI sur feature branch | ✅ | ❌ |
| CI sur develop | ✅ | ⚠️  |
| CI sur main | ⚠️  | ✅ |
| Validation QA | ❌ | ✅ |
| Tests E2E réels | ❌ | ✅ |
| Déploiement production | ❌ | ✅ |
| Budget limité | ✅ | ❌ |
| Pas d'infra Cloud Foundry | ✅ | ❌ |

**Légende** :
- ✅ Recommandé
- ⚠️  Possible mais pas optimal
- ❌ Non recommandé

---

## Ressources

### Documentation Mock Deployment
- `README.md` : Guide complet
- `QUICKSTART.md` : Setup en 10 minutes
- `ARCHITECTURE.md` : Architecture technique

### Documentation Cloud Foundry
- `SETUP-CLOUD-FOUNDRY.md` : Guide de setup complet
- `manifest.yml` : Exemple de configuration CF
- [Cloud Foundry Docs](https://docs.cloudfoundry.org/)

---

## Conclusion

**Mock Deployment** : Parfait pour développer et tester le workflow sans infrastructure.

**Cloud Foundry** : Nécessaire pour les déploiements réels et tests E2E contre environnements réels.

**Recommandation** : Commencez par Mock pour comprendre le workflow, puis migrez vers Cloud Foundry pour staging/preprod.
