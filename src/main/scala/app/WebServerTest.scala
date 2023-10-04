package app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object WebServerTest {
  val buttonClickCount: AtomicInteger = new AtomicInteger(0)

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val route: Route =
      post {
        path("button-clicked") {
          val newCount = buttonClickCount.incrementAndGet()
          println(s"Button was clicked. Current click count: $newCount")
          complete(newCount.toString)
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
    val portNumber = 27015

    val bindingFuture = Http().newServerAt("0.0.0.0", portNumber).withSettings(customSettings).bind(route)

    println(s"Server online at http://localhost:$portNumber/\nPress RETURN to stop...")
    StdIn.readLine()

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
