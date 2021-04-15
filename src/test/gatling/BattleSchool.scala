import java.io.{File, FileInputStream}
import java.util.{Base64, Random, UUID}

import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._
import scala.util.parsing.json.JSON


class BattleSchool extends Simulation {

  val host: String = sys.env("SOCKET_ADDRESS")
  val numUsers: Int = sys.env("USERS").toInt
  val wsProtocol: String = sys.env("WS_PROTOCOL")

  val protocol: HttpProtocolBuilder = http
    .baseUrl("http://" + host)
    .acceptHeader("*/*")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Gatling")
    .wsBaseUrl(wsProtocol + "://" + host)
    .wsReconnect
    .wsMaxReconnects(100)

  val scn: ScenarioBuilder = scenario("Connection-Scenario")
    .exec(Connection.connect)
    .pause(1)
    .exec(Connection.ships)
    .pause(5)
    .exec(Attack.attack)
    .pause(1)
    .exec(ws("Close").close)

  setUp(
    scn.inject(rampUsers(numUsers) during (60 seconds)).protocols(protocol)
  )


  object Connection {
    val checkConfiguration = ws.checkTextMessage("checkConfiguration")
      .matching(
        jsonPath("$.type").is("configuration")
      )
      .check(
        jsonPath("$.data.player.username").saveAs("username"),
        jsonPath("$.data.game.uuid").saveAs("gameId"),
        jsonPath("$.data.game.state").saveAs("gameState"),
        jsonPath("$.data").saveAs("data"),
        jsonPath("$.data.match.state.activePlayer").saveAs("activePlayer"),
        jsonPath("$.data.match.state.phase").saveAs("matchPhase"),
        jsonPath("$.data.player.uuid").saveAs("playerId"),
      )
    val connect: ChainBuilder =
        exec(ws("Start Socket").connect("")
          .onConnected(
            doIfOrElse(session => session("data").asOption[String].forall(_.isEmpty)) {
              exec(ws("Connect to Socket")
                .sendText("""{"type": "connection", "data": {}}""")
                .await(5 seconds)(checkConfiguration))  
            } {
              exec(ws("Reconnect")
                .sendText("""{"type": "connection", "data": {"username": ${username}, "gameId": "${gameId}", "playerId": ${playerId}}}""")
                .await(5 seconds)(checkConfiguration))
            }
          ))


    val ships: ChainBuilder =
        exec(ws("Send Ships Positions")
              .sendText("""{"type": "ship-positions","data": {"Carrier": { "origin": [3, 0], "orientation": "vertical"}, "Battleship": { "origin": [0, 1], "orientation": "vertical" }, "Submarine": { "origin": [1, 1], "orientation": "vertical"}, "Destroyer": { "origin": [1, 4], "orientation": "horizontal"}}}""")
              .await(5 seconds)(checkConfiguration)).pause(4)
  }


  object Attack {

    val checkAttack = ws.checkTextMessage("CheckAttack")
      .matching(
        jsonPath("$.data.game.state").is("${gameState}"),
        // jsonPath("$.data.match.state.phase").isNot("finished"),
        jsonPath("$.data.match.state.activePlayer").is("${playerId}")


      )
      .check(
        jsonPath("$.data.match.state.phase").saveAs("matchPhase"),
        jsonPath("$.data.match.state.activePlayer").saveAs("activePlayer"),
        )


  

    val attack: ChainBuilder =
    asLongAs(session => (session("matchPhase").as[String]) != "finished", "index") {
      val numPairs = List((0, 0), (0, 1), (0, 2), (0,3), (0,4), (1, 0), (1, 1), (1, 2), (1,3), (1,4), (2, 0), (2, 1), (2, 2), (2,3), (2,4), (3, 0), (3, 1), (3, 2), (3,3), (3,4), (4, 0), (4, 1), (4, 2), (4,3), (4,4))

      doIfOrElse(session => session("matchPhase").as[String] != "bonus") {
        exec(session => {
        val index = session("index").as[Int]
        val x_axis = numPairs(index)._1
        val y_axis = numPairs(index)._2
        println("Game State: " + session("gameState").as[String] + "  Match Phase:" +  session("matchPhase").as[String] + " PlayerId: " + session("playerId").as[String] + " Active Player: " + session("activePlayer").as[String] )
        session
          .set("x_axis", x_axis)
          .set("y_axis", y_axis)
      }).pause(3)
        .exec(ws("Attack")
          .sendText("""{"type":"attack","data":{"type":"1x1","origin":[${x_axis},${y_axis}],"orientation":"horizontal"}}""").await(30.seconds)(checkAttack)
        ).exitHereIfFailed
      }
      {
        exec(session => {
          println("Game State: " + session("gameState").as[String] + "  Match Phase:" +  session("matchPhase").as[String] + " PlayerId: " + session("playerId").as[String] + " Active Player: " + session("activePlayer").as[String] )
          session
        })
        exec(ws("Bonus Round")
          .sendText("""{"type":"bonus","data":{"hits":1}}""").await(30.seconds)(checkAttack)).exitHereIfFailed
      }
    }
  }
}