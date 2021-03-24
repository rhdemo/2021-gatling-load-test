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
  val listA = (0 to 5) zip (0 to 5) zip  (0 to 1)
  val numUsers: Int = sys.env("USERS").toInt
  val numAttacks: Int = sys.env("ATTACKS").toInt
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
    .pause(1)
    .exec(Attack.attack)
    .pause(1)
    .exec(ws("Close").close)

  setUp(
    scn.inject(rampUsers(numUsers) during (5 seconds)).protocols(protocol)
  )


  object Connection {
    val checkConfiguration = ws.checkTextMessage("checkConfiguration")
      .matching(
        jsonPath("$.type").is("configuration")
      )
      .check(
        jsonPath("$.data.player.username").saveAs("username"),
        jsonPath("$.data.game.uuid").saveAs("gameId"),
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
        exec(ws("Send Ships Positions").sendText("""{"type": "ship-positions","data": {"Carrier": { "origin": [3, 0], "orientation": "vertical"}, "Battleship": { "origin": [0, 1], "orientation": "vertical" }, "Submarine": { "origin": [1, 1], "orientation": "vertical"}, "Destroyer": { "origin": [1, 4], "orientation": "horizontal"}}}"""))

  }


  object Attack {


    val checkAttack = ws.checkTextMessage("CheckAttack")
      .matching(
        jsonPath("$.type").is("attack-result")
      )
      .check(jsonPath("$").saveAs("attack-result"))


    val attack: ChainBuilder =
      repeat(numAttacks, "i") {
        exec(session => {
          val r = scala.util.Random
          val x_axis = r.nextInt(4) + 1
          val y_axis = r.nextInt(4) + 1
          session
            .set("x_axis", x_axis)
            .set("y_axis", y_axis)
        })
          .exec(ws("Attack")
            .sendText("""{"type":"attack","data":{"type":"1x1","origin":["${x_axis}","${y_axis}"],"orientation":"horizontal"}}""")
          )
          .pause(5)
      }
  }
}