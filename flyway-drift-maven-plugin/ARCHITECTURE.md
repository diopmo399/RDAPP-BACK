# Architecture - Flyway Drift Maven Plugin

## Vue d'ensemble

Le plugin utilise **JGit** pour lire les fichiers Flyway directement depuis le repository Git, sans modifier le workspace, et compare les migrations entre deux refs Git.

## Diagramme de flux

```
┌─────────────────────────────────────────────────────────────┐
│                    FlywayDriftCheckMojo                      │
│                   (Maven Plugin Entry)                       │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            │ 1. Résoudre refs Git
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      GitFileReader                           │
│  - Ouvre le repository Git (.git/)                          │
│  - Résout baseRef (origin/main) et targetRef (HEAD)         │
│  - Lit les fichiers .sql via TreeWalk                       │
│  - Calcule SHA-256 pour chaque fichier                      │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            │ 2. Parse migrations
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    MigrationParser                           │
│  - Valide les noms de fichiers (V*, R__)                    │
│  - Extrait version et description                           │
│  - Crée des objets FlywayMigration                          │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            │ 3. Détecte drifts
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     DriftDetector                            │
│  - Détecte duplicates (même version)                        │
│  - Détecte behind (manquant dans target)                    │
│  - Détecte diverged (même version, hash différent)          │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            │ 4. Génère rapport
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                       DriftReport                            │
│  - Affiche dans la console (logs Maven)                     │
│  - Génère rapport Markdown (target/)                        │
│  - Fail le build si drifts détectés                         │
└─────────────────────────────────────────────────────────────┘
```

## Composants

### 1. FlywayDriftCheckMojo

**Responsabilité** : Point d'entrée Maven, orchestration.

**Paramètres** :
- `baseRef` : Branche de base (ex: origin/main)
- `targetRef` : Branche cible (HEAD)
- `migrationsPath` : Chemin des migrations
- `failIfBehind`, `failIfDiverged`, `failOnDuplicates` : Flags de fail
- `generateReport`, `reportFileName` : Génération rapport

**Workflow** :
1. Résoudre les refs Git
2. Lire les fichiers de migration via `GitFileReader`
3. Parser les migrations via `MigrationParser`
4. Détecter les drifts via `DriftDetector`
5. Générer le rapport via `DriftReport`
6. Fail le build si nécessaire

### 2. GitFileReader

**Responsabilité** : Lecture des fichiers depuis Git via JGit.

**Méthodes clés** :
- `resolveRef(String ref)` : Résout une ref Git en ObjectId
- `listMigrationFiles(String ref, String path)` : Liste tous les .sql dans un chemin
- `readFileContent(String ref, String filePath)` : Lit un fichier spécifique
- `detectMainBranch()` : Auto-détecte origin/main ou origin/master
- `refExists(String ref)` : Vérifie si une ref existe

**Algorithme `listMigrationFiles`** :
```java
1. Résoudre ref → commit ObjectId
2. Ouvrir RevWalk → RevCommit
3. Récupérer RevTree du commit
4. Créer TreeWalk avec PathFilter sur migrationsPath
5. Pour chaque fichier .sql :
   a. Lire le contenu (ObjectLoader)
   b. Calculer SHA-256 du contenu
   c. Ajouter à Map<fileName, hash>
6. Retourner la map
```

### 3. FlywayMigration

**Responsabilité** : Modèle de migration Flyway.

**Attributs** :
- `fileName` : Nom du fichier (ex: V1__init.sql)
- `type` : VERSIONED ou REPEATABLE
- `version` : Version normalisée (ex: 1.2.3)
- `description` : Description extraite du nom
- `contentHash` : SHA-256 du contenu
- `filePath` : Chemin complet

**Parsing du nom** :
```java
// Versioned: V1__init.sql
Pattern: ^V(\d+(?:[._]\d+)*)__(.+)\.sql$
  → version = "1", description = "init"

// Repeatable: R__refresh_view.sql
Pattern: ^R__(.+)\.sql$
  → version = null, description = "refresh_view"
```

**Normalisation de version** :
```java
V1_2_3 → 1.2.3
V1.2.3 → 1.2.3
```

**Comparaison** :
- Versioned : compare versions sémantiquement (1.10 > 1.9)
- Repeatable : compare par description alphabétique

### 4. MigrationParser

**Responsabilité** : Parser les fichiers Flyway.

**Méthode** :
```java
parseMigrations(Map<fileName, hash>) {
  for (fileName, hash) in filesWithHash:
    if isValidMigrationFile(fileName):
      migration = new FlywayMigration(fileName, filePath, hash)
      migrations.add(migration)

  migrations.sort()  // Par version
  return migrations
}
```

**Validation** :
- Nom doit matcher `V<version>__<description>.sql` ou `R__<description>.sql`
- Extension doit être `.sql`

### 5. DriftDetector

**Responsabilité** : Détection des drifts.

**Algorithme** :

#### Duplicates
```java
1. Grouper migrations par version (pour versioned) ou fileName (pour repeatable)
2. Pour chaque groupe avec size > 1 :
   → Ajouter à duplicates
```

#### Behind
```java
1. Créer Map<version, migration> de target
2. Pour chaque migration dans base :
   if version not in targetMap:
     → Ajouter à behind
```

#### Diverged
```java
1. Créer Map<version, migration> de base et target
2. Pour chaque version commune :
   if hash_base != hash_target:
     → Ajouter à diverged
```

**Clé unique** :
- Versioned : `V<version>` (ex: V1.2.3)
- Repeatable : `fileName` (ex: R__refresh_view.sql)

### 6. DriftReport

**Responsabilité** : Génération du rapport.

**Formats** :
1. **Console** : Logs Maven colorés (🟠, 🔴, 🟡)
2. **Markdown** : Fichier `target/flyway-drift-report.md`

**Structure Markdown** :
```markdown
# Flyway Migration Drift Report

**Base Ref:** origin/main
**Target Ref:** HEAD

## ❌ Drifts Detected

### 🔴 Duplicate Migrations
### 🟠 Behind Migrations
### 🟡 Diverged Migrations

## 📋 Recommendations
```

## Algorithme complet

```
┌─ execute() ────────────────────────────────────────────────┐
│                                                             │
│ 1. Résoudre refs                                           │
│    baseRef = resolveBaseRef()  // Auto-detect si vide      │
│    targetRef = "HEAD"                                       │
│                                                             │
│ 2. Valider refs                                            │
│    if !refExists(baseRef) → FAIL                           │
│    if !refExists(targetRef) → FAIL                         │
│                                                             │
│ 3. Lire migrations depuis Git                              │
│    baseFiles = gitReader.listMigrationFiles(baseRef)       │
│    targetFiles = gitReader.listMigrationFiles(targetRef)   │
│                                                             │
│ 4. Parser migrations                                        │
│    baseMigrations = parser.parseMigrations(baseFiles)      │
│    targetMigrations = parser.parseMigrations(targetFiles)  │
│                                                             │
│ 5. Détecter drifts                                         │
│    detector = new DriftDetector(base, target)              │
│    result = detector.detectDrifts()                        │
│                                                             │
│ 6. Générer rapport                                         │
│    report = new DriftReport(result)                        │
│    report.printToConsole()                                 │
│    report.generateMarkdownReport()                         │
│                                                             │
│ 7. Fail si nécessaire                                      │
│    if (failIfBehind && !result.behindMigrations.isEmpty()) │
│      → throw MojoFailureException                          │
│    if (failIfDiverged && !result.diverged.isEmpty())       │
│      → throw MojoFailureException                          │
│    if (failOnDuplicates && !result.duplicates.isEmpty())   │
│      → throw MojoFailureException                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Calcul du hash

**Pourquoi SHA-256 ?**
- Détection fiable de modifications (même minimes)
- Rapide à calculer
- Collision quasi-impossible

**Implémentation** :
```java
import org.apache.commons.codec.digest.DigestUtils;

String content = readFileContent(ref, filePath);
String hash = DigestUtils.sha256Hex(content);
```

**Exemple** :
```sql
-- V1__init.sql
CREATE TABLE users (id INT);
```
→ Hash : `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`

Modification :
```sql
-- V1__init.sql
CREATE TABLE users (id BIGINT);  -- Changé de INT à BIGINT
```
→ Hash : `9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08` (différent)

## Gestion des cas limites

### 1. Repository shallow (`fetch-depth: 1`)

**Problème** : `origin/main` n'existe pas localement.

**Détection** :
```java
if (!gitReader.refExists(baseRef)) {
  throw new MojoFailureException(
    "Base ref does not exist: " + baseRef +
    "\n\nHint: If running in CI, ensure fetch-depth is set to 0."
  );
}
```

**Solution** : `fetch-depth: 0` dans GitHub Actions.

### 2. Premier commit

**Problème** : Pas de commit parent.

**Gestion** :
```java
if (baseMigrations.isEmpty() && targetMigrations.isEmpty()) {
  log.info("No migrations found. Skip drift check.");
  return;
}
```

### 3. Ref inexistante

**Exemple** : `origin/develop` n'existe pas.

**Détection** :
```java
ObjectId objectId = repository.resolve(ref);
if (objectId == null) {
  throw new IOException("Cannot resolve Git ref: " + ref);
}
```

### 4. Fichiers non-SQL

**Exemple** : `README.md` dans `db/migration/`

**Filtrage** :
```java
if (!path.endsWith(".sql")) {
  continue;  // Ignorer
}
```

### 5. Noms invalides

**Exemple** : `migration.sql` (pas de version)

**Gestion** :
```java
try {
  migration = new FlywayMigration(fileName, filePath, hash);
} catch (IllegalArgumentException e) {
  log.warn("Invalid migration filename ignored: " + fileName);
}
```

## Performance

### Optimisations

1. **Pas de checkout** : Lecture directe depuis Git (pas de modification du workspace)
2. **TreeWalk avec PathFilter** : Lecture uniquement du répertoire migrations
3. **SHA-256 calculé une fois** : Stocké dans FlywayMigration
4. **Map pour les comparaisons** : O(1) lookup au lieu de O(n)

### Complexité

- **Lecture Git** : O(n) où n = nombre de fichiers .sql
- **Parsing** : O(n)
- **Duplicates** : O(n)
- **Behind** : O(n)
- **Diverged** : O(n)
- **Total** : O(n)

### Benchmarks (estimé)

| Nombre de migrations | Temps d'exécution |
|---------------------|-------------------|
| 10 | < 1s |
| 100 | < 2s |
| 1000 | < 5s |

## Dépendances

| Dépendance | Version | Usage |
|------------|---------|-------|
| `org.eclipse.jgit` | 6.8.0 | Lecture fichiers Git |
| `commons-codec` | 1.16.0 | Calcul SHA-256 |
| `maven-plugin-api` | 3.9.6 | API Maven Plugin |
| `maven-plugin-annotations` | 3.11.0 | Annotations Mojo |

## Extensibilité

### Ajouter un nouveau type de drift

1. Ajouter la détection dans `DriftDetector` :
```java
public List<NewDrift> detectNewDrift() {
  // Logique de détection
}
```

2. Ajouter au `DriftResult` :
```java
public static class DriftResult {
  public List<NewDrift> newDrifts = new ArrayList<>();
}
```

3. Ajouter dans le rapport :
```java
if (!result.newDrifts.isEmpty()) {
  sb.append("### 🔵 New Drift Type\n\n");
  // ...
}
```

### Ajouter un nouveau format de rapport

1. Créer une nouvelle classe :
```java
public class JsonReportGenerator {
  public String generateJson(DriftResult result) {
    // Générer JSON
  }
}
```

2. Appeler dans le Mojo :
```java
JsonReportGenerator jsonGen = new JsonReportGenerator();
String json = jsonGen.generateJson(result);
```

---

**Version** : 1.0.0
**Dernière mise à jour** : 2025-12-30
