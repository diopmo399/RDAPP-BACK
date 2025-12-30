# Guide de Démarrage Rapide

## 1. Installation (5 minutes)

```bash
# Cloner ou naviguer vers le plugin
cd gatling-shadow-maven-plugin

# Installer le plugin dans votre repository Maven local
mvn clean install
```

✅ Le plugin est maintenant disponible pour tous vos projets Maven

## 2. Configuration de votre projet (2 minutes)

### Option A : Nouveau projet

Copiez le projet exemple :

```bash
cp -r gatling-shadow-maven-plugin/example-client my-gatling-project
cd my-gatling-project
```

### Option B : Projet existant

Ajoutez dans votre `pom.xml` :

```xml
<plugin>
    <groupId>com.gatling</groupId>
    <artifactId>gatling-shadow-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</plugin>
```

Assurez-vous d'avoir les dépendances Gatling :

```xml
<dependency>
    <groupId>io.gatling.highcharts</groupId>
    <artifactId>gatling-charts-highcharts</artifactId>
    <version>3.10.3</version>
    <scope>test</scope>
</dependency>
```

## 3. Créer une simulation (3 minutes)

Créez `src/test/scala/simulations/MyFirstSimulation.scala` :

```scala
package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class MyFirstSimulation extends Simulation {

  val httpProtocol = http.baseUrl("https://jsonplaceholder.typicode.com")

  val scn = scenario("My First Test")
    .exec(http("Get Users").get("/users").check(status.is(200)))
    .pause(1.second)
    .exec(http("Get Posts").get("/posts").check(status.is(200)))

  setUp(scn.inject(atOnceUsers(5))).protocols(httpProtocol)
}
```

## 4. Exécuter (1 minute)

```bash
# Tout en un : compiler, créer le JAR, exécuter
mvn clean test-compile gatling-shadow:shadow-test
```

🎉 C'est tout !

## Résultats

Les rapports sont générés dans :
```
target/gatling/reports/
```

Ouvrez `index.html` dans votre navigateur :

```bash
# macOS
open target/gatling/reports/*/index.html

# Linux
xdg-open target/gatling/reports/*/index.html

# Windows
start target/gatling/reports/*/index.html
```

## Commandes utiles

### Exécuter une simulation spécifique

```bash
mvn gatling-shadow:shadow-test -DsimulationClass=simulations.MyFirstSimulation
```

### Créer le JAR seulement (sans exécuter)

```bash
mvn gatling-shadow:shadow
```

### Exécuter avec plus de mémoire

```bash
mvn gatling-shadow:shadow-test -DjvmArgs="-Xms1g -Xmx4g"
```

### Debug

```bash
mvn gatling-shadow:shadow-test -X
```

## Tester l'exemple fourni

```bash
cd example-client
mvn clean test-compile
mvn gatling-shadow:shadow-test
```

Vous devriez voir :
```
=== Gatling Shadow-Test ===
1. Création du shadow JAR
2. Exécution de Gatling

--- Création du shadow JAR ---
...
Shadow JAR créé: gatling-shadow-example-1.0.0-SNAPSHOT-gatling-all.jar (XX MB)

--- Exécution de Gatling ---
Simulation: simulations.BasicSimulation
...
=== Shadow-Test terminé avec succès ===
```

## Workflow typique

```bash
# 1. Écrire/modifier vos simulations
vim src/test/scala/simulations/*.scala

# 2. Compiler
mvn test-compile

# 3. Tester
mvn gatling-shadow:shadow-test

# 4. Voir les résultats
open target/gatling/reports/*/index.html
```

## Configuration minimale vs complète

### Minimale (fonctionne directement)

```xml
<plugin>
    <groupId>com.gatling</groupId>
    <artifactId>gatling-shadow-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</plugin>
```

### Complète (personnalisée)

```xml
<plugin>
    <groupId>com.gatling</groupId>
    <artifactId>gatling-shadow-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <simulationClass>simulations.MySimulation</simulationClass>
        <jvmArgs>
            <jvmArg>-Xmx2g</jvmArg>
        </jvmArgs>
        <runDescription>Test de performance</runDescription>
    </configuration>
</plugin>
```

## Problèmes courants

### "Aucune simulation trouvée"

➡️ Compilez d'abord : `mvn test-compile`

### "Shadow JAR introuvable"

➡️ Utilisez `shadow-test` au lieu de `test`

### "NoClassDefFoundError"

➡️ Vérifiez que les dépendances Gatling sont en scope `test`

## Prochaines étapes

1. 📖 Lire le [README complet](README.md)
2. 🔧 Personnaliser la configuration
3. 🚀 Intégrer dans votre CI/CD
4. 📊 Analyser les rapports Gatling

---

**Temps total : ~11 minutes** ⏱️

Vous êtes prêt à tester vos APIs avec Gatling dans un environnement isolé !
