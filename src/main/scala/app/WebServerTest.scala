package app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings
import languages.LArith
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

case class EvalRequest(text: String)
case class EvalResponse(rawExpr: String, html: String)
case class ChangeRequest(rawExpr: String, blankId: String, selectedValue: String)

trait JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val evalRequestFormat: RootJsonFormat[EvalRequest] = jsonFormat1(EvalRequest)
  implicit val evalResponseFormat: RootJsonFormat[EvalResponse] = jsonFormat2(EvalResponse)
  implicit val changeRequestFormat: RootJsonFormat[ChangeRequest] = jsonFormat3(ChangeRequest)
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
            val expr = LArith.ExpressionEvalTree.exprToTree(LArith.readExpr(request.text).get)
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, expr.toSvg))
          }
        }
      } ~
      post {
        path("expr-to-html-tree") {
          entity(as[EvalRequest]) { request =>
            val expr = LArith.ExpressionEvalTree.exprToTree(LArith.readExpr(request.text).get)
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, expr.toHtml))
          }
        }
      } ~
      post {
        path("expr-to-html-tree-blank") {
          entity(as[EvalRequest]) { request =>
            val expr = LArith.readExpr(request.text).get
            val children = List(
              LArith.ExpressionEvalTree(LArith.BlankExprDropDown(), None, None, Nil),
              LArith.ExpressionEvalTree(LArith.BlankExprDropDown(), None, None, Nil)
            )
            val exprTree = LArith.ExpressionEvalTree(expr, Some(LArith.eval(expr)), None, children)
            children.foreach(_.parent = Some(exprTree))
            val response = EvalResponse(expr.toString, exprTree.toHtml)
            complete(response)
          }
        }
      } ~
      post {
        path("update-expr") {
          entity(as[ChangeRequest]) { request =>
            val expr = LArith.createUnfilledExpr(request.selectedValue)

            val tree = LArith.ExpressionEvalTree.exprToTree(expr)
            val response = EvalResponse(expr.toString, tree.toHtml)
            println(request.toString + " -> " + response.toString)
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
