# Quickstart - Cypress Cross-Repo E2E Testing

## D\u00e9marrage rapide en 10 minutes

Ce guide vous permet de mettre en place le syst\u00e8me de tests E2E cross-repo en moins de 10 minutes.

## Pr\u00e9requis

- 2 repositories GitHub (Repo A = application, Repo B = tests)
- Acc\u00e8s administrateur aux deux repos
- Git install\u00e9 localement
- Node.js 18+ (pour Repo B)

## \u00c9tape 1 : Cr\u00e9er le Personal Access Token (2 min)

1. Allez sur GitHub : https://github.com/settings/tokens
2. Cliquez sur **Generate new token (classic)**
3. Nom : `CYPRESS_CROSS_REPO_TOKEN`
4. Expiration : 90 jours
5. **Cochez** :
   - `repo` (Full control)
   - `actions:write`
6. **Generate token**
7. **Copiez le token** (ex: `ghp_abc123...`)

## \u00c9tape 2 : Configurer le secret dans Repo B (1 min)

1. Allez dans **Repo B > Settings > Secrets > Actions**
2. Cliquez sur **New repository secret**
3. Name: `DEPLOY_REPO_PAT`
4. Value: Collez le token cr\u00e9\u00e9 \u00e0 l'\u00e9tape 1
5. **Add secret**

## \u00c9tape 3 : Copier les workflows (2 min)

### Dans Repo A (Application)

```bash
cd /path/to/repo-a

# Cr\u00e9er le dossier workflows
mkdir -p .github/workflows

# Copier le workflow deploy.yml
cp /path/to/cypress-cross-repo-example/repo-a/.github/workflows/deploy.yml .github/workflows/

# Commit & push
git add .github/workflows/deploy.yml
git commit -m "Add deployment workflow for cross-repo E2E testing"
git push
```

### Dans Repo B (Tests)

```bash
cd /path/to/repo-b

# Cr\u00e9er le dossier workflows
mkdir -p .github/workflows

# Copier le workflow Cypress
cp /path/to/cypress-cross-repo-example/repo-b/.github/workflows/cypress-e2e.yml .github/workflows/

# Copier la configuration Cypress
cp /path/to/cypress-cross-repo-example/repo-b/cypress.config.js .
cp /path/to/cypress-cross-repo-example/repo-b/package.json .
cp -r /path/to/cypress-cross-repo-example/repo-b/cypress .

# Commit & push
git add .
git commit -m "Add Cypress E2E tests with cross-repo orchestration"
git push
```

## \u00c9tape 4 : Modifier les variables (2 min)

\u00c9ditez `.github/workflows/cypress-e2e.yml` dans Repo B :

```yaml
env:
  DEPLOY_REPO_OWNER: 'VotrePseudoGitHub'  # \u00c0 REMPLACER !
  DEPLOY_REPO_NAME: 'nom-de-repo-a'       # \u00c0 REMPLACER !
  DEPLOY_WORKFLOW_FILE: 'deploy.yml'      # OK par d\u00e9faut
```

**Exemple** :
```yaml
env:
  DEPLOY_REPO_OWNER: 'mohamed'
  DEPLOY_REPO_NAME: 'my-backend-app'
  DEPLOY_WORKFLOW_FILE: 'deploy.yml'
```

Commit et push :
```bash
git add .github/workflows/cypress-e2e.yml
git commit -m "Configure cross-repo variables"
git push
```

## \u00c9tape 5 : Installer Cypress dans Repo B (2 min)

```bash
cd /path/to/repo-b

# Installer les d\u00e9pendances
npm install

# V\u00e9rifier que Cypress est install\u00e9
npx cypress --version
```

## \u00c9tape 6 : Activer les permissions GitHub Actions (1 min)

### Repo A (Application)
1. Allez dans **Settings > Actions > General**
2. Sous **Workflow permissions** :
   - S\u00e9lectionnez **Read and write permissions**
   - Cochez **Allow GitHub Actions to create and approve pull requests**
3. **Save**

### Repo B (Tests)
1. Allez dans **Settings > Actions > General**
2. Sous **Workflow permissions** :
   - S\u00e9lectionnez **Read and write permissions**
3. **Save**

## \u00c9tape 7 : Tester le workflow (2 min)

### Test manuel via GitHub UI

1. Allez dans **Repo B > Actions > Cypress E2E Tests**
2. Cliquez sur **Run workflow**
3. Laissez les valeurs par d\u00e9faut :
   - Environment: `staging`
   - Deploy ref: (vide = utilise la branche actuelle)
   - Skip deployment: `false`
4. Cliquez sur **Run workflow**
5. Attendez que le workflow se termine (2-5 minutes)

### V\u00e9rifier les r\u00e9sultats

1. **Repo A** : V\u00e9rifiez que le workflow "Deploy Application" s'est ex\u00e9cut\u00e9
   - Actions > Deploy Application > V\u00e9rifiez le dernier run
   - V\u00e9rifiez l'artifact `deployment-info-xxx`

2. **Repo B** : V\u00e9rifiez le workflow Cypress
   - Actions > Cypress E2E Tests > V\u00e9rifiez le dernier run
   - V\u00e9rifiez les 3 jobs : trigger-deployment, wait-for-deployment, cypress-tests
   - T\u00e9l\u00e9chargez les artifacts (videos, screenshots)

## Probl\u00e8mes courants

### Erreur : "Failed to trigger deployment"

**Cause** : Token PAT invalide ou manquant

**Solution** :
```bash
# V\u00e9rifiez que le secret existe dans Repo B
# Settings > Secrets > Actions > DEPLOY_REPO_PAT

# Si manquant, recr\u00e9ez-le (voir \u00c9tape 1 et 2)
```

### Erreur : "Could not find triggered workflow run"

**Cause** : Le workflow `deploy.yml` n'existe pas dans Repo A ou n'est pas activ\u00e9

**Solution** :
```bash
# V\u00e9rifiez que deploy.yml existe
ls -la /path/to/repo-a/.github/workflows/deploy.yml

# V\u00e9rifiez qu'il est commit\u00e9
git log -- .github/workflows/deploy.yml

# V\u00e9rifiez qu'il est activ\u00e9 dans GitHub Actions
# Repo A > Actions > D\u00e9ploiement Application (devrait \u00eatre visible)
```

### Erreur : "Deployment artifact not found"

**Cause** : Le workflow Repo A n'a pas upload\u00e9 l'artifact

**Solution** :
```bash
# V\u00e9rifiez les logs du workflow Repo A
# Cherchez le step "Upload deployment artifact"
# V\u00e9rifiez qu'il ne retourne pas d'erreur
```

### Erreur : Variables d'environnement incorrectes

**Cause** : `DEPLOY_REPO_OWNER` ou `DEPLOY_REPO_NAME` mal configur\u00e9s

**Solution** :
```bash
# V\u00e9rifiez les valeurs dans cypress-e2e.yml
cat .github/workflows/cypress-e2e.yml | grep DEPLOY_REPO

# Exemple correct :
# DEPLOY_REPO_OWNER: 'mohamed'
# DEPLOY_REPO_NAME: 'my-app-backend'

# PAS :
# DEPLOY_REPO_OWNER: 'YOUR_GITHUB_USERNAME'  # <- \u00c0 REMPLACER !
```

## Prochaines \u00e9tapes

### 1. Adapter le d\u00e9ploiement Repo A

Le workflow fourni contient un **mock deployment** (simulation).

Pour int\u00e9grer votre vrai d\u00e9ploiement, modifiez le step "Deploy application" dans `repo-a/.github/workflows/deploy.yml` :

**Vercel** :
```yaml
- name: Deploy to Vercel
  run: |
    npm i -g vercel
    DEPLOY_URL=$(vercel deploy --prod --token=${{ secrets.VERCEL_TOKEN }})
    echo "url=${DEPLOY_URL}" >> $GITHUB_OUTPUT

    mkdir -p deployment
    echo "{\"baseUrl\": \"${DEPLOY_URL}\"}" > deployment/deployment.json
```

**Netlify** :
```yaml
- name: Deploy to Netlify
  run: |
    npm i -g netlify-cli
    netlify deploy --prod --site=${{ secrets.NETLIFY_SITE_ID }}
    DEPLOY_URL=$(netlify status --json | jq -r '.site_url')
    echo "url=${DEPLOY_URL}" >> $GITHUB_OUTPUT

    mkdir -p deployment
    echo "{\"baseUrl\": \"${DEPLOY_URL}\"}" > deployment/deployment.json
```

**Important** : Conservez toujours la g\u00e9n\u00e9ration de `deployment/deployment.json` avec le champ `baseUrl`.

### 2. \u00c9crire vos tests Cypress

Les tests d'exemple sont dans `cypress/e2e/*.cy.js`. Adaptez-les \u00e0 votre application :

```bash
cd /path/to/repo-b

# Ouvrir Cypress en mode interactif
npx cypress open

# Modifier les tests dans cypress/e2e/
# Ajouter vos propres tests
```

### 3. Tester localement

```bash
# Tester contre votre environnement de dev local
CYPRESS_BASE_URL=http://localhost:3000 npx cypress run

# Tester contre staging
CYPRESS_BASE_URL=https://staging.example.com npx cypress run

# Tester un test sp\u00e9cifique
npx cypress run --spec "cypress/e2e/01-health-check.cy.js"
```

### 4. Activer les tests automatiques sur PR

Le workflow Cypress se d\u00e9clenche automatiquement sur les Pull Requests vers `main` ou `develop`.

Cr\u00e9ez une PR pour tester :

```bash
git checkout -b feature/test-automation
git commit --allow-empty -m "Test automatic E2E workflow"
git push origin feature/test-automation

# Cr\u00e9ez une PR sur GitHub
# Le workflow devrait se d\u00e9clencher automatiquement
```

## Commandes utiles

### GitHub CLI

```bash
# Installer GitHub CLI
brew install gh  # macOS
# ou https://cli.github.com/

# Se connecter
gh auth login

# D\u00e9clencher le workflow Cypress manuellement
gh workflow run cypress-e2e.yml \
  -f environment=staging \
  -f deploy_ref=main

# Voir le status des workflows
gh run list --workflow=cypress-e2e.yml

# Voir les logs d'un run sp\u00e9cifique
gh run view RUN_ID --log
```

### Cypress

```bash
# Ouvrir Cypress en mode interactif
npx cypress open

# Ex\u00e9cuter tous les tests
npx cypress run

# Ex\u00e9cuter avec Chrome
npx cypress run --browser chrome

# Ex\u00e9cuter un test sp\u00e9cifique
npx cypress run --spec "cypress/e2e/01-health-check.cy.js"

# Ex\u00e9cuter avec variables d'environnement custom
CYPRESS_BASE_URL=https://staging.example.com npx cypress run

# Mode headed (voir le navigateur)
npx cypress run --headed

# G\u00e9n\u00e9rer le rapport
npx cypress run --reporter mochawesome
```

## Checklist de validation

- [ ] Token PAT cr\u00e9\u00e9
- [ ] Secret `DEPLOY_REPO_PAT` configur\u00e9 dans Repo B
- [ ] Workflow `deploy.yml` dans Repo A (commit\u00e9 et push\u00e9)
- [ ] Workflow `cypress-e2e.yml` dans Repo B (commit\u00e9 et push\u00e9)
- [ ] Variables `DEPLOY_REPO_OWNER` et `DEPLOY_REPO_NAME` modifi\u00e9es
- [ ] Cypress install\u00e9 dans Repo B (`npm install` ex\u00e9cut\u00e9)
- [ ] Permissions GitHub Actions activ\u00e9es (read+write)
- [ ] Test manuel r\u00e9ussi (workflow ex\u00e9cut\u00e9 sans erreur)
- [ ] Artifacts g\u00e9n\u00e9r\u00e9s dans les deux repos
- [ ] Tests Cypress ex\u00e9cut\u00e9s avec succ\u00e8s

## Support

- **Documentation compl\u00e8te** : Consultez `README.md` pour plus de d\u00e9tails
- **Troubleshooting** : Section "Troubleshooting" dans README.md
- **Logs** : Actions > Workflow run > Chaque job a des logs d\u00e9taill\u00e9s
- **Artifacts** : T\u00e9l\u00e9chargez les artifacts pour inspecter deployment.json, screenshots, videos

---

**F\u00e9licitations !** Votre syst\u00e8me de tests E2E cross-repo est op\u00e9rationnel. 🎉

**Prochaine \u00e9tape recommand\u00e9e** : Int\u00e9grer votre vrai d\u00e9ploiement (Vercel/Netlify/AWS) dans `repo-a/.github/workflows/deploy.yml`.
