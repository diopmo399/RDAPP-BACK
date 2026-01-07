# Détection Automatique du Langage (Scala/Java)

## 🎯 Vue d'ensemble

Le plugin OpenAPI Gatling Maven Plugin détecte automatiquement si votre projet utilise **Scala ou Java** pour Gatling, et génère le helper approprié (`GatlingFeeders.scala` ou `GatlingFeeders.java`).

## ✨ Fonctionnalités

- ✅ **Détection automatique** du langage basée sur :
  - Les dépendances Maven
  - Les plugins Maven
  - Les répertoires sources
- ✅ **Support Scala** : Génère `GatlingFeeders.scala` avec l'API Scala de Gatling
- ✅ **Support Java** : Génère `GatlingFeeders.java` avec l'API Java de Gatling
- ✅ **Configuration manuelle** optionnelle pour forcer un langage spécifique

## 🔍 Comment fonctionne la détection ?

Le plugin essaie plusieurs méthodes dans l'ordre suivant :

### 1. **Détection via les dépendances Maven** (Priorité élevée)

```xml
<!-- Projet Scala détecté -->
<dependency>
    <groupId>org.scala-lang</groupId>
    <artifactId>scala-library</artifactId>
    <version>2.13.12</version>
</dependency>

<!-- OU -->
<dependency>
    <groupId>io.gatling.highcharts</groupId>
    <artifactId>gatling-charts-highcharts</artifactId>  <!-- Scala -->
    <version>3.10.3</version>
</dependency>
```

```xml
<!-- Projet Java détecté -->
<dependency>
    <groupId>io.gatling</groupId>
    <artifactId>gatling-javaapi</artifactId>
    <version>3.10.3</version>
</dependency>
```

### 2. **Détection via les plugins Maven**

```xml
<!-- Scala détecté -->
<plugin>
    <groupId>net.alchim31.maven</groupId>
    <artifactId>scala-maven-plugin</artifactId>
</plugin>
```

```xml
<!-- Plugin Gatling (Scala par défaut) -->
<plugin>
    <groupId>io.gatling</groupId>
    <artifactId>gatling-maven-plugin</artifactId>
</plugin>
```

### 3. **Détection via les répertoires sources**

Le plugin vérifie l'existence de fichiers :
- `src/test/scala/**/*.scala` → Scala détecté
- `src/test/java/**/*.java` → Java détecté

### 4. **Par défaut : Scala**

Si aucune détection n'aboutit, le plugin utilise **Scala** par défaut (rétrocompatibilité).

## 📖 Utilisation

### Détection automatique (recommandé)

Aucune configuration nécessaire ! Le plugin détecte automatiquement :

```xml
<plugin>
    <groupId>com.gatling</groupId>
    <artifactId>openapi-gatling-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <inputSpec>src/main/resources/openapi.yaml</inputSpec>
        <generateScalaHelper>true</generateScalaHelper>
        <!-- Détection automatique -->
    </configuration>
</plugin>
```

**Sortie console** :
```
[INFO] → Détection automatique du langage...
[INFO]   ✓ Java détecté via dépendance Gatling Java: gatling-javaapi
[INFO] Langage cible détecté: java
[INFO] Génération du helper Java...
[INFO]   ✓ Fichier généré: GatlingFeeders.java
```

### Configuration manuelle (optionnel)

Pour forcer un langage spécifique :

```xml
<plugin>
    <groupId>com.gatling</groupId>
    <artifactId>openapi-gatling-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <inputSpec>src/main/resources/openapi.yaml</inputSpec>
        <language>java</language>  <!-- Forcer Java -->
    </configuration>
</plugin>
```

**Valeurs possibles** :
- `scala` : Force la génération du helper Scala
- `java` : Force la génération du helper Java

**En ligne de commande** :
```bash
mvn generate-gatling-data -Dlanguage=java
```

## 📊 Exemples de helpers générés

### Helper Scala (`GatlingFeeders.scala`)

```scala
package helpers

import io.gatling.core.Predef._
import io.gatling.core.feeder._
import io.gatling.core.body.StringBody

object GatlingFeeders {

  def get_users: RecordSeqFeederBuilder[String] = {
    csv("target/gatling-data/endpoints/get_users.csv").circular
  }

  def post_users: RecordSeqFeederBuilder[String] = {
    csv("target/gatling-data/endpoints/post_users.csv").circular
  }

  def jsonBodyFrom(columnName: String = "body"): StringBody = {
    StringBody(session => session(columnName).as[String])
  }

  def customFeeder(path: String): RecordSeqFeederBuilder[String] = {
    csv(path).circular
  }
}
```

**Utilisation dans un scénario Scala** :
```scala
import helpers.GatlingFeeders._

val scn = scenario("User API Test")
  .feed(get_users)
  .exec(http("Get Users")
    .get("/users")
  )
  .feed(post_users)
  .exec(http("Create User")
    .post("/users")
    .body(jsonBodyFrom())
  )
```

### Helper Java (`GatlingFeeders.java`)

```java
package helpers;

import io.gatling.javaapi.core.*;
import static io.gatling.javaapi.core.CoreDsl.*;

public class GatlingFeeders {

    private GatlingFeeders() {
        // Classe utilitaire
    }

    public static FeederBuilder<String> getUsers() {
        return csv("target/gatling-data/endpoints/get_users.csv").circular();
    }

    public static FeederBuilder<String> postUsers() {
        return csv("target/gatling-data/endpoints/post_users.csv").circular();
    }

    public static Body.WithString jsonBodyFrom(String columnName) {
        return StringBody(session -> session.getString(columnName));
    }

    public static Body.WithString jsonBody() {
        return jsonBodyFrom("body");
    }

    public static String[] getAvailableFeeders() {
        return new String[] {
            "get_users",
            "post_users"
        };
    }
}
```

**Utilisation dans un scénario Java** :
```java
import helpers.GatlingFeeders;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

ScenarioBuilder scn = scenario("User API Test")
    .feed(GatlingFeeders.getUsers())
    .exec(http("Get Users")
        .get("/users")
    )
    .feed(GatlingFeeders.postUsers())
    .exec(http("Create User")
        .post("/users")
        .body(GatlingFeeders.jsonBody())
    );
```

## 🎨 Différences entre Scala et Java

| Aspect | Scala | Java |
|--------|-------|------|
| **Fichier généré** | `GatlingFeeders.scala` | `GatlingFeeders.java` |
| **Structure** | Object Scala | Classe statique Java |
| **Naming** | snake_case | camelCase |
| **API Gatling** | `io.gatling.core` | `io.gatling.javaapi.core` |
| **Body** | `StringBody(session => ...)` | `StringBody(session -> ...)` |
| **Feeder** | `csv(...).circular` | `csv(...).circular()` |

## 🐛 Dépannage

### Le mauvais langage est détecté

**Symptôme** : Le plugin génère un helper Scala alors que vous utilisez Java (ou vice-versa).

**Solution** : Forcer le langage explicitement :
```xml
<configuration>
    <language>java</language>  <!-- ou scala -->
</configuration>
```

### Vérifier la détection

Lancez Maven en mode debug pour voir les logs de détection :
```bash
mvn generate-gatling-data -X | grep "détecté"
```

**Sortie attendue** :
```
[INFO] → Détection automatique du langage...
[INFO]   ✓ Java détecté via dépendance Gatling Java: gatling-javaapi
[INFO] Langage cible détecté: java
```

### Aucun helper généré

Vérifiez que `generateScalaHelper` n'est pas désactivé :
```xml
<configuration>
    <generateScalaHelper>true</generateScalaHelper>  <!-- Doit être true -->
</configuration>
```

## 📚 Ressources

- [Gatling Documentation](https://gatling.io/docs/)
- [Gatling Java API](https://gatling.io/docs/gatling/reference/current/core/java/)
- [Gatling Scala API](https://gatling.io/docs/gatling/reference/current/core/scala/)

## 🔄 Rétrocompatibilité

Cette fonctionnalité est **100% rétrocompatible** :
- Les projets Scala existants continueront à générer `GatlingFeeders.scala`
- Aucune modification de configuration nécessaire
- Le comportement par défaut (Scala) est préservé

---

**Version** : 1.0.0
**Auteur** : OpenAPI Gatling Maven Plugin Team
