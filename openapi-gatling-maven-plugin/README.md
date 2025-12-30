# OpenAPI Gatling Maven Plugin

Plugin Maven pour générer automatiquement des données de test Gatling (feeders CSV/JSON) à partir d'un contrat OpenAPI 3.x.

## Fonctionnalités

- Parse les fichiers OpenAPI 3.x (YAML)
- Génère des feeders CSV et/ou JSON pour Gatling
- Génère des données réalistes et déterministes (basées sur une seed)
- Supporte tous les types OpenAPI : string, integer, number, boolean, array, object
- Supporte les formats : email, uuid, date, date-time, phone, uri
- Résout les références $ref
- Gère allOf, oneOf, anyOf
- Respecte les contraintes : min/max, minLength/maxLength, enum, required
- Génère un helper Scala optionnel pour faciliter l'utilisation des feeders

## Installation

### 1. Compiler et installer le plugin

```bash
cd openapi-gatling-maven-plugin
mvn clean install
```

### 2. Utiliser le plugin dans votre projet

Ajoutez le plugin dans votre `pom.xml` :

```xml
<build>
    <plugins>
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
                        <rows>500</rows>
                        <format>CSV</format>
                        <seed>12345</seed>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Configuration

### Paramètres disponibles

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `inputSpec` | String | **requis** | Chemin vers le fichier OpenAPI YAML |
| `outputDir` | File | `target/gatling-data` | Répertoire de sortie |
| `rows` | int | `500` | Nombre de lignes par feeder |
| `format` | enum | `CSV` | Format de sortie : CSV, JSON, BOTH |
| `seed` | long | `12345` | Seed pour génération déterministe |
| `includeSchemas` | List<String> | null | Filtre schemas à inclure |
| `includePaths` | List<String> | null | Filtre paths à inclure |
| `arraysMaxSize` | int | `3` | Taille max des arrays |
| `overwrite` | boolean | `true` | Écraser fichiers existants |
| `jsonColumnName` | String | `body` | Nom de la colonne pour le JSON |
| `addCorrelationColumns` | boolean | `true` | Ajouter colonnes corrélation |
| `generateScalaHelper` | boolean | `true` | Générer helper Scala |

### Exemple de configuration complète

```xml
<configuration>
    <inputSpec>src/test/resources/openapi.yml</inputSpec>
    <outputDir>target/gatling-data</outputDir>
    <rows>1000</rows>
    <format>CSV</format>
    <seed>98765</seed>
    <arraysMaxSize>5</arraysMaxSize>
    <jsonColumnName>requestBody</jsonColumnName>
    <addCorrelationColumns>true</addCorrelationColumns>
    <generateScalaHelper>true</generateScalaHelper>
    <includePaths>
        <includePath>/users</includePath>
        <includePath>/orders</includePath>
    </includePaths>
</configuration>
```

## Utilisation

### 1. Générer les données

```bash
mvn generate-test-resources
```

Ou exécuter directement le goal :

```bash
mvn com.gatling:openapi-gatling-maven-plugin:generate-gatling-data
```

### 2. Structure des fichiers générés

```
target/gatling-data/
├── schemas/
│   ├── User.csv
│   ├── Order.csv
│   └── OrderItem.csv
├── endpoints/
│   ├── POST_users_request.csv
│   ├── GET_users_request.csv
│   ├── PUT_users_userId_request.csv
│   └── POST_orders_request.csv
└── GatlingFeeders.scala
```

### 3. Utiliser les feeders dans Gatling

#### Option 1 : Utilisation directe

```scala
package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class UserSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080/api")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // Charger le feeder
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

#### Option 2 : Avec le helper généré

```scala
import helpers.GatlingFeeders._

class UserSimulation extends Simulation {

  val scenario = scenario("Create Users")
    .feed(POST_users_request)  // Utilisation du helper
    .exec(
      http("Create User")
        .post("/users")
        .body(jsonBodyFrom("body"))  // Méthode helper
        .check(status.is(201))
    )
}
```

### 4. Exemples de feeders générés

#### Exemple CSV (POST_users_request.csv)

```csv
email,firstName,lastName,age,phone,body,correlationId,userId
test0@example.com,Jean,Martin,18,+33600000000,"{""email"":""test0@example.com"",""firstName"":""Jean"",""lastName"":""Martin"",""age"":18}",corr_0,user_0
test1@example.com,Marie,Bernard,19,+33600000001,"{""email"":""test1@example.com"",""firstName"":""Marie"",""lastName"":""Bernard"",""age"":19}",corr_1,user_1
```

#### Exemple JSON

```json
[
  {
    "email": "test0@example.com",
    "firstName": "Jean",
    "lastName": "Martin",
    "age": 18,
    "body": "{\"email\":\"test0@example.com\",\"firstName\":\"Jean\",\"lastName\":\"Martin\",\"age\":18}"
  }
]
```

## Génération de données

### Types supportés

- **string** : génération intelligente basée sur le nom du champ
  - `email` → test0@example.com
  - `firstName` / `prenom` → Jean, Marie, Pierre...
  - `lastName` / `nom` → Martin, Bernard, Dubois...
  - `city` / `ville` → Paris, Lyon, Marseille...
  - `phone` / `tel` → +33600000000

- **integer** : respecte min/max
- **number** : respecte min/max
- **boolean** : alterne true/false
- **array** : génère 1-3 éléments (configurable)
- **object** : génère récursivement

### Formats supportés

- `email` → test{index}@example.com
- `uuid` → UUID déterministe
- `date` → 2025-01-01 + index jours
- `date-time` → ISO-8601
- `uri` / `url` → https://example.com/resource/{index}
- `phone` → +33{9 digits}

### Contraintes respectées

- `enum` : sélection aléatoire déterministe
- `minimum` / `maximum`
- `minLength` / `maxLength`
- `minItems` / `maxItems`
- `required`

## Tests

Exécuter les tests du plugin :

```bash
cd openapi-gatling-maven-plugin
mvn test
```

## Exemple complet

Un projet exemple complet est disponible dans `example-usage/`.

### Exécuter l'exemple

```bash
cd example-usage

# 1. Installer le plugin parent (si pas déjà fait)
cd ..
mvn clean install
cd example-usage

# 2. Générer les données
mvn generate-test-resources

# 3. Vérifier les fichiers générés
ls -la target/gatling-data/endpoints/

# 4. Exécuter la simulation Gatling (nécessite un serveur running)
mvn gatling:test
```

## Structure du code

```
openapi-gatling-maven-plugin/
├── src/main/java/com/gatling/openapi/plugin/
│   ├── GenerateGatlingDataMojo.java       # Mojo principal
│   ├── OpenApiLoader.java                 # Chargement OpenAPI
│   ├── RefResolver.java                   # Résolution $ref
│   ├── SchemaExampleGenerator.java        # Génération données
│   ├── EndpointDatasetGenerator.java      # Génération endpoints
│   ├── CsvWriter.java                     # Écriture CSV
│   ├── JsonWriter.java                    # Écriture JSON
│   └── ScalaHelperGenerator.java          # Génération helper Scala
├── src/test/
│   ├── java/                              # Tests JUnit
│   └── resources/
│       └── test-openapi.yml               # OpenAPI de test
└── example-usage/                         # Projet exemple
```

## Dépannage

### Le plugin ne trouve pas le fichier OpenAPI

Vérifiez que le chemin dans `inputSpec` est correct :

```xml
<inputSpec>${project.basedir}/src/test/resources/openapi.yml</inputSpec>
```

### Les feeders ne sont pas générés

Vérifiez que le goal est bien exécuté :

```bash
mvn generate-test-resources -X
```

### Erreur lors du parsing OpenAPI

Vérifiez que votre fichier OpenAPI est valide :
- Utilisez https://editor.swagger.io/
- Vérifiez la version OpenAPI (doit être 3.x)

## Licence

MIT

## Auteur

Généré pour les tests de performance Gatling avec données réalistes.
