# Architecture du Plugin

## Vue d'ensemble

Le plugin `gatling-shadow-maven-plugin` résout le problème de l'isolation du classpath pour Gatling en créant un **uber-JAR** (shadow JAR) autonome contenant toutes les dépendances.

## Flux d'exécution

```
┌─────────────────────────────────────────────────────────────┐
│                    Projet Client Maven                       │
│  - Simulations Gatling (src/test/scala/simulations/)        │
│  - Dépendances Gatling (pom.xml)                            │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
         ┌──────────────────────────────┐
         │   GOAL: shadow-test          │
         │ (ShadowAndRunGatlingMojo)    │
         └──────┬───────────────┬───────┘
                │               │
       ┌────────▼──────┐   ┌───▼────────────┐
       │  ÉTAPE 1:     │   │  ÉTAPE 2:      │
       │  CREATE JAR   │   │  RUN GATLING   │
       └────────┬──────┘   └───┬────────────┘
                │              │
     ┌──────────▼────────┐     │
     │ DependencyResolver│     │
     │ - Résout deps     │     │
     │ - Filtre Gatling  │     │
     └──────────┬────────┘     │
                │              │
     ┌──────────▼────────┐     │
     │ ShadowJarBuilder  │     │
     │ - Crée uber-JAR   │     │
     │ - Merge JARs      │     │
     │ - Manifest        │     │
     └──────────┬────────┘     │
                │              │
                ▼              │
    target/gatling-runner/    │
    <artifact>-gatling-all.jar│
                               │
                ┌──────────────▼────────┐
                │ SimulationScanner     │
                │ - Scan classes (ASM)  │
                │ - Détecte Simulations │
                └──────────┬────────────┘
                           │
                ┌──────────▼────────┐
                │ ProcessRunner     │
                │ - ProcessBuilder  │
                │ - java -cp JAR... │
                │ - Gatling CLI     │
                └──────────┬────────┘
                           │
                           ▼
                  Rapports Gatling
                target/gatling/reports/
```

## Composants principaux

### 1. Mojos (Points d'entrée)

#### `ShadowGatlingJarMojo` (goal: `shadow`)
- **Responsabilité** : Créer le shadow JAR
- **Phase Maven** : `package`
- **Dépendances** : `DependencyResolver`, `ShadowJarBuilder`

```java
@Mojo(name = "shadow", defaultPhase = LifecyclePhase.PACKAGE)
public class ShadowGatlingJarMojo extends AbstractMojo {
    // 1. Résoudre dépendances
    // 2. Créer shadow JAR
    // 3. Stocker chemin dans propriétés projet
}
```

#### `RunGatlingMojo` (goal: `test`)
- **Responsabilité** : Exécuter Gatling depuis le shadow JAR
- **Phase Maven** : `integration-test`
- **Dépendances** : `SimulationScanner`, `ProcessRunner`

```java
@Mojo(name = "test", defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class RunGatlingMojo extends AbstractMojo {
    // 1. Trouver shadow JAR
    // 2. Scanner simulations
    // 3. Exécuter Gatling
}
```

#### `ShadowAndRunGatlingMojo` (goal: `shadow-test`)
- **Responsabilité** : Combiner shadow + test
- **Phase Maven** : `integration-test`
- **Avantage** : Un seul goal pour tout faire

### 2. Classes utilitaires

#### `DependencyResolver`
**Rôle** : Résoudre les dépendances du projet Maven

```java
public class DependencyResolver {
    // Utilise MavenProject.getArtifacts()
    // Filtre par scope (compile, test, runtime)
    // Identifie les dépendances Gatling

    Set<File> resolveGatlingDependencies() {
        // Parcourt tous les artifacts
        // Filtre io.gatling.*, scala-*, netty, etc.
        // Retourne Set<File> des JARs
    }
}
```

**Filtres appliqués** :
- Scope : `compile`, `runtime`, `test`
- GroupId : `io.gatling.*`, `org.scala-lang.*`
- ArtifactId : `netty*`, `akka*`, `jackson*`, `logback*`, `slf4j*`

#### `ClasspathBuilder`
**Rôle** : Construire le classpath complet

```java
public class ClasspathBuilder {
    String buildFullClasspath(Set<File> dependencies) {
        // 1. target/test-classes
        // 2. target/classes
        // 3. src/test/resources
        // 4. Dependencies JARs
        // Retour: String avec File.pathSeparator
    }
}
```

#### `SimulationScanner`
**Rôle** : Détecter les simulations Gatling via ASM

```java
public class SimulationScanner {
    List<String> scanSimulations() throws IOException {
        // Walk target/test-classes
        // Pour chaque .class :
        //   - ClassReader (ASM)
        //   - Vérifier superclass = io/gatling/core/scenario/Simulation
        //   - Appliquer includes/excludes
        // Retourner List<String> de FQCNs
    }
}
```

**Détection ASM** :
```java
class SimulationClassVisitor extends ClassVisitor {
    public void visit(int version, int access, String name,
                     String signature, String superName, String[] interfaces) {
        if (superName.equals("io/gatling/core/scenario/Simulation")) {
            isSimulation = true;
        }
    }
}
```

#### `ShadowJarBuilder`
**Rôle** : Créer l'uber-JAR avec fusion de dépendances

```java
public class ShadowJarBuilder {
    File buildShadowJar(File outputJar, Set<File> dependencies) {
        try (JarOutputStream jos = new JarOutputStream(..., createManifest())) {
            // 1. Ajouter target/test-classes
            addDirectoryToJar(jos, testClasses, "");

            // 2. Ajouter target/classes
            addDirectoryToJar(jos, mainClasses, "");

            // 3. Ajouter resources
            addDirectoryToJar(jos, testResources, "");

            // 4. Ajouter chaque dependency JAR
            for (File dep : dependencies) {
                addJarToJar(jos, dep); // Décompresse et merge
            }

            // 5. Écrire META-INF/services fusionnés
            writeMergedServices(jos);
        }
    }
}
```

**Exclusions** :
- Signatures : `*.SF`, `*.DSA`, `*.RSA`, `*.EC`
- Manifests : `META-INF/MANIFEST.MF` (on utilise le nôtre)
- Module Java 9+ : `module-info.class`, `META-INF/versions/`

**Manifest généré** :
```
Manifest-Version: 1.0
Main-Class: io.gatling.app.Gatling
Created-By: Gatling Shadow Maven Plugin
Built-By: <user>
Build-Date: <timestamp>
```

**Gestion META-INF/services** :
Les fichiers SPI sont fusionnés (pas écrasés) :
```java
// META-INF/services/com.example.Service
// JAR1: impl.Service1
// JAR2: impl.Service2
// Résultat: impl.Service1 + impl.Service2
```

#### `ProcessRunner`
**Rôle** : Exécuter Gatling via `ProcessBuilder`

```java
public class ProcessRunner {
    int runGatling(File shadowJar, String simulationClass, ...) {
        // Construire la commande
        List<String> cmd = buildGatlingCommand(...);

        // ProcessBuilder
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        // Démarrer le process
        Process process = pb.start();

        // Capturer stdout/stderr
        BufferedReader reader = ...;
        while ((line = reader.readLine()) != null) {
            log.info(line);
        }

        // Attendre et retourner exit code
        return process.waitFor();
    }
}
```

**Commande générée** :
```bash
/path/to/java \
  -Xms512m -Xmx2g \                    # JVM args
  -Dgatling.core.directory.results=... # System props
  -cp /path/to/shadow-all.jar \        # Classpath
  io.gatling.app.Gatling \             # Main class
  -s simulations.BasicSimulation \     # Simulation
  -rf target/gatling/results \         # Results folder
  -rsf target/gatling/reports \        # Reports folder
  -rd "Run description" \              # Description
  -nr                                  # Non-interactive
```

## Flux de données

### Création du Shadow JAR

```
MavenProject
    │
    ├─> project.getArtifacts()
    │       │
    │       ▼
    │   DependencyResolver
    │       │
    │       ├─> Filter by scope (test, runtime, compile)
    │       ├─> Filter by groupId (io.gatling.*, scala-*, ...)
    │       │
    │       ▼
    │   Set<File> dependencies
    │
    ├─> project.getBuild().getTestOutputDirectory()
    │       │
    │       ▼
    │   target/test-classes/
    │
    ├─> project.getBuild().getOutputDirectory()
    │       │
    │       ▼
    │   target/classes/
    │
    └─> ALL FILES
            │
            ▼
        ShadowJarBuilder
            │
            ├─> Create JarOutputStream
            ├─> Add test classes
            ├─> Add main classes
            ├─> For each dependency JAR:
            │       ├─> Open ZipInputStream
            │       ├─> For each entry:
            │       │       ├─> Exclude signatures
            │       │       ├─> Merge META-INF/services
            │       │       └─> Add to uber-JAR
            ├─> Write merged services
            └─> Close JAR
                    │
                    ▼
            target/gatling-runner/
            <artifact>-gatling-all.jar
```

### Exécution de Gatling

```
RunGatlingMojo
    │
    ├─> Get shadow JAR path
    │       │
    │       ├─> From project.properties["gatling.shadowJar"]
    │       └─> Or default: target/gatling-runner/<artifact>-gatling-all.jar
    │
    ├─> SimulationScanner
    │       │
    │       ├─> Walk target/test-classes/
    │       ├─> For each .class:
    │       │       ├─> ClassReader (ASM)
    │       │       └─> Check superName = "io/gatling/core/scenario/Simulation"
    │       │
    │       └─> List<String> simulations
    │
    ├─> Filter simulations
    │       │
    │       ├─> Apply includes/excludes patterns
    │       └─> If !runAll, keep only first
    │
    └─> ProcessRunner
            │
            ├─> Build command
            │       java -cp <jar> io.gatling.app.Gatling -s <sim> ...
            │
            ├─> ProcessBuilder.start()
            ├─> Capture stdout/stderr
            ├─> Wait for exit
            │
            └─> Return exit code
```

## Gestion des erreurs

### DependencyResolver
```java
if (artifacts == null || artifacts.isEmpty()) {
    log.warn("Aucune dépendance trouvée");
    return emptySet();
}
```

### SimulationScanner
```java
if (simulations.isEmpty()) {
    log.warn("Aucune simulation Gatling trouvée !");
}
```

### ProcessRunner
```java
int exitCode = process.waitFor();
if (exitCode != 0 && failOnError) {
    throw new MojoExecutionException("Gatling failed with code: " + exitCode);
}
```

### ShadowJarBuilder
```java
private boolean shouldExcludeEntry(String entryName) {
    // Signatures
    if (entryName.endsWith(".SF") || entryName.endsWith(".DSA") || ...) {
        return true;
    }
    // Manifests dupliqués
    if (entryName.equals("META-INF/MANIFEST.MF")) {
        return true;
    }
    return false;
}
```

## Optimisations

### 1. Set pour éviter les doublons
```java
private final Set<String> addedEntries = new HashSet<>();

if (!addedEntries.contains(entryName)) {
    jos.putNextEntry(jarEntry);
    addedEntries.add(entryName);
}
```

### 2. BufferedOutputStream pour performance
```java
new JarOutputStream(
    new BufferedOutputStream(new FileOutputStream(outputJar)),
    manifest
)
```

### 3. Scan ASM optimisé
```java
ClassReader reader = new ClassReader(fis);
reader.accept(visitor,
    ClassReader.SKIP_CODE |
    ClassReader.SKIP_DEBUG |
    ClassReader.SKIP_FRAMES
);
```

### 4. Propriétés Maven pour partage
```java
// ShadowGatlingJarMojo
project.getProperties().setProperty("gatling.shadowJar", jarPath);

// RunGatlingMojo
String jarPath = project.getProperties().getProperty("gatling.shadowJar");
```

## Tests et validation

### Vérifier le shadow JAR
```bash
# Lister le contenu
jar tf target/gatling-runner/*-gatling-all.jar | head -20

# Vérifier Gatling
jar tf target/gatling-runner/*-gatling-all.jar | grep -i "io/gatling"

# Vérifier vos simulations
jar tf target/gatling-runner/*-gatling-all.jar | grep simulations

# Vérifier le manifest
unzip -p target/gatling-runner/*-gatling-all.jar META-INF/MANIFEST.MF
```

### Exécuter manuellement
```bash
java -jar target/gatling-runner/*-gatling-all.jar \
  -s simulations.BasicSimulation \
  -rf target/gatling/results \
  -rsf target/gatling/reports
```

## Comparaison avec Maven Shade Plugin

| Aspect | Maven Shade Plugin | Notre implémentation |
|--------|-------------------|---------------------|
| Dépendances | Plugin externe | Code intégré |
| Contrôle | Configuration XML | Code Java |
| Personnalisation | Limitée | Totale |
| Debug | Difficile | Facile (logs) |
| Performance | Optimisé | Suffisant |

## Dépendances techniques

```xml
<!-- ASM pour analyse bytecode -->
<dependency>
    <groupId>org.ow2.asm</groupId>
    <artifactId>asm</artifactId>
    <version>9.6</version>
</dependency>

<!-- Plexus Archiver pour JARs -->
<dependency>
    <groupId>org.codehaus.plexus</groupId>
    <artifactId>plexus-archiver</artifactId>
    <version>4.9.1</version>
</dependency>

<!-- Maven Plugin API -->
<dependency>
    <groupId>org.apache.maven</groupId>
    <artifactId>maven-plugin-api</artifactId>
    <version>3.8.1</version>
</dependency>
```

## Conclusion

Le plugin fonctionne en **deux étapes séquentielles** :

1. **Création du shadow JAR** : Fusion de toutes les dépendances dans un seul JAR autonome
2. **Exécution de Gatling** : Lancement via ProcessBuilder avec classpath isolé

Cette approche garantit :
- ✅ Isolation complète du classpath
- ✅ Reproductibilité des tests
- ✅ Distribution facile (un seul JAR)
- ✅ Compatibilité avec tous les environnements
