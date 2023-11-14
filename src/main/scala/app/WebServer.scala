package app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings
import languages.{ClickDeduceLanguage, LArith, LIf}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

case class EvalRequest(text: String)
case class NodeResponse(nodeString: String, html: String)
case class ActionRequest(actionName: String, nodeString: String, treePath: String, extraArgs: List[String])

trait JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val evalRequestFormat: RootJsonFormat[EvalRequest] = jsonFormat1(EvalRequest)
  implicit val nodeResponseFormat: RootJsonFormat[NodeResponse] = jsonFormat2(NodeResponse)
  implicit val actionRequestFormat: RootJsonFormat[ActionRequest] = jsonFormat4(ActionRequest)
}


object WebServerTest extends JsonSupport {
  val buttonClickCount: AtomicInteger = new AtomicInteger(0)

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val route: Route = {
      post {
        path("start-node-blank") {
          entity(as[EvalRequest]) { request =>
            val tree = LIf.ExprChoiceNode()
            val response = NodeResponse(tree.toString, tree.toHtml.toString)
            complete(response)
          }
        }
      } ~
      post {
        path("process-action") {
          entity(as[ActionRequest]) { request =>
            val action = LIf.createAction(request.actionName, request.nodeString, request.treePath, request.extraArgs)
            val updatedTree = action.newTree
            val response = NodeResponse(updatedTree.toString, updatedTree.toHtml.toString)
            complete(response)
          }
        }
      } ~
      get {
        pathEndOrSingleSlash {
          getFromDirectory("webapp/index.html")
        } ~
          getFromDirectory("webapp")
      }
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

  val knownLanguages: List[ClickDeduceLanguage] = List(LArith, LIf)
}
