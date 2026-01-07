# Flyway Drift Maven Plugin

## 🎯 Vue d'ensemble

Plugin Maven professionnel qui détecte les **drifts de migrations Flyway** entre branches Git et **fail le build** si des incohérences sont détectées.

**Cas d'usage** :
- Empêcher les merges de branches avec migrations manquantes
- Détecter les modifications de migrations existantes (interdit par Flyway)
- Prévenir les migrations dupliquées
- Valider la cohérence des migrations en CI/CD

## ✨ Fonctionnalités

✅ **Détection de drifts automatique** via JGit (pas de commandes shell)
✅ **3 types de drifts détectés** :
   - **Behind** : Migrations présentes en `base` mais manquantes en `target`
   - **Diverged** : Même version, contenu différent (hash)
   - **Duplicates** : Plusieurs fichiers avec la même version

✅ **Fetch automatique** des branches distantes (configurable)
✅ **Compatible CI/CD** (GitHub Actions, GitLab CI, Jenkins)
✅ **Aucune modification du workspace**
✅ **Rapports Markdown** générés dans `target/`
✅ **Auto-détection** de `origin/main` ou `origin/master`
✅ **Support Repeatable migrations** (`R__*.sql`)
✅ **Messages en français** 🇫🇷

## 📦 Installation

### 1. Installer le plugin dans votre repository local

```bash
cd flyway-drift-maven-plugin
mvn clean install
```

### 2. Ajouter le plugin à votre projet

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.example</groupId>
      <artifactId>flyway-drift-maven-plugin</artifactId>
      <version>1.0.0</version>
      <executions>
        <execution>
          <id>check-flyway-drift</id>
          <phase>validate</phase>
          <goals>
            <goal>check</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <!-- Branche de référence (auto-détection si vide) -->
        <baseRef>main</baseRef>

        <!-- Branche à vérifier (HEAD par défaut) -->
        <targetRef>HEAD</targetRef>

        <!-- ⚠️ IMPORTANT: Chemin RELATIF À LA RACINE DU REPO GIT -->
        <migrationsPath>src/main/resources/db/migration</migrationsPath>

        <!-- Faire un git fetch avant la vérification -->
        <fetchBeforeCheck>true</fetchBeforeCheck>

        <!-- Fail le build si des drifts sont détectés -->
        <failIfBehind>true</failIfBehind>
        <failIfDiverged>true</failIfDiverged>
        <failOnDuplicates>true</failOnDuplicates>

        <!-- Générer un rapport Markdown -->
        <generateReport>true</generateReport>
        <reportFileName>flyway-drift-report.md</reportFileName>
      </configuration>
    </plugin>
  </plugins>
</build>
```

## ⚙️ Configuration

| Paramètre | Défaut | Description |
|-----------|--------|-------------|
| `baseRef` | Auto-détecté | Branche de base (ex: `main`, `origin/main`) |
| `targetRef` | `HEAD` | Branche cible à comparer |
| `migrationsPath` | - | **Chemin RELATIF à la racine du repo Git** |
| `fetchBeforeCheck` | `true` | Faire un `git fetch` avant vérification |
| `failIfBehind` | `true` | Fail si migrations manquantes |
| `failIfDiverged` | `true` | Fail si migrations divergentes |
| `failOnDuplicates` | `true` | Fail si migrations dupliquées |
| `generateReport` | `true` | Générer rapport Markdown |
| `reportFileName` | `flyway-drift-report.md` | Nom du fichier de rapport |
| `skip` | `false` | Skip l'exécution |

### ⚠️ Configuration Critique : `migrationsPath`

Le `migrationsPath` doit être **relatif à la racine du repository Git**, pas au `pom.xml`.

**Exemple avec structure mono-repo** :

```
RDAPP_BACK/                          ← Racine Git (.git est ici)
├── .git/
├── flyway-drift-maven-plugin/
│   ├── pom.xml
│   └── example-project/
│       ├── pom.xml                  ← Votre pom.xml
│       └── src/
│           └── main/
│               └── resources/
│                   └── db/
│                       └── migration/   ← Vos migrations
```

**Configuration correcte** :
```xml
<migrationsPath>flyway-drift-maven-plugin/example-project/src/main/resources/db/migration</migrationsPath>
```

**Configuration incorrecte** :
```xml
<!-- ❌ FAUX : relatif au pom.xml -->
<migrationsPath>src/main/resources/db/migration</migrationsPath>
```

**Comment trouver le bon chemin** :
```bash
# 1. Lister les fichiers dans Git
git ls-tree -r HEAD --name-only | grep migration

# 2. Copier le chemin jusqu'au dossier migration
# Exemple de sortie:
# flyway-drift-maven-plugin/example-project/src/main/resources/db/migration/V1__init.sql
#                                                                          ^^^^^^^^^^^^
#                                Utilisez ce chemin dans migrationsPath
```

## 🚀 Utilisation

### Commande de base

```bash
mvn flyway-drift:check
```

### Avec paramètres

```bash
# Comparer avec une branche spécifique
mvn flyway-drift:check -Dflyway.drift.baseRef=origin/main

# Désactiver le fetch automatique
mvn flyway-drift:check -Dflyway.drift.fetchBeforeCheck=false

# Comparer deux branches
mvn flyway-drift:check \
  -Dflyway.drift.baseRef=origin/develop \
  -Dflyway.drift.targetRef=origin/feature/my-branch

# Ignorer la vérification
mvn flyway-drift:check -Dflyway.drift.skip=true
```

## 🔄 Fetch Automatique

Par défaut, le plugin fait un `git fetch origin` avant la vérification pour s'assurer que les branches distantes sont à jour.

**Activation** (par défaut) :
```xml
<fetchBeforeCheck>true</fetchBeforeCheck>
```

**Désactivation** :
```xml
<fetchBeforeCheck>false</fetchBeforeCheck>
```

**En ligne de commande** :
```bash
mvn flyway-drift:check -Dflyway.drift.fetchBeforeCheck=false
```

**Comportement** :
- ✅ Si le fetch réussit : `✓ Derniers changements récupérés avec succès depuis origin.`
- ⚠️ Si le fetch échoue (pas de réseau) : Continue en mode silencieux avec l'état local

## 📊 Exemples de détection

### 1. 🟠 Behind (Migrations manquantes)

**Base (`main`)** :
```
V1__init.sql
V2__add_users.sql
V3__add_products.sql
```

**Target (`HEAD`)** :
```
V1__init.sql
V2__add_users.sql
```

**Résultat** :
```
================================================================================
RAPPORT DE DRIFT DES MIGRATIONS FLYWAY
================================================================================

Branche de base:   main
Branche cible:     HEAD

❌ DRIFTS DÉTECTÉS: 1 problème(s)

🟠 MIGRATIONS MANQUANTES (présentes dans la base, absentes de la cible):
  - V3__add_products (hash: abc12345)

================================================================================

❌ DRIFT DE MIGRATION FLYWAY DÉTECTÉ

🟠 Migrations manquantes détectées (absentes dans la branche cible).

Consultez le rapport ci-dessus pour plus de détails.

Pour corriger:
  - En retard: Fusionnez ou rebasez avec la branche de base.
```

### 2. 🟡 Diverged (Même version, contenu différent)

**Base (`main`)** :
```sql
-- V1__init.sql
CREATE TABLE users (id INT);
```

**Target (`HEAD`)** :
```sql
-- V1__init.sql
CREATE TABLE users (id BIGINT);  -- Modifié !
```

**Résultat** :
```
================================================================================
RAPPORT DE DRIFT DES MIGRATIONS FLYWAY
================================================================================

Branche de base:   main
Branche cible:     HEAD

❌ DRIFTS DÉTECTÉS: 1 problème(s)

🟡 MIGRATIONS DIVERGENTES (même version, contenu différent):
  - V1__init
    Base:  e3b0c442b4f2e123
    Cible: 9f86d081a4d0e456

================================================================================

❌ DRIFT DE MIGRATION FLYWAY DÉTECTÉ

🟡 Migrations divergentes détectées (même version, contenu différent).

Consultez le rapport ci-dessus pour plus de détails.

Pour corriger:
  - Divergentes: Ne modifiez jamais les migrations existantes. Créez plutôt une nouvelle migration.
```

### 3. 🔴 Duplicates (Même version, plusieurs fichiers)

**Target (`HEAD`)** :
```
V1__init.sql
V1__initialize.sql  # Duplicate !
V2__add_users.sql
```

**Résultat** :
```
================================================================================
RAPPORT DE DRIFT DES MIGRATIONS FLYWAY
================================================================================

Branche de base:   main
Branche cible:     HEAD

❌ DRIFTS DÉTECTÉS: 1 problème(s)

🔴 MIGRATIONS DUPLIQUÉES DANS LA CIBLE (HEAD):
  - V1 (2 fichiers)
    • V1__init.sql
    • V1__initialize.sql

================================================================================

❌ DRIFT DE MIGRATION FLYWAY DÉTECTÉ

🔴 Migrations dupliquées trouvées.

Consultez le rapport ci-dessus pour plus de détails.

Pour corriger:
  - Doublons: Supprimez les fichiers de migration dupliqués.
```

### 4. ✅ Aucun drift

**Résultat** :
```
================================================================================
RAPPORT DE DRIFT DES MIGRATIONS FLYWAY
================================================================================

Branche de base:   main
Branche cible:     HEAD

✅ Aucun drift détecté. Toutes les migrations sont cohérentes.
================================================================================

✅ Aucun drift détecté. Le build peut continuer.
```

## 📄 Rapport généré

Le plugin génère un rapport Markdown dans `target/flyway-drift-report.md` :

```markdown
# Rapport de Drift des Migrations Flyway

**Généré le:** 2026-01-06 20:36:25

**Branche de base:** `main`

**Branche cible:** `HEAD`

## ❌ Drifts Détectés

**Nombre total de problèmes:** 2

### 🟠 Migrations Manquantes (En Retard)

Migrations présentes dans `main` mais absentes de `HEAD`:

| Migration | Type | Hash |
|-----------|------|------|
| `V4__add_categories_table` | VERSIONED | `d3afe5e4` |

### 🟡 Migrations Divergentes

Migrations avec la même version mais un contenu différent:

| Migration | Hash Base | Hash Cible |
|-----------|-----------|------------|
| `V2__add_products_table` | `163a93c0` | `82e4b06c` |

## 📋 Recommandations

- **En retard:** Fusionnez ou rebasez `HEAD` avec `main` pour récupérer les migrations manquantes.
- **Divergentes:** Contenu différent détecté. Ne modifiez jamais une migration existante. Créez plutôt une nouvelle migration.
```

## 🚦 Intégration CI/CD

### GitHub Actions

```yaml
name: Flyway Drift Check

on:
  pull_request:
    branches: [ main ]

jobs:
  drift-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0  # ⚠️ IMPORTANT : fetch all history

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Check Flyway Drift
        run: mvn flyway-drift:check
```

### GitLab CI

```yaml
flyway-drift-check:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  script:
    - mvn flyway-drift:check
  only:
    - merge_requests
```

**⚠️ Important** : `fetch-depth: 0` est **obligatoire** pour accéder à l'historique complet des branches.

## 🔧 Convention Flyway

Le plugin reconnaît les formats suivants :

### Versioned Migrations

```
V1__init.sql
V1_1__add_table.sql
V1.2__update.sql
V2__create_index.sql
```

**Format** : `V<version>__<description>.sql`

### Repeatable Migrations

```
R__refresh_view.sql
R__insert_data.sql
```

**Format** : `R__<description>.sql`

## 🐛 Dépannage

### Le plugin trouve 0 fichiers

**Cause** : Le `migrationsPath` est incorrect.

**Solution** :
```bash
# Lister les fichiers dans Git
git ls-tree -r HEAD --name-only | grep migration

# Utiliser ce chemin dans votre pom.xml
```

### Le fetch ne fonctionne pas

**Cause** : Le fetch s'exécute mais n'a rien à récupérer (tout est à jour).

**Vérification** :
```bash
git log origin/main --oneline -1  # Version distante
git log main --oneline -1         # Version locale
```

Si `main` local est en avance sur `origin/main`, poussez vos commits :
```bash
git push origin main
```

### "La référence de base n'existe pas"

**Cause** : La branche n'existe pas localement.

**Solution** :
```bash
git fetch origin
git branch -a  # Vérifier les branches disponibles
```

En CI/CD, assurez-vous d'utiliser `fetch-depth: 0` dans GitHub Actions.

### Le plugin ne détecte pas mes modifications

**Cause** : Les fichiers modifiés ne sont **pas committés**.

**Important** : Le plugin lit les **commits Git**, pas les fichiers modifiés dans le working directory.

**Solution** :
```bash
git add .
git commit -m "test drift"
mvn flyway-drift:check
```

## 🧪 Tests

Pour tester le plugin avec des scénarios réels, consultez le fichier [`SCENARIOS-DE-TEST.md`](example-project/SCENARIOS-DE-TEST.md) dans le projet d'exemple.

## 📚 Architecture

### Structure du plugin

```
flyway-drift-maven-plugin/
├── pom.xml
└── src/main/java/com/example/flyway/drift/
    ├── FlywayDriftCheckMojo.java        # Mojo principal
    ├── model/
    │   └── FlywayMigration.java         # Modèle migration
    ├── git/
    │   └── GitFileReader.java           # Lecture Git via JGit + Fetch
    ├── parser/
    │   └── MigrationParser.java         # Parser migrations
    ├── detector/
    │   └── DriftDetector.java           # Détection drifts
    └── report/
        └── DriftReport.java             # Génération rapports
```

### Algorithme de détection

1. **Fetch automatique** (si activé) via JGit
2. **Lecture des fichiers** via JGit depuis les deux refs
3. **Parsing** des migrations (extraction version, description)
4. **Calcul SHA-256** du contenu de chaque fichier
5. **Détection** :
   - Duplicates : Map version → List<Migration>
   - Behind : Migrations dans base ∖ target
   - Diverged : Même version, hash différent
6. **Génération** du rapport Markdown en français

## 🚫 Limitations

- **Pas de support SQL** : Le plugin ne parse pas le contenu SQL, seulement le nom de fichier et le hash
- **Pas d'exécution Flyway** : Aucune connexion base de données requise
- **Git uniquement** : Fonctionne uniquement avec Git (pas SVN, Mercurial, etc.)

## 🤝 Contributing

Contributions bienvenues ! Pour contribuer :

1. Fork le repo
2. Créez une branche (`git checkout -b feature/amazing-feature`)
3. Commit (`git commit -m 'Add amazing feature'`)
4. Push (`git push origin feature/amazing-feature`)
5. Ouvrez une Pull Request

## 📝 License

MIT License

## 📞 Support

Pour toute question ou problème :
- Consulter [`SCENARIOS-DE-TEST.md`](example-project/SCENARIOS-DE-TEST.md)
- Consulter la documentation Flyway : https://flywaydb.org/

---

**Version** : 1.0.0
**Auteur** : Mohamed DIOP (diopmo0312@gmail.com)
**Java** : 17+
**Maven** : 3.6+
