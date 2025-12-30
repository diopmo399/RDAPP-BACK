# Guide de Démarrage Rapide

## Installation du plugin

```bash
cd openapi-gatling-maven-plugin
mvn clean install
```

## Utilisation dans votre projet

### 1. Ajouter le plugin au pom.xml

```xml
<plugin>
    <groupId>com.gatling</groupId>
    <artifactId>openapi-gatling-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals>
                <goal>generate-gatling-data</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/src/test/resources/openapi.yml</inputSpec>
                <outputDir>${project.build.directory}/gatling-data</outputDir>
                <rows>100</rows>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 2. Créer votre fichier OpenAPI

Créez `src/test/resources/openapi.yml` avec votre contrat OpenAPI 3.x.

### 3. Générer les données

```bash
mvn generate-test-resources
```

### 4. Fichiers générés

Les fichiers sont générés dans `target/gatling-data/` :

```
target/gatling-data/
├── schemas/
│   └── User.csv
├── endpoints/
│   ├── POST_users_request.csv
│   └── GET_users_request.csv
└── GatlingFeeders.scala
```

### 5. Utiliser dans Gatling

```scala
package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class UserSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080/api")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val createUserFeeder = csv("target/gatling-data/endpoints/POST_users_request.csv").circular

  val scenario = scenario("Create Users")
    .feed(createUserFeeder)
    .exec(
      http("Create User")
        .post("/users")
        .body(StringBody(session => session("body").as[String]))
        .check(status.is(201))
    )

  setUp(
    scenario.inject(rampUsers(100) during (60.seconds))
  ).protocols(httpProtocol)
}
```

## Exemple complet

Voir le projet `example-usage/` pour un exemple complet fonctionnel.

```bash
cd example-usage
mvn generate-test-resources
ls -la target/gatling-data/endpoints/
```

## Paramètres utiles

- `rows` : Nombre de lignes par feeder (défaut: 500)
- `format` : CSV, JSON, ou BOTH
- `seed` : Seed pour génération déterministe
- `generateScalaHelper` : Générer le helper Scala (défaut: true)

## Workflow complet

```bash
# 1. Installer le plugin
cd openapi-gatling-maven-plugin
mvn clean install

# 2. Tester avec l'exemple
cd example-usage
mvn generate-test-resources

# 3. Vérifier les fichiers générés
cat target/gatling-data/endpoints/POST_users_request.csv

# 4. Exécuter vos tests Gatling
mvn gatling:test
```

## Génération de données

Le plugin génère des données **réalistes et déterministes** :

- **Emails** : test0@example.com, test1@example.com...
- **Noms** : Jean Martin, Marie Bernard...
- **UUID** : Déterministe basé sur la seed
- **Dates** : 2025-01-01, 2025-01-02...
- **Téléphones** : +33600000000, +33600000001...

Tous les types OpenAPI sont supportés : string, integer, number, boolean, array, object.

## Résolution de problèmes

### Le plugin ne génère rien

Vérifiez que :
- Le fichier OpenAPI existe au chemin `inputSpec`
- Le fichier est un YAML valide OpenAPI 3.x
- Le goal est bien exécuté : `mvn generate-test-resources -X`

### Les feeders sont vides

Vérifiez que votre OpenAPI contient :
- Des `requestBody` pour les endpoints POST/PUT
- Des `parameters` pour les endpoints GET
- Des `schemas` dans `components`

### Erreur de compilation

```bash
# Nettoyer et recompiler
mvn clean compile
```

## Support

Pour plus d'informations, consultez le README.md complet.
