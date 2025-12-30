package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class BasicSimulation extends Simulation {

  // Configuration HTTP
  val httpProtocol = http
    .baseUrl("https://computer-database.gatling.io")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/109.0")

  // Scénario de test
  val scn = scenario("Basic Simulation")
    .exec(
      http("Home Page")
        .get("/computers")
        .check(status.is(200))
    )
    .pause(1.second)
    .exec(
      http("Search Computers")
        .get("/computers?f=macbook")
        .check(status.is(200))
        .check(css("a:contains('MacBook')", "href").saveAs("computerUrl"))
    )
    .pause(1.second)
    .exec(
      http("View Computer")
        .get("${computerUrl}")
        .check(status.is(200))
    )

  // Configuration de la charge
  setUp(
    scn.inject(
      atOnceUsers(1),
      rampUsers(5) during (10.seconds),
      constantUsersPerSec(2) during (20.seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(5000),
      global.successfulRequests.percent.gt(95)
    )
}
