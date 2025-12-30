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

✅ **Compatible CI/CD** (GitHub Actions, GitLab CI, Jenkins)
✅ **Aucune modification du workspace**
✅ **Rapports Markdown** générés dans `target/`
✅ **Auto-détection** de `origin/main` ou `origin/master`
✅ **Support Repeatable migrations** (`R__*.sql`)

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
          <goals>
            <goal>check</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <baseRef>origin/main</baseRef>
        <targetRef>HEAD</targetRef>
        <migrationsPath>src/main/resources/db/migration</migrationsPath>
        <failIfBehind>true</failIfBehind>
        <failIfDiverged>true</failIfDiverged>
        <failOnDuplicates>true</failOnDuplicates>
      </configuration>
    </plugin>
  </plugins>
</build>
```

## ⚙️ Configuration

| Paramètre | Défaut | Description |
|-----------|--------|-------------|
| `baseRef` | Auto-détecté | Branche de base (ex: `origin/main`) |
| `targetRef` | `HEAD` | Branche cible à comparer |
| `migrationsPath` | `src/main/resources/db/migration` | Chemin des migrations |
| `failIfBehind` | `true` | Fail si migrations manquantes |
| `failIfDiverged` | `true` | Fail si migrations divergentes |
| `failOnDuplicates` | `true` | Fail si migrations dupliquées |
| `generateReport` | `true` | Générer rapport Markdown |
| `reportFileName` | `flyway-drift-report.md` | Nom du fichier de rapport |
| `skip` | `false` | Skip l'exécution |

## 🚀 Utilisation

### Exécution locale

```bash
# Comparer HEAD avec origin/main
mvn flyway-drift:check

# Comparer une branche spécifique avec main
mvn flyway-drift:check -Dflyway.drift.targetRef=feature/my-branch

# Comparer deux branches
mvn flyway-drift:check \
  -Dflyway.drift.baseRef=origin/develop \
  -Dflyway.drift.targetRef=origin/feature/my-branch
```

### GitHub Actions

```yaml
name: Flyway Drift Check

on:
  pull_request:
    branches:
      - main
      - develop

jobs:
  flyway-drift:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # ⚠️ IMPORTANT : fetch all history

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'maven'

      - name: Check Flyway Drift
        run: mvn flyway-drift:check
```

**⚠️ Important** : `fetch-depth: 0` est **obligatoire** pour accéder à l'historique complet des branches.

## 📊 Exemples de détection

### 1. Behind (Migrations manquantes)

**Base (`origin/main`)** :
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
🟠 BEHIND MIGRATIONS (present in base, missing in target):
  - V3__add_products (hash: abc12345)

❌ BUILD FAILED
```

### 2. Diverged (Même version, contenu différent)

**Base (`origin/main`)** :
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
🟡 DIVERGED MIGRATIONS (same version, different content):
  - V1__init
    Base:   e3b0c442b
    Target: 9f86d081a

❌ BUILD FAILED
```

### 3. Duplicates (Même version, plusieurs fichiers)

**Target (`HEAD`)** :
```
V1__init.sql
V1__initialize.sql  # Duplicate !
V2__add_users.sql
```

**Résultat** :
```
🔴 DUPLICATE MIGRATIONS IN TARGET (HEAD):
  - V1 (2 files)
    • V1__init.sql
    • V1__initialize.sql

❌ BUILD FAILED
```

## 📄 Rapport généré

Le plugin génère un rapport Markdown dans `target/flyway-drift-report.md` :

```markdown
# Flyway Migration Drift Report

**Generated:** 2025-12-30 12:00:00

**Base Ref:** `origin/main`

**Target Ref:** `HEAD`

## ❌ Drifts Detected

**Total Issues:** 3

### 🟠 Behind Migrations

Migrations present in `origin/main` but missing in `HEAD`:

| Migration | Type | Hash |
|-----------|------|------|
| `V3__add_products` | VERSIONED | `abc12345` |

### 🟡 Diverged Migrations

Migrations with same version but different content:

| Migration | Base Hash | Target Hash |
|-----------|-----------|-------------|
| `V1__init` | `e3b0c442` | `9f86d081` |

## 📋 Recommendations

- **Behind:** Merge or rebase `HEAD` with `origin/main` to get missing migrations.
- **Diverged:** Content mismatch detected. Never modify existing migrations. Create a new migration instead.
```

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

## 🛡️ Cas limites gérés

### Repository shallow

Si vous utilisez `fetch-depth: 1` en CI, le plugin échouera avec un message clair :

```
Base ref does not exist: origin/main

Hint: If running in CI, ensure fetch-depth is set to 0 in GitHub Actions checkout.
```

**Solution** : Utilisez `fetch-depth: 0` dans `actions/checkout`.

### Premier commit

Si la branche cible est au premier commit (pas d'historique), le plugin skip proprement :

```
✅ No drifts detected. Build can proceed.
```

### Ref inexistante

```
❌ Base ref does not exist: origin/develop

Please specify a valid <baseRef> in plugin configuration.
```

## 🧪 Tests

### Test unitaire (exemple)

Créez un test Maven IT :

```xml
<project>
  <build>
    <plugins>
      <plugin>
        <groupId>com.example</groupId>
        <artifactId>flyway-drift-maven-plugin</artifactId>
        <version>1.0.0</version>
        <executions>
          <execution>
            <goals>
              <goal>check</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

Créez des migrations de test :

```
src/test/resources/db/migration/
  ├── V1__init.sql
  ├── V2__add_users.sql
  └── V3__add_products.sql
```

Exécutez :

```bash
mvn verify
```

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
    │   └── GitFileReader.java           # Lecture Git via JGit
    ├── parser/
    │   └── MigrationParser.java         # Parser migrations
    ├── detector/
    │   └── DriftDetector.java           # Détection drifts
    └── report/
        └── DriftReport.java             # Génération rapports
```

### Algorithme de détection

1. **Lecture des fichiers** via JGit depuis les deux refs
2. **Parsing** des migrations (extraction version, description)
3. **Calcul SHA-256** du contenu de chaque fichier
4. **Détection** :
   - Duplicates : Map version → List<Migration>
   - Behind : Migrations dans base ∖ target
   - Diverged : Même version, hash différent
5. **Génération** du rapport Markdown

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
- Ouvrir une issue sur GitHub
- Consulter la documentation Flyway : https://flywaydb.org/

---

**Version** : 1.0.0
**Auteur** : Flyway Drift Plugin Team
**Java** : 17+
**Maven** : 3.6+
