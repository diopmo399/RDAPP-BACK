package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class UserApiSimulation extends Simulation {

  // Configuration HTTP
  val httpProtocol = http
    .baseUrl("http://localhost:8080/api")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // Chargement des feeders générés automatiquement
  val createUserFeeder = csv("target/gatling-data/endpoints/POST_users_request.csv").circular
  val listUsersFeeder = csv("target/gatling-data/endpoints/GET_users_request.csv").circular

  // Scénario: Création d'utilisateurs
  val createUsers = scenario("Create Users")
    .feed(createUserFeeder)
    .exec(
      http("Create User")
        .post("/users")
        .body(StringBody(session => session("body").as[String]))
        .check(status.is(201))
    )

  // Scénario: Liste des utilisateurs
  val listUsers = scenario("List Users")
    .feed(listUsersFeeder)
    .exec(
      http("List Users")
        .get("/users")
        .queryParam("page", "${page}")
        .queryParam("size", "${size}")
        .check(status.is(200))
    )

  // Scénario mixte
  val mixedScenario = scenario("Mixed Operations")
    .exec(
      // Lister les utilisateurs
      feed(listUsersFeeder)
        .exec(
          http("List Users")
            .get("/users")
            .queryParam("page", "${page}")
            .queryParam("size", "${size}")
            .check(status.is(200))
        )
    )
    .pause(1.second)
    .exec(
      // Créer un utilisateur
      feed(createUserFeeder)
        .exec(
          http("Create User")
            .post("/users")
            .body(StringBody(session => session("body").as[String]))
            .check(status.is(201))
            .check(jsonPath("$.id").saveAs("userId"))
        )
    )

  // Configuration de la charge
  setUp(
    createUsers.inject(
      rampUsers(10) during (5.seconds)
    ),
    listUsers.inject(
      constantUsersPerSec(5) during (10.seconds)
    ),
    mixedScenario.inject(
      rampUsers(20) during (10.seconds)
    )
  ).protocols(httpProtocol)
}
