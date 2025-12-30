# GitHub Actions + Maven CI-Friendly Versioning - Guide Complet

## 🎯 Solution au problème des timestamps SNAPSHOT

Cette configuration **élimine complètement** le problème des versions SNAPSHOT timestampées en release.

## 📦 Fichiers fournis

```
.github/workflows/
  ├── ci.yml                          # Workflow CI (PR/push)
  └── release.yml                     # Workflow Release (tags)

pom-example-ci-friendly.xml           # Exemple pom.xml avec ${revision}
settings-example.xml                  # Exemple settings.xml (optionnel)
VERSIONING.md                         # Documentation complète
```

## 🚀 Mise en place (5 étapes)

### Étape 1 : Modifier votre `pom.xml`

Remplacez :

```xml
<version>1.0.0-SNAPSHOT</version>
```

Par :

```xml
<version>${revision}</version>

<properties>
    <revision>1.0.0-SNAPSHOT</revision>
    <changelist></changelist>
    <sha1></sha1>
</properties>
```

Ajoutez le `flatten-maven-plugin` :

```xml
<build>
    <plugins>
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

### Étape 2 : Copier les workflows

```bash
# Créer le dossier
mkdir -p .github/workflows

# Copier les workflows
cp ci.yml .github/workflows/
cp release.yml .github/workflows/
```

### Étape 3 : Configurer les secrets GitHub

Allez dans `Settings > Secrets and variables > Actions` et ajoutez :

| Secret | Description | Exemple |
|--------|-------------|---------|
| `MAVEN_USERNAME` | Nexus/Artifactory username | `myuser` |
| `MAVEN_PASSWORD` | Nexus/Artifactory password | `mypassword` |
| `GPG_PRIVATE_KEY` | GPG private key (pour signer) | `-----BEGIN PGP PRIVATE KEY BLOCK-----...` |
| `MAVEN_GPG_PASSPHRASE` | GPG passphrase | `mypassphrase` |

#### Générer une clé GPG (pour Maven Central)

```bash
# Générer la clé
gpg --gen-key

# Lister les clés
gpg --list-secret-keys --keyid-format LONG

# Exporter la clé privée
gpg --armor --export-secret-keys YOUR_KEY_ID

# Copier le résultat dans le secret GPG_PRIVATE_KEY
```

### Étape 4 : Tester localement

```bash
# Vérifier la version par défaut
mvn help:evaluate -Dexpression=project.version -q -DforceStdout

# Tester avec une version CI
mvn clean verify \
  -Drevision=1.0.0-ci-test-SNAPSHOT \
  -Dchangelist="" \
  -Dsha1=""

# Tester avec une version release
mvn clean verify \
  -Drevision=1.0.0 \
  -Dchangelist="" \
  -Dsha1=""

# Vérifier le POM résolu
cat target/.flattened-pom.xml | grep "<version>"
# Doit afficher: <version>1.0.0</version>
```

### Étape 5 : Créer votre première release

#### Option A : Via tag Git

```bash
# Créer et pusher le tag
git tag v1.0.0
git push origin v1.0.0

# Le workflow release.yml se déclenche automatiquement
```

#### Option B : Via workflow_dispatch

1. Allez dans `Actions > Release`
2. Cliquez sur `Run workflow`
3. Entrez la version : `1.0.0`
4. Cochez `Create Git tag` et `Publish GitHub Release`
5. Cliquez sur `Run workflow`

## 📊 Comment ça fonctionne

### Build CI (PR/push)

```yaml
# 1. Générer version unique
CI_VERSION="1.0.0-ci-123-abc1234-SNAPSHOT"

# 2. Build SANS deploy
mvn verify -Drevision=${CI_VERSION}

# 3. Vérifier pas de timestamp
if [[ $VERSION =~ [0-9]{8}\.[0-9]{6}-[0-9]+ ]]; then
  echo "ERROR: timestamp detected"
  exit 1
fi
```

**Résultat** :
- ✅ Version : `1.0.0-ci-123-abc1234-SNAPSHOT`
- ✅ Pas de timestamp Maven
- ✅ Pas de commit
- ✅ Pas de deploy

### Release (tag v1.0.0)

```yaml
# 1. Valider la version
VERSION="1.0.0"
# Vérifier: pas de SNAPSHOT, pas de timestamp

# 2. Build + Deploy
mvn deploy -Drevision=1.0.0 -Prelease

# 3. Vérifier version finale
MAVEN_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
# Doit être: 1.0.0
```

**Résultat** :
- ✅ Version : `1.0.0` (stable)
- ✅ POM publié : `<version>1.0.0</version>` (résolu par flatten)
- ✅ Artifacts signés (GPG)
- ✅ Tag Git : `v1.0.0`
- ✅ GitHub Release créée

## 🔍 Debug et vérification

### Vérifier la version effective

```bash
# Dans GitHub Actions logs, cherchez :
::notice::CI Version: 1.0.0-ci-123-abc1234-SNAPSHOT
::notice::Final Maven version: 1.0.0-ci-123-abc1234-SNAPSHOT

# Pour release :
::notice::Release Version: 1.0.0
::notice::Maven version: 1.0.0
```

### Vérifier le POM publié

Après une release, téléchargez le POM depuis Maven Central :

```bash
curl https://repo1.maven.org/maven2/com/example/my-project/1.0.0/my-project-1.0.0.pom

# Vérifier la version
cat my-project-1.0.0.pom | grep "<version>"
# Doit afficher: <version>1.0.0</version>
# PAS: <version>${revision}</version>
```

### Vérifier qu'il n'y a pas de timestamp

```bash
# Lister les artifacts déployés
ls -la ~/.m2/repository/com/example/my-project/1.0.0/

# Doit contenir:
my-project-1.0.0.jar
my-project-1.0.0.pom
my-project-1.0.0-sources.jar
my-project-1.0.0-javadoc.jar

# PAS:
my-project-1.0.0-20251229.091234-1.jar  ❌
```

## ❓ FAQ

### Q1 : Pourquoi `flatten-maven-plugin` est nécessaire ?

**R** : Sans flatten, le POM publié contient littéralement `${revision}` au lieu de `1.0.0`.

Les consommateurs de votre library ne pourront pas résoudre la version.

Avec flatten, le POM publié contient la version résolue.

### Q2 : Pourquoi les builds CI ne font pas de `deploy` ?

**R** : Pour **éviter les timestamps Maven**.

Quand vous faites `mvn deploy` avec SNAPSHOT, Maven ajoute automatiquement un timestamp :

```
1.0.0-SNAPSHOT → 1.0.0-20251229.091234-1
```

En CI, on fait seulement `mvn verify` (build + test) sans deploy.

### Q3 : Comment gérer les versions RC (Release Candidate) ?

**R** : Créez un tag avec suffix :

```bash
git tag v1.0.0-rc.1
git push origin v1.0.0-rc.1
```

Le workflow release détecte automatiquement les RC et marque la GitHub Release comme `prerelease`.

### Q4 : Peut-on utiliser cette approche pour multi-modules ?

**R** : Oui ! Définissez `${revision}` dans le parent POM :

```xml
<!-- Parent POM -->
<version>${revision}</version>
<properties>
    <revision>1.0.0-SNAPSHOT</revision>
</properties>

<!-- Module enfant -->
<parent>
    <groupId>com.example</groupId>
    <artifactId>parent</artifactId>
    <version>${revision}</version>
</parent>

<artifactId>module-1</artifactId>
<!-- Hérite la version du parent -->
```

### Q5 : Comment rollback une release ratée ?

**R** : Supprimez le tag et la release GitHub :

```bash
# Supprimer le tag localement
git tag -d v1.0.0

# Supprimer le tag sur GitHub
git push origin :refs/tags/v1.0.0

# Supprimer la GitHub Release via UI
```

Ensuite, supprimez les artifacts déployés sur Maven Central (nécessite un ticket Sonatype).

## ✅ Validation finale

Vérifiez que :

- [ ] `pom.xml` contient `<version>${revision}</version>`
- [ ] `flatten-maven-plugin` est configuré
- [ ] `.github/workflows/ci.yml` existe
- [ ] `.github/workflows/release.yml` existe
- [ ] Secrets GitHub configurés (MAVEN_USERNAME, etc.)
- [ ] Test local réussi : `mvn verify -Drevision=1.0.0-test`
- [ ] POM résolu correct : `cat target/.flattened-pom.xml | grep version`
- [ ] Premier tag créé : `git tag v1.0.0 && git push origin v1.0.0`
- [ ] Workflow release déclenché et réussi
- [ ] Artifacts déployés sur Maven Central
- [ ] GitHub Release créée

## 📚 Documentation complète

Lisez `VERSIONING.md` pour :
- Explication détaillée du problème
- Architecture de la solution
- Comparaison avant/après
- Commandes de debug
- Ressources

## 🎉 Résumé

Avec cette configuration :

✅ **Builds CI** : versions uniques `1.0.0-ci-123-abc1234-SNAPSHOT` sans timestamp
✅ **Releases** : versions stables `1.0.0` sans SNAPSHOT, sans timestamp
✅ **POMs publiés** : versions résolues (pas `${revision}`)
✅ **Traçabilité** : chaque build a une version unique avec commit SHA
✅ **Reproductibilité** : même version = même code source

**Fini les timestamps Maven dans les releases !** 🚀
