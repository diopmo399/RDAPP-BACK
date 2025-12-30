# Flyway Drift Maven Plugin - Démarrage Rapide

## 🚀 Installation en 3 étapes

### Étape 1 : Installer le plugin

```bash
cd flyway-drift-maven-plugin
mvn clean install
```

### Étape 2 : Ajouter le plugin à votre `pom.xml`

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
    </plugin>
  </plugins>
</build>
```

### Étape 3 : Exécuter

```bash
mvn flyway-drift:check
```

---

## ✅ Test avec le projet exemple

```bash
# 1. Aller dans le projet exemple
cd example-project

# 2. Exécuter le plugin
mvn flyway-drift:check

# 3. Voir le rapport
cat target/flyway-drift-report.md
```

---

## 🧪 Simuler des drifts

### Test 1 : Behind (migration manquante)

```bash
# 1. Créer une branche de test
git checkout -b test-behind

# 2. Supprimer une migration
rm src/main/resources/db/migration/V2__add_products_table.sql

# 3. Commit
git add .
git commit -m "Remove migration V2"

# 4. Exécuter le plugin
mvn flyway-drift:check

# Résultat attendu : ❌ FAIL (migration V2 manquante)
```

### Test 2 : Diverged (contenu modifié)

```bash
# 1. Créer une branche de test
git checkout -b test-diverged

# 2. Modifier une migration existante
echo "-- Modified" >> src/main/resources/db/migration/V1__init.sql

# 3. Commit
git add .
git commit -m "Modify V1 migration"

# 4. Exécuter le plugin
mvn flyway-drift:check

# Résultat attendu : ❌ FAIL (migration V1 divergente)
```

### Test 3 : Duplicates

```bash
# 1. Créer une branche de test
git checkout -b test-duplicates

# 2. Créer un fichier dupliqué
cp src/main/resources/db/migration/V1__init.sql \
   src/main/resources/db/migration/V1__initialize.sql

# 3. Commit
git add .
git commit -m "Add duplicate V1"

# 4. Exécuter le plugin
mvn flyway-drift:check

# Résultat attendu : ❌ FAIL (migration V1 dupliquée)
```

---

## 🔧 Configuration minimale

```xml
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
  <!-- Configuration par défaut : détection automatique -->
</plugin>
```

---

## 🎯 Configuration avancée

```xml
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
    <!-- Branche de base (auto-détection si vide) -->
    <baseRef>origin/main</baseRef>

    <!-- Branche cible (HEAD par défaut) -->
    <targetRef>HEAD</targetRef>

    <!-- Chemin des migrations -->
    <migrationsPath>src/main/resources/db/migration</migrationsPath>

    <!-- Fail si behind -->
    <failIfBehind>true</failIfBehind>

    <!-- Fail si diverged -->
    <failIfDiverged>true</failIfDiverged>

    <!-- Fail si duplicates -->
    <failOnDuplicates>true</failOnDuplicates>

    <!-- Générer le rapport -->
    <generateReport>true</generateReport>

    <!-- Nom du rapport -->
    <reportFileName>flyway-drift-report.md</reportFileName>

    <!-- Skip l'exécution -->
    <skip>false</skip>
  </configuration>
</plugin>
```

---

## 📊 Cas d'usage CI/CD

### GitHub Actions

```yaml
name: CI

on:
  pull_request:
    branches:
      - main

jobs:
  flyway-drift-check:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # ⚠️ IMPORTANT

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'maven'

      - name: Flyway Drift Check
        run: mvn flyway-drift:check
```

### GitLab CI

```yaml
flyway-drift-check:
  stage: validate
  image: maven:3.9-eclipse-temurin-17
  script:
    - git fetch origin main  # Fetch base branch
    - mvn flyway-drift:check
  only:
    - merge_requests
```

---

## 🐛 Troubleshooting

### Erreur : "Cannot resolve Git ref: origin/main"

**Cause** : `fetch-depth: 1` en CI (shallow clone)

**Solution** : Utilisez `fetch-depth: 0`

```yaml
- uses: actions/checkout@v4
  with:
    fetch-depth: 0  # Fetch all history
```

### Erreur : "Cannot auto-detect base branch"

**Cause** : Ni `origin/main` ni `origin/master` n'existe

**Solution** : Spécifiez explicitement `<baseRef>`

```xml
<configuration>
  <baseRef>origin/develop</baseRef>
</configuration>
```

### Aucune migration détectée

**Cause** : Mauvais chemin de migrations

**Solution** : Vérifiez `<migrationsPath>`

```xml
<configuration>
  <migrationsPath>src/main/resources/db/migration</migrationsPath>
</configuration>
```

---

## 📝 Commandes utiles

```bash
# Exécuter le plugin
mvn flyway-drift:check

# Exécuter avec une branche spécifique
mvn flyway-drift:check -Dflyway.drift.targetRef=feature/my-branch

# Comparer deux branches
mvn flyway-drift:check \
  -Dflyway.drift.baseRef=origin/main \
  -Dflyway.drift.targetRef=origin/develop

# Skip l'exécution
mvn clean install -Dflyway.drift.skip=true

# Voir le rapport généré
cat target/flyway-drift-report.md

# Débug (verbose)
mvn flyway-drift:check -X
```

---

## 🎓 Prochaines étapes

1. **Lire le README complet** : `README.md`
2. **Tester le projet exemple** : `example-project/`
3. **Intégrer dans votre CI/CD**
4. **Adapter la configuration** selon vos besoins

---

**Version** : 1.0.0
**Temps de setup** : < 5 minutes
