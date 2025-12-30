package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class AdvancedSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("https://computer-database.gatling.io")
    .acceptHeader("application/json")

  val feeder = Iterator.continually(Map(
    "searchTerm" -> ("term" + scala.util.Random.nextInt(100))
  ))

  val scn = scenario("Advanced Simulation")
    .feed(feeder)
    .exec(
      http("Search with term")
        .get("/computers?f=${searchTerm}")
        .check(status.is(200))
    )
    .pause(500.milliseconds)

  setUp(
    scn.inject(
      rampUsers(10) during (5.seconds)
    )
  ).protocols(httpProtocol)
}
