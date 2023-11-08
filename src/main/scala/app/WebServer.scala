package app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings
import languages.LIf
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

case class EvalRequest(text: String)
case class EvalResponse(rawExpr: String, html: String)
case class ChangeRequest(rawExpr: String, blankTreePath: String, selectedValue: String)
case class NodeExprChoiceRequest(nodeString: String, treePath: String, selectedValue: String)
case class NodeLiteralValueRequest(nodeString: String, treePath: String, literalValue: String)
case class NodeResponse(nodeString: String, html: String)
case class ActionRequest(actionName: String, nodeString: String, treePath: String, extraArgs: List[String])

trait JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val evalRequestFormat: RootJsonFormat[EvalRequest] = jsonFormat1(EvalRequest)
  implicit val evalResponseFormat: RootJsonFormat[EvalResponse] = jsonFormat2(EvalResponse)
  implicit val changeRequestFormat: RootJsonFormat[ChangeRequest] = jsonFormat3(ChangeRequest)
  implicit val nodeExprChoiceRequestFormat: RootJsonFormat[NodeExprChoiceRequest] = jsonFormat3(NodeExprChoiceRequest)
  implicit val nodeLiteralValueRequestFormat: RootJsonFormat[NodeLiteralValueRequest] = jsonFormat3(NodeLiteralValueRequest)
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
        path("expr-to-tree") {
          entity(as[EvalRequest]) { request =>
            val expr = LIf.ExpressionEvalTree.exprToTree(LIf.readExpr(request.text).get)
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, expr.toSvg))
          }
        }
      } ~
      post {
        path("expr-to-html-tree") {
          entity(as[EvalRequest]) { request =>
            val expr = LIf.readExpr(request.text).get
            val tree = LIf.ExpressionEvalTree.exprToTree(expr)
            complete(EvalResponse(expr.toString, tree.toHtml))
          }
        }
      } ~
      post {
        path("expr-to-html-tree-blank") {
          entity(as[EvalRequest]) { request =>
            val expr = LIf.readExpr(request.text).get
            val children = List(
              LIf.ExpressionEvalTree(LIf.BlankExprDropDown(), None, None, Nil),
              LIf.ExpressionEvalTree(LIf.BlankExprDropDown(), None, None, Nil)
            )
            val exprTree = LIf.ExpressionEvalTree(expr, Some(LIf.eval(expr)), None, children)
            children.foreach(_.parent = Some(exprTree))
            val response = EvalResponse(expr.toString, exprTree.toHtml)
            complete(response)
          }
        }
      } ~
      post {
        path("update-expr") {
          entity(as[ChangeRequest]) { request =>
            val expr = LIf.createUnfilledExpr(request.selectedValue)
            val treePath = request.blankTreePath.split("-").map(_.toInt).toList
            val tree = LIf.ExpressionEvalTree.exprToTree(expr)
            tree.initialTreePath = treePath
            val response = EvalResponse(expr.toString, tree.toHtml)
            complete(response)
          }
        }
      } ~
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
