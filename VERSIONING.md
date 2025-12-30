# Versioning Strategy - CI-Friendly Maven avec GitHub Actions

## 🎯 Objectif

Garantir que :
- **Builds CI** : utilisent des versions SNAPSHOT uniques **sans timestamp Maven**
- **Releases** : produisent des versions **stables** (ex: `1.0.0`) **sans SNAPSHOT, sans timestamp**

## ❌ Problème actuel

### Symptômes

Lors des builds Maven avec SNAPSHOT deployment, Maven génère automatiquement des versions avec timestamp :

```
Version dans pom.xml:     1.0.0-SNAPSHOT
Version déployée Nexus:   1.0.0-20251229.091234-1
                                 ^^^^^^^^^^^^^^^^
                                 TIMESTAMP MAVEN
```

**Conséquences** :

1. ❌ Les builds CI utilisent des versions avec timestamp → impossible à tracer
2. ❌ Les releases peuvent réutiliser une version SNAPSHOT timestampée → **version instable en prod**
3. ❌ Le pom.xml publié contient `${revision}` au lieu de la version résolue
4. ❌ Confusion entre la version du code source et la version déployée

### Pourquoi ça arrive ?

#### 1. Maven SNAPSHOT Deployment

Quand vous déployez un artifact SNAPSHOT vers un repository Maven (Nexus, Artifactory) :

```bash
mvn deploy
```

Maven **ajoute automatiquement un timestamp unique** pour permettre plusieurs déploiements de la même version SNAPSHOT :

```
my-artifact-1.0.0-20251229.091234-1.jar
my-artifact-1.0.0-20251229.092145-2.jar
my-artifact-1.0.0-20251229.093456-3.jar
```

C'est le **comportement normal de Maven** pour les SNAPSHOTs.

#### 2. Problème en Release

Si votre workflow de release fait juste `mvn deploy` sans changer la version :

```bash
# ❌ ERREUR : déploie 1.0.0-20251229.091234-1 au lieu de 1.0.0
mvn deploy
```

Vous déployez **la version SNAPSHOT timestampée** au lieu de la version release stable.

#### 3. Problème du POM publié

Si vous utilisez `${revision}` dans votre `pom.xml` **sans** le `flatten-maven-plugin` :

```xml
<version>${revision}</version>
```

Le POM publié vers Maven Central contiendra **littéralement** `${revision}` au lieu de `1.0.0`.

Les consommateurs de votre library ne pourront pas résoudre la version.

## ✅ Solution : CI-Friendly Versioning

### Principes

1. **Utiliser `${revision}`** dans le `pom.xml`
2. **Configurer `flatten-maven-plugin`** pour résoudre `${revision}` lors du deploy
3. **Builds CI** : définir `revision` dynamiquement **sans commit**
4. **Releases** : définir `revision` avec la version stable **sans SNAPSHOT**

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         pom.xml                              │
│  <version>${revision}</version>                              │
│  <properties>                                                │
│    <revision>1.0.0-SNAPSHOT</revision>  ← Valeur par défaut │
│  </properties>                                               │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────┴────────────────┐
        │                                │
        ▼                                ▼
┌────────────────┐            ┌──────────────────┐
│   BUILD CI     │            │     RELEASE      │
│   (PR/push)    │            │   (tag v1.0.0)   │
└────────┬───────┘            └────────┬─────────┘
         │                             │
         ▼                             ▼
mvn -Drevision=              mvn -Drevision=1.0.0
  1.0.0-ci-123-abc1234         -Dchangelist=""
  -SNAPSHOT                    -Dsha1=""
  -Dchangelist=""
  -Dsha1=""
         │                             │
         ▼                             ▼
Version CI:                   Version Release:
1.0.0-ci-123-abc1234-SNAPSHOT 1.0.0 (STABLE)
         │                             │
         ▼                             ▼
  Pas de deploy               Deploy vers Maven Central
  (build + test only)         avec flatten-maven-plugin
                                      │
                                      ▼
                              POM publié: <version>1.0.0</version>
```

## 📋 Implémentation Complète

### 1. Modifier le `pom.xml`

#### Avant (❌ Problématique)

```xml
<groupId>com.example</groupId>
<artifactId>my-project</artifactId>
<version>1.0.0-SNAPSHOT</version>
```

#### Après (✅ CI-Friendly)

```xml
<groupId>com.example</groupId>
<artifactId>my-project</artifactId>
<version>${revision}</version>

<properties>
    <!-- Version par défaut (pour dev local) -->
    <revision>1.0.0-SNAPSHOT</revision>
    <changelist></changelist>
    <sha1></sha1>
</properties>

<build>
    <plugins>
        <!-- Flatten Maven Plugin -->
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>flatten-maven-plugin</artifactId>
            <version>1.5.0</version>
            <configuration>
                <flattenMode>resolveCiFriendliesOnly</flattenMode>
                <updatePomFile>true</updatePomFile>
            </configuration>
            <executions>
                <execution>
                    <id>flatten</id>
                    <phase>process-resources</phase>
                    <goals>
                        <goal>flatten</goal>
                    </goals>
                </execution>
                <execution>
                    <id>flatten.clean</id>
                    <phase>clean</phase>
                    <goals>
                        <goal>clean</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 2. Workflow CI (`.github/workflows/ci.yml`)

```yaml
name: CI Build

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      # Générer version CI unique
      - name: Generate CI version
        id: version
        run: |
          BASE_VERSION=$(mvn help:evaluate -Dexpression=revision -q -DforceStdout 2>/dev/null)
          BASE_VERSION=${BASE_VERSION%-SNAPSHOT}
          SHORT_SHA=$(git rev-parse --short HEAD)
          RUN_NUMBER=${{ github.run_number }}

          # Format: 1.0.0-ci-123-abc1234-SNAPSHOT
          CI_VERSION="${BASE_VERSION}-ci-${RUN_NUMBER}-${SHORT_SHA}-SNAPSHOT"

          echo "ci-version=${CI_VERSION}" >> $GITHUB_OUTPUT
          echo "::notice::CI Version: ${CI_VERSION}"

      # Build avec version CI (SANS deploy)
      - name: Build and Test
        run: |
          mvn -B clean verify \
            -Drevision=${{ steps.version.outputs.ci-version }} \
            -Dchangelist="" \
            -Dsha1=""

      # Vérifier qu'il n'y a pas de timestamp Maven
      - name: Verify version
        run: |
          FINAL_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout \
            -Drevision=${{ steps.version.outputs.ci-version }} \
            -Dchangelist="" \
            -Dsha1="")

          echo "::notice::Final version: ${FINAL_VERSION}"

          # Vérifier qu'il n'y a pas de timestamp Maven (format: YYYYMMDD.HHMMSS-N)
          if [[ $FINAL_VERSION =~ [0-9]{8}\.[0-9]{6}-[0-9]+ ]]; then
            echo "::error::Version contains Maven timestamp: ${FINAL_VERSION}"
            exit 1
          fi
```

**Points clés** :
- ✅ Version CI unique : `1.0.0-ci-123-abc1234-SNAPSHOT`
- ✅ Pas de timestamp Maven (car pas de `deploy`)
- ✅ Pas de commit (la version est passée en paramètre)
- ✅ Vérification anti-timestamp

### 3. Workflow Release (`.github/workflows/release.yml`)

```yaml
name: Release

on:
  push:
    tags:
      - 'v[0-9]+.[0-9]+.[0-9]+'
  workflow_dispatch:
    inputs:
      version:
        description: 'Release version (ex: 1.2.3)'
        required: true

jobs:
  validate:
    runs-on: ubuntu-latest
    outputs:
      release-version: ${{ steps.version.outputs.release-version }}
    steps:
      - uses: actions/checkout@v4

      - name: Determine version
        id: version
        run: |
          if [ "${{ github.event_name }}" = "workflow_dispatch" ]; then
            VERSION="${{ github.event.inputs.version }}"
          else
            VERSION="${GITHUB_REF#refs/tags/v}"
          fi
          echo "release-version=${VERSION}" >> $GITHUB_OUTPUT

      - name: Validate version
        run: |
          VERSION="${{ steps.version.outputs.release-version }}"

          # Vérifier format semver
          if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
            echo "::error::Invalid version: ${VERSION}"
            exit 1
          fi

          # Vérifier pas de SNAPSHOT
          if [[ $VERSION =~ SNAPSHOT ]]; then
            echo "::error::Release cannot contain SNAPSHOT"
            exit 1
          fi

          # Vérifier pas de timestamp Maven
          if [[ $VERSION =~ [0-9]{8}\.[0-9]{6}-[0-9]+ ]]; then
            echo "::error::Release cannot contain Maven timestamp"
            exit 1
          fi

  deploy:
    needs: validate
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'
          server-id: ossrh
          server-username: MAVEN_USERNAME
          server-password: MAVEN_PASSWORD
          gpg-private-key: ${{ secrets.GPG_PRIVATE_KEY }}
          gpg-passphrase: MAVEN_GPG_PASSPHRASE

      # Build + Deploy avec version STABLE
      - name: Deploy Release
        env:
          MAVEN_USERNAME: ${{ secrets.MAVEN_USERNAME }}
          MAVEN_PASSWORD: ${{ secrets.MAVEN_PASSWORD }}
          MAVEN_GPG_PASSPHRASE: ${{ secrets.MAVEN_GPG_PASSPHRASE }}
        run: |
          RELEASE_VERSION="${{ needs.validate.outputs.release-version }}"

          mvn -B clean deploy \
            -Drevision=${RELEASE_VERSION} \
            -Dchangelist="" \
            -Dsha1="" \
            -DskipTests=true \
            -Prelease

      # Vérifier la version finale
      - name: Verify Maven version
        run: |
          EXPECTED="${{ needs.validate.outputs.release-version }}"
          ACTUAL=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout \
            -Drevision=${EXPECTED} \
            -Dchangelist="" \
            -Dsha1="")

          if [ "$ACTUAL" != "$EXPECTED" ]; then
            echo "::error::Version mismatch: expected ${EXPECTED}, got ${ACTUAL}"
            exit 1
          fi

          echo "::notice::✅ Release version verified: ${ACTUAL}"
```

**Points clés** :
- ✅ Version release stable : `1.0.0` (sans SNAPSHOT)
- ✅ Validations strictes (pas de SNAPSHOT, pas de timestamp)
- ✅ Deploy vers Maven Central avec flatten-maven-plugin
- ✅ Le POM publié contient `<version>1.0.0</version>` (résolu)

## 🔍 Debug et Vérification

### Vérifier la version localement

```bash
# Version par défaut (du pom.xml)
mvn help:evaluate -Dexpression=project.version -q -DforceStdout

# Version CI
mvn help:evaluate -Dexpression=project.version -q -DforceStdout \
  -Drevision=1.0.0-ci-123-abc1234-SNAPSHOT \
  -Dchangelist="" \
  -Dsha1=""

# Version release
mvn help:evaluate -Dexpression=project.version -q -DforceStdout \
  -Drevision=1.0.0 \
  -Dchangelist="" \
  -Dsha1=""
```

### Vérifier le POM résolu (flatten)

```bash
# Build avec flatten
mvn clean package -Drevision=1.0.0

# Le POM résolu est dans target/.flattened-pom.xml
cat target/.flattened-pom.xml | grep "<version>"
# Doit afficher: <version>1.0.0</version>
```

### Tester le deploy local

```bash
# Deploy vers repository local
mvn clean deploy \
  -Drevision=1.0.0 \
  -Dchangelist="" \
  -Dsha1="" \
  -DaltDeploymentRepository=local::file:///tmp/maven-repo

# Vérifier le fichier déployé
ls -la /tmp/maven-repo/com/example/my-project/1.0.0/
# Doit contenir: my-project-1.0.0.jar, my-project-1.0.0.pom
```

### Vérifier qu'il n'y a pas de timestamp

```bash
# Vérifier le nom du JAR déployé
ls -la target/*.jar

# Doit être: my-project-1.0.0.jar
# PAS: my-project-1.0.0-20251229.091234-1.jar
```

## 📊 Comparaison des approches

### ❌ Sans CI-Friendly (Problématique)

| Aspect | Comportement |
|--------|--------------|
| Version dans `pom.xml` | `1.0.0-SNAPSHOT` |
| Version build CI | `1.0.0-20251229.091234-1` (timestamp Maven) |
| Version release | `1.0.0-SNAPSHOT` ou `1.0.0-20251229.091234-1` ❌ |
| POM publié | `<version>1.0.0-SNAPSHOT</version>` ❌ |
| Problème | Version instable en prod, timestamp illisible |

### ✅ Avec CI-Friendly (Solution)

| Aspect | Comportement |
|--------|--------------|
| Version dans `pom.xml` | `${revision}` |
| Version build CI | `1.0.0-ci-123-abc1234-SNAPSHOT` ✅ |
| Version release | `1.0.0` ✅ |
| POM publié | `<version>1.0.0</version>` ✅ |
| Avantage | Version stable, traçable, sans timestamp |

## 🎓 Résumé : Pourquoi votre problème arrive

### 1. Maven SNAPSHOT Deployment

Quand vous faites `mvn deploy` avec une version SNAPSHOT :

```bash
# Dans pom.xml: <version>1.0.0-SNAPSHOT</version>
mvn deploy
```

Maven déploie automatiquement :
```
1.0.0-20251229.091234-1.jar  ← TIMESTAMP MAVEN
```

C'est **normal** pour permettre plusieurs déploiements du même SNAPSHOT.

### 2. Réutilisation en Release

Si votre release workflow fait juste :

```bash
mvn deploy  # ❌ ERREUR
```

Sans changer la version, vous déployez **la version SNAPSHOT timestampée** au lieu de `1.0.0`.

### 3. Solution : Définir `revision` explicitement

```bash
# CI: version unique SANS deploy
mvn verify -Drevision=1.0.0-ci-123-abc1234-SNAPSHOT

# Release: version stable AVEC deploy
mvn deploy -Drevision=1.0.0 -Prelease
```

### 4. Flatten pour résoudre `${revision}`

Sans `flatten-maven-plugin`, le POM publié contient littéralement `${revision}`.

Avec `flatten-maven-plugin`, le POM publié contient la version résolue (`1.0.0`).

## 🚀 Migration depuis version existante

### Étape 1 : Modifier le pom.xml

```xml
<!-- Avant -->
<version>1.0.0-SNAPSHOT</version>

<!-- Après -->
<version>${revision}</version>
<properties>
    <revision>1.0.0-SNAPSHOT</revision>
</properties>
```

### Étape 2 : Ajouter flatten-maven-plugin

Voir `pom-example-ci-friendly.xml`

### Étape 3 : Créer les workflows

Copier `.github/workflows/ci.yml` et `.github/workflows/release.yml`

### Étape 4 : Configurer les secrets

```bash
# GitHub Settings > Secrets and variables > Actions

MAVEN_USERNAME       # Nexus/Artifactory username
MAVEN_PASSWORD       # Nexus/Artifactory password
GPG_PRIVATE_KEY      # GPG private key (pour signer les artifacts)
MAVEN_GPG_PASSPHRASE # GPG passphrase
```

### Étape 5 : Tester

```bash
# Test local
mvn clean verify -Drevision=1.0.0-test

# Push vers GitHub
git push

# Créer une release
git tag v1.0.0
git push origin v1.0.0
```

## ✅ Checklist finale

- [ ] `pom.xml` utilise `${revision}`
- [ ] `flatten-maven-plugin` configuré
- [ ] Workflow CI ne fait PAS de `deploy`
- [ ] Workflow Release valide la version (pas de SNAPSHOT, pas de timestamp)
- [ ] Workflow Release définit `-Drevision=X.Y.Z`
- [ ] Secrets GitHub configurés
- [ ] Tests locaux passent avec `-Drevision=...`
- [ ] Premier tag de release créé et testé

## 📚 Ressources

- [Maven CI-Friendly Versions](https://maven.apache.org/maven-ci-friendly.html)
- [Flatten Maven Plugin](https://www.mojohaus.org/flatten-maven-plugin/)
- [GitHub Actions - setup-java](https://github.com/actions/setup-java)
