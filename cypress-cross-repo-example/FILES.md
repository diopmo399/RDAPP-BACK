# Liste des fichiers - Cypress Cross-Repo E2E Testing

## Structure compl\u00e8te du projet

```
cypress-cross-repo-example/
├── README.md                           # Documentation principale
├── QUICKSTART.md                       # Guide de d\u00e9marrage rapide (10 min)
├── ARCHITECTURE.md                     # Architecture technique d\u00e9taill\u00e9e
├── FILES.md                            # Ce fichier
│
├── repo-a/                             # Repository A (Application)
│   ├── .github/
│   │   └── workflows/
│   │       └── deploy.yml              # Workflow de d\u00e9ploiement
│   ├── .gitignore                      # Fichiers \u00e0 ignorer (Git)
│   ├── package.json                    # D\u00e9pendances npm (exemple)
│   └── index.js                        # Application exemple (Express)
│
└── repo-b/                             # Repository B (Tests)
    ├── .github/
    │   └── workflows/
    │       └── cypress-e2e.yml         # Workflow Cypress E2E
    ├── cypress/
    │   ├── e2e/                        # Tests Cypress
    │   │   ├── 01-health-check.cy.js   # Tests de sant\u00e9
    │   │   ├── 02-navigation.cy.js     # Tests de navigation
    │   │   ├── 03-authentication.cy.js # Tests d'authentification
    │   │   └── 04-api.cy.js            # Tests d'API
    │   ├── fixtures/                   # Donn\u00e9es de test
    │   │   └── users.json              # Utilisateurs de test
    │   └── support/                    # Code support Cypress
    │       ├── commands.js             # Commandes custom
    │       └── e2e.js                  # Configuration globale
    ├── .gitignore                      # Fichiers \u00e0 ignorer (Git)
    ├── cypress.config.js               # Configuration Cypress
    └── package.json                    # D\u00e9pendances npm
```

## Description d\u00e9taill\u00e9e des fichiers

### Documentation

#### README.md (Documentation principale)
- **Taille** : ~18 KB
- **Contenu** :
  - Vue d'ensemble et architecture
  - Guide de setup complet (7 \u00e9tapes)
  - Configuration des secrets GitHub
  - Utilisation (automatique et manuelle)
  - Sc\u00e9narios d'usage
  - Synchronisation des branches
  - Multi-environnement (staging, preprod, production)
  - Concurrency control
  - Troubleshooting d\u00e9taill\u00e9
  - Optimisations et ressources
  - Checklist de validation

#### QUICKSTART.md (D\u00e9marrage rapide)
- **Taille** : ~8 KB
- **Contenu** :
  - Setup en 10 minutes (7 \u00e9tapes)
  - Commandes pr\u00eat-\u00e0-copier
  - Probl\u00e8mes courants et solutions
  - Prochaines \u00e9tapes recommand\u00e9es
  - Checklist de validation rapide

#### ARCHITECTURE.md (Documentation technique)
- **Taille** : ~15 KB
- **Contenu** :
  - Architecture globale avec diagrammes
  - Flux de donn\u00e9es d\u00e9taill\u00e9 (4 phases)
  - D\u00e9tails techniques (auth, communication, polling)
  - Sch\u00e9ma de s\u00e9quence
  - Performance et co\u00fbts
  - Mod\u00e8le de s\u00e9curit\u00e9
  - Extensions et customisations
  - Limitations et alternatives

#### FILES.md (Ce fichier)
- **Taille** : ~5 KB
- **Contenu** :
  - Liste de tous les fichiers avec descriptions
  - Structure compl\u00e8te du projet
  - Utilit\u00e9 de chaque fichier

### Repo A (Application)

#### .github/workflows/deploy.yml
- **Taille** : ~7 KB
- **Fonctionnalit\u00e9s** :
  - Workflow `workflow_dispatch` avec inputs
  - Supporte 3 environnements (staging, preprod, production)
  - Mock deployment (configurable pour Vercel/Netlify/AWS/Firebase)
  - G\u00e9n\u00e9ration de `deployment.json` avec baseUrl
  - Upload artifact pour communication cross-repo
  - Health check avec polling
  - GitHub Deployment integration
  - Deployment summary
  - Concurrency control (annule runs pr\u00e9c\u00e9dents par environnement)

#### package.json
- **Taille** : ~400 bytes
- **Contenu** :
  - Configuration npm basique
  - Scripts : dev, build, start, test
  - D\u00e9pendance : express (serveur web)

#### index.js
- **Taille** : ~2 KB
- **Contenu** :
  - Serveur Express simple
  - Endpoints :
    - `/` : Page d'accueil HTML
    - `/api/health` : Health check JSON
    - `/api/version` : Version de l'API
    - `/api/items` : Exemple de donn\u00e9es
  - Handler 404
  - Configuration port (env var)

#### .gitignore
- **Taille** : ~300 bytes
- **Contenu** :
  - node_modules/
  - dist/, build/
  - .env*
  - Logs, OS files, IDE files

### Repo B (Tests)

#### .github/workflows/cypress-e2e.yml
- **Taille** : ~12 KB
- **Fonctionnalit\u00e9s** :
  - 3 jobs orchestr\u00e9s : trigger-deployment, wait-for-deployment, cypress-tests
  - D\u00e9clenchement cross-repo via GitHub API
  - Polling pour attendre compl\u00e9tion (max 30 min)
  - T\u00e9l\u00e9chargement d'artifact pour r\u00e9cup\u00e9rer baseUrl
  - Ex\u00e9cution Cypress avec parall\u00e9lisation (2 containers)
  - Upload artifacts (screenshots, videos, results)
  - G\u00e9n\u00e9ration de rapport
  - Commentaire PR automatique
  - Mode `skip_deployment` pour tester contre URL existante
  - Concurrency control (annule runs pr\u00e9c\u00e9dents par branche)

#### cypress.config.js
- **Taille** : ~1.5 KB
- **Configuration** :
  - baseUrl override par workflow
  - Timeouts configur\u00e9s
  - Viewport : 1280x720
  - Video & screenshots activ\u00e9s
  - Reporter : cypress-multi-reporters (spec + json)
  - Retries : 2 en CI, 0 en local
  - Test isolation activ\u00e9
  - Variables d'environnement custom (apiUrl, testUser, feature flags)

#### cypress/e2e/01-health-check.cy.js
- **Taille** : ~2 KB
- **Tests** :
  - Load homepage
  - V\u00e9rification base URL
  - Status 200
  - API health endpoint
  - Console errors
  - Static assets (CSS, JS, images)
  - Viewport
  - Responsive mobile

#### cypress/e2e/02-navigation.cy.js
- **Taille** : ~1.5 KB
- **Tests** :
  - Navigation entre pages
  - Gestion 404
  - Query parameters
  - Redirects
  - Breadcrumbs
  - Navigation accessible
  - Active navigation item

#### cypress/e2e/03-authentication.cy.js
- **Taille** : ~4 KB
- **Tests** :
  - Formulaire de login
  - Validation des erreurs
  - Credentials invalides
  - Login valide
  - Toggle password visibility
  - Session persistence
  - Protected pages redirect
  - Logout
  - Password reset
  - API authentication

#### cypress/e2e/04-api.cy.js
- **Taille** : ~4.5 KB
- **Tests** :
  - API health & version
  - GET/POST requests
  - Query parameters
  - Pagination
  - Gestion erreurs (404, 400, 422)
  - Rate limiting
  - Authentication required
  - Security headers
  - Performance (response time)
  - Concurrent requests
  - Data validation
  - Input sanitization (XSS protection)

#### cypress/support/commands.js
- **Taille** : ~3.5 KB
- **Commandes custom** :
  - `cy.login()` : Login UI avec session
  - `cy.apiLogin()` : Login via API (plus rapide)
  - `cy.logout()` : D\u00e9connexion
  - `cy.checkApiHealth()` : V\u00e9rification sant\u00e9 API
  - `cy.waitForApi()` : Attendre r\u00e9ponse API
  - `cy.dataCy()` : S\u00e9lecteur data-cy
  - `cy.dataTestId()` : S\u00e9lecteur data-testid
  - `cy.fillForm()` : Remplir formulaire
  - `cy.checkA11y()` : V\u00e9rification accessibilit\u00e9
  - `cy.screenshotWithTimestamp()` : Screenshot dat\u00e9
  - `cy.waitForPageLoad()` : Attendre chargement complet
  - `cy.seedDatabase()` : Seed DB via API
  - `cy.cleanDatabase()` : Nettoyer DB via API

#### cypress/support/e2e.js
- **Taille** : ~1 KB
- **Configuration globale** :
  - Import des commandes custom
  - Gestion des exceptions non captur\u00e9es
  - beforeEach : Configuration viewport, cookies
  - afterEach : Screenshot sur \u00e9chec
  - Handler d'\u00e9chec global avec logging

#### cypress/fixtures/users.json
- **Taille** : ~600 bytes
- **Donn\u00e9es** :
  - testUser : Utilisateur de test basique
  - adminUser : Utilisateur admin
  - users : Liste de 3 utilisateurs exemple

#### package.json
- **Taille** : ~800 bytes
- **Scripts** :
  - `cy:open` : Ouvrir Cypress UI
  - `cy:run` : Ex\u00e9cuter tests headless
  - `cy:run:chrome` : Tests avec Chrome
  - `cy:run:firefox` : Tests avec Firefox
  - `test` : Alias pour cy:run
  - `test:staging` : Tests contre staging
  - `test:preprod` : Tests contre preprod
- **D\u00e9pendances** :
  - cypress : 13.6.2
  - cypress-multi-reporters : Rapports multiples
  - mocha, mochawesome : Rapports HTML

#### .gitignore
- **Taille** : ~400 bytes
- **Contenu** :
  - node_modules/
  - cypress/videos/, cypress/screenshots/
  - cypress/results/, cypress/reports/
  - .env, cypress.env.json
  - Logs, OS files, IDE files

## Utilisation des fichiers

### Setup initial

1. **Lire QUICKSTART.md** (10 minutes)
   - Guide pas-\u00e0-pas pour mise en place rapide
   - Configuration des secrets et permissions

2. **Copier les workflows**
   - `repo-a/.github/workflows/deploy.yml` → Votre Repo A
   - `repo-b/.github/workflows/cypress-e2e.yml` → Votre Repo B

3. **Copier la configuration Cypress**
   - `repo-b/cypress.config.js` → Votre Repo B
   - `repo-b/cypress/` → Votre Repo B
   - `repo-b/package.json` → Votre Repo B (merger avec existant si n\u00e9cessaire)

### Customisation

1. **Adapter le d\u00e9ploiement** (Repo A)
   - Modifier `deploy.yml` step "Deploy application"
   - Remplacer le mock par votre vrai d\u00e9ploiement (Vercel, Netlify, AWS, etc.)
   - Conserver la g\u00e9n\u00e9ration de `deployment.json`

2. **\u00c9crire vos tests** (Repo B)
   - Modifier les tests dans `cypress/e2e/*.cy.js`
   - Adapter les s\u00e9lecteurs \u00e0 votre application
   - Ajouter vos propres tests

3. **Configurer les environnements**
   - Modifier les URLs dans `deploy.yml` (case statement)
   - Ajouter des environnements si n\u00e9cessaire

### Ex\u00e9cution

1. **Lancement manuel**
   - GitHub UI : Actions > Cypress E2E Tests > Run workflow
   - GitHub CLI : `gh workflow run cypress-e2e.yml -f environment=staging`

2. **Lancement automatique**
   - Push vers `main` ou `develop`
   - Cr\u00e9ation de Pull Request

3. **Tests locaux**
   - `npx cypress open` : Mode interactif
   - `npx cypress run` : Mode headless

### Troubleshooting

1. **Consulter README.md** section "Troubleshooting"
   - 8 erreurs courantes avec solutions d\u00e9taill\u00e9es

2. **Consulter ARCHITECTURE.md** pour comprendre le fonctionnement interne
   - Flux de donn\u00e9es
   - Sch\u00e9ma de s\u00e9quence
   - Gestion des erreurs

3. **V\u00e9rifier les logs GitHub Actions**
   - Actions > Workflow run > Job logs
   - T\u00e9l\u00e9charger les artifacts

## Checklist avant utilisation

### Configuration requise

- [ ] Token PAT cr\u00e9\u00e9 avec permissions `repo` + `actions:write`
- [ ] Secret `DEPLOY_REPO_PAT` configur\u00e9 dans Repo B
- [ ] Variables `DEPLOY_REPO_OWNER` et `DEPLOY_REPO_NAME` modifi\u00e9es dans `cypress-e2e.yml`
- [ ] Permissions GitHub Actions (read+write) activ\u00e9es dans les 2 repos

### Fichiers \u00e0 copier

- [ ] `repo-a/.github/workflows/deploy.yml` → Votre Repo A
- [ ] `repo-b/.github/workflows/cypress-e2e.yml` → Votre Repo B
- [ ] `repo-b/cypress.config.js` → Votre Repo B
- [ ] `repo-b/cypress/` (dossier complet) → Votre Repo B
- [ ] `repo-b/package.json` → Votre Repo B

### Fichiers \u00e0 personnaliser

- [ ] `deploy.yml` : Remplacer mock deployment par vrai d\u00e9ploiement
- [ ] `cypress-e2e.yml` : Modifier `DEPLOY_REPO_OWNER` et `DEPLOY_REPO_NAME`
- [ ] `cypress/e2e/*.cy.js` : Adapter tests \u00e0 votre application

### Tests de validation

- [ ] Test manuel : D\u00e9clencher workflow via GitHub UI
- [ ] V\u00e9rifier : Workflow Repo A ex\u00e9cut\u00e9
- [ ] V\u00e9rifier : Artifact `deployment-info-xxx` cr\u00e9\u00e9
- [ ] V\u00e9rifier : Workflow Repo B compl\u00e9t\u00e9
- [ ] V\u00e9rifier : Tests Cypress ex\u00e9cut\u00e9s
- [ ] V\u00e9rifier : Artifacts (videos, screenshots) disponibles

## Support et ressources

- **Documentation compl\u00e8te** : README.md
- **D\u00e9marrage rapide** : QUICKSTART.md
- **Architecture technique** : ARCHITECTURE.md
- **GitHub Actions docs** : https://docs.github.com/en/actions
- **Cypress docs** : https://docs.cypress.io

---

**Projet cr\u00e9\u00e9 le** : 2025-12-30
**Version** : 1.0
**Fichiers totaux** : 18
**Taille totale** : ~80 KB
