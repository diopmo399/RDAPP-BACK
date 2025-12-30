# Gatling Shadow JAR Maven Plugin

Plugin Maven qui construit un **shadow JAR** (uber-jar) contenant toutes les dépendances Gatling et vos simulations, puis exécute Gatling dans un environnement isolé.

## Avantages

✅ **Classpath isolé** : Aucun conflit de dépendances
✅ **JAR exécutable** : Distribuez et exécutez vos tests partout
✅ **Reproductible** : Même environnement à chaque exécution
✅ **Simple** : Un seul goal pour tout faire
✅ **Compatible** : Fonctionne avec Gatling 3.10+ et Java 17+

## Installation

### 1. Installer le plugin

```bash
cd gatling-shadow-maven-plugin
mvn clean install
```

### 2. Configurer dans votre projet

Ajoutez le plugin dans le `pom.xml` de votre projet de tests Gatling :

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.gatling</groupId>
            <artifactId>gatling-shadow-maven-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <configuration>
                <jvmArgs>
                    <jvmArg>-Xms512m</jvmArg>
                    <jvmArg>-Xmx2g</jvmArg>
                </jvmArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Goals disponibles

### `shadow`
Crée le shadow JAR avec toutes les dépendances.

```bash
mvn gatling-shadow:shadow
```

**Sortie** : `target/gatling-runner/<artifact>-gatling-all.jar`

### `test`
Exécute Gatling (nécessite que le shadow JAR existe).

```bash
mvn gatling-shadow:test
```

### `shadow-test` (recommandé)
Combine les deux : crée le JAR puis exécute Gatling.

```bash
mvn gatling-shadow:shadow-test
```

## Configuration complète

```xml
<plugin>
    <groupId>com.gatling</groupId>
    <artifactId>gatling-shadow-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <!-- Simulation spécifique à exécuter -->
        <simulationClass>simulations.BasicSimulation</simulationClass>

        <!-- Répertoires de sortie -->
        <outputDir>${project.build.directory}/gatling-runner</outputDir>
        <resultsDir>${project.build.directory}/gatling/results</resultsDir>
        <reportsDir>${project.build.directory}/gatling/reports</reportsDir>

        <!-- Nom personnalisé du shadow JAR -->
        <shadowJarName>my-gatling-tests.jar</shadowJarName>

        <!-- Options JVM -->
        <jvmArgs>
            <jvmArg>-Xms512m</jvmArg>
            <jvmArg>-Xmx2g</jvmArg>
            <jvmArg>-XX:+UseG1GC</jvmArg>
        </jvmArgs>

        <!-- Propriétés système -->
        <systemProps>
            <gatling.core.directory.results>${project.build.directory}/gatling/results</gatling.core.directory.results>
            <logback.configurationFile>logback.xml</logback.configurationFile>
        </systemProps>

        <!-- Variables d'environnement -->
        <env>
            <API_URL>http://localhost:8080</API_URL>
        </env>

        <!-- Description du run -->
        <runDescription>Test de charge prod-like</runDescription>

        <!-- Exécuter toutes les simulations trouvées -->
        <runAll>false</runAll>

        <!-- Filtres de simulations -->
        <includes>
            <include>simulations.*Simulation</include>
        </includes>
        <excludes>
            <exclude>.*Test.*</exclude>
        </excludes>

        <!-- Comportement -->
        <failOnError>true</failOnError>
        <nonInteractive>true</nonInteractive>
        <skip>false</skip>
    </configuration>
</plugin>
```

## Paramètres

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `simulationClass` | String | null | Classe de simulation spécifique (ex: `simulations.BasicSimulation`) |
| `outputDir` | File | `target/gatling-runner` | Répertoire du shadow JAR |
| `shadowJarName` | String | `<artifact>-<version>-gatling-all.jar` | Nom du JAR |
| `resultsDir` | File | `target/gatling/results` | Résultats Gatling |
| `reportsDir` | File | `target/gatling/reports` | Rapports HTML |
| `jvmArgs` | List | null | Arguments JVM (ex: `-Xmx2g`) |
| `systemProps` | Map | null | Propriétés système Java |
| `env` | Map | null | Variables d'environnement |
| `runDescription` | String | null | Description du test |
| `runAll` | boolean | false | Exécuter toutes les simulations |
| `includes` | List | null | Patterns de simulations à inclure |
| `excludes` | List | null | Patterns de simulations à exclure |
| `failOnError` | boolean | true | Échouer si Gatling échoue |
| `nonInteractive` | boolean | true | Mode non-interactif |
| `fork` | boolean | true | Exécuter dans un process séparé |
| `skip` | boolean | false | Ignorer l'exécution |

## Utilisation

### Scénario 1 : Exécution simple

```bash
# Tout en un : créer le JAR + exécuter les tests
mvn clean test-compile gatling-shadow:shadow-test
```

### Scénario 2 : Simulation spécifique

```bash
mvn gatling-shadow:shadow-test -DsimulationClass=simulations.AdvancedSimulation
```

### Scénario 3 : Exécuter toutes les simulations

```bash
mvn gatling-shadow:shadow-test -DrunAll=true
```

### Scénario 4 : Personnaliser la mémoire

```bash
mvn gatling-shadow:shadow-test -DjvmArgs="-Xms1g -Xmx4g"
```

### Scénario 5 : Créer le JAR sans exécuter

```bash
mvn gatling-shadow:shadow
```

Puis exécuter manuellement :

```bash
java -jar target/gatling-runner/<artifact>-gatling-all.jar \
  -s simulations.BasicSimulation \
  -rf target/gatling/results \
  -rsf target/gatling/reports
```

### Scénario 6 : Intégration CI/CD

```yaml
# .github/workflows/performance-tests.yml
- name: Run Gatling Tests
  run: mvn clean test-compile gatling-shadow:shadow-test -DfailOnError=true

- name: Upload Reports
  uses: actions/upload-artifact@v3
  with:
    name: gatling-reports
    path: target/gatling/reports/
```

## Structure du projet client

```
mon-projet-gatling/
├── pom.xml                           # Avec le plugin gatling-shadow
├── src/
│   └── test/
│       ├── scala/
│       │   └── simulations/
│       │       ├── BasicSimulation.scala
│       │       └── AdvancedSimulation.scala
│       └── resources/
│           ├── gatling.conf          # Config Gatling
│           ├── logback.xml           # Config logs
│           └── data/                 # Feeders CSV/JSON
└── target/
    ├── gatling-runner/
    │   └── mon-projet-1.0.0-gatling-all.jar
    └── gatling/
        ├── results/                  # Résultats bruts
        └── reports/                  # Rapports HTML
```

## Exemple complet

Un projet exemple complet est fourni dans `example-client/`.

### Exécuter l'exemple

```bash
# 1. Installer le plugin
cd gatling-shadow-maven-plugin
mvn clean install

# 2. Aller dans l'exemple
cd example-client

# 3. Compiler les simulations Scala
mvn clean test-compile

# 4. Créer le shadow JAR et exécuter
mvn gatling-shadow:shadow-test

# 5. Voir les rapports
open target/gatling/reports/index.html
```

## Débogage

### Activer les logs de debug

```bash
mvn gatling-shadow:shadow-test -X
```

### Vérifier le contenu du shadow JAR

```bash
jar tf target/gatling-runner/<artifact>-gatling-all.jar | grep -i gatling
```

### Lister les simulations détectées

```bash
mvn gatling-shadow:test -X 2>&1 | grep "Simulation trouvée"
```

### Problème : "Aucune simulation trouvée"

1. Vérifiez que les classes sont compilées :
   ```bash
   ls -la target/test-classes/simulations/
   ```

2. Vérifiez que la classe étend `Simulation` :
   ```scala
   class MySimulation extends Simulation { ... }
   ```

3. Compilez avant d'exécuter :
   ```bash
   mvn test-compile gatling-shadow:shadow-test
   ```

### Problème : "Shadow JAR introuvable"

Exécutez d'abord le goal `shadow` :
```bash
mvn gatling-shadow:shadow
mvn gatling-shadow:test
```

Ou utilisez `shadow-test` qui fait les deux :
```bash
mvn gatling-shadow:shadow-test
```

## Architecture du plugin

```
gatling-shadow-maven-plugin/
├── pom.xml
├── src/main/java/com/gatling/shadow/plugin/
│   ├── ShadowGatlingJarMojo.java          # Goal: shadow
│   ├── RunGatlingMojo.java                # Goal: test
│   ├── ShadowAndRunGatlingMojo.java       # Goal: shadow-test
│   ├── DependencyResolver.java            # Résout les dépendances
│   ├── ClasspathBuilder.java              # Construit le classpath
│   ├── SimulationScanner.java             # Détecte les simulations (ASM)
│   ├── ProcessRunner.java                 # Exécute Gatling (ProcessBuilder)
│   └── ShadowJarBuilder.java              # Crée l'uber-jar
└── example-client/                        # Projet exemple
```

## Comment ça marche ?

### Création du Shadow JAR

1. **Résolution des dépendances** : Le plugin récupère toutes les dépendances test + runtime du projet
2. **Extraction** : Chaque JAR de dépendance est décompressé
3. **Fusion** : Tous les fichiers sont combinés dans un seul JAR :
   - Classes compilées du projet (test + main)
   - Resources du projet
   - Toutes les dépendances
   - META-INF/services fusionnés (pour les SPI)
4. **Manifest** : Création avec `Main-Class: io.gatling.app.Gatling`
5. **Exclusions** : Signatures (*.SF, *.DSA, *.RSA) et manifests dupliqués

### Exécution de Gatling

1. **Scan des simulations** : Utilise ASM pour détecter les classes qui étendent `io.gatling.core.scenario.Simulation`
2. **Filtrage** : Applique les includes/excludes
3. **Commande Java** : Construit et exécute :
   ```
   java [jvmArgs] -cp <shadowJar> io.gatling.app.Gatling \
     -s <SimulationClass> \
     -rf <resultsDir> \
     -rsf <reportsDir> \
     -rd <runDescription> \
     -nr
   ```
4. **Capture de sortie** : Redirige stdout/stderr vers les logs Maven
5. **Code de sortie** : Propage l'échec si Gatling échoue

## Technologies utilisées

- **Java 17**
- **Maven Plugin API 3.8+**
- **ASM 9.6** : Analyse bytecode pour détecter les simulations
- **Plexus Archiver** : Manipulation de JARs
- **ProcessBuilder** : Exécution isolée de Gatling

## Compatibilité

- ✅ Maven 3.8+, 3.9+
- ✅ Java 17+
- ✅ Gatling 3.10+, 3.11+, 3.12+
- ✅ Scala 2.13+
- ✅ Simulations Scala (Java non testé mais devrait fonctionner)

## FAQ

### Différence avec gatling-maven-plugin ?

| Feature | gatling-maven-plugin | gatling-shadow-maven-plugin |
|---------|---------------------|----------------------------|
| Classpath | Maven classpath | Shadow JAR isolé |
| Conflits | Possibles | Aucun |
| Distribution | Non | JAR exécutable |
| Reproductibilité | Dépend de l'environnement | Garantie |

### Puis-je utiliser les deux plugins ?

Oui, ils sont compatibles et peuvent coexister dans le même projet.

### Le shadow JAR est-il réutilisable ?

Oui ! Une fois créé, vous pouvez l'exécuter n'importe où :

```bash
java -jar my-tests-gatling-all.jar -s MySimulation
```

### Taille du shadow JAR ?

Environ 50-100 MB selon vos dépendances. C'est normal, il contient :
- Gatling core + HTTP
- Scala runtime
- Netty, Akka, Jackson, etc.
- Vos simulations

## Licence

MIT

## Auteur

Plugin créé pour simplifier l'exécution de Gatling avec isolation du classpath.
