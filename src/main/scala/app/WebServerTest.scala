import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings

import scala.io.StdIn

object WebServerTest {
  def main(args: Array[String]) = {
    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val executionContext = system.dispatcher

    val route: Route =
      post {
        path("button-clicked") {
          println("Button was clicked.")
          complete(StatusCodes.OK)
        }
      } ~
        get {
          pathEndOrSingleSlash {
            getFromDirectory("webapp/index.html")
          } ~
            getFromDirectory("webapp")
        }

    val defaultSettings = ServerSettings(system)
    val customSettings = defaultSettings.withTransparentHeadRequests(true)

    val bindingFuture = Http().newServerAt("localhost", 8080).withSettings(customSettings).bind(route)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
