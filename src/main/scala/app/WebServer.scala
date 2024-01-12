package app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.settings.ServerSettings
import languages.*
import scalatags.Text.all.*
import scalatags.Text.{TypedTag, attrs}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.sys.process.Process
import scala.util.{Failure, Success, Try}

case class EvalRequest(langName: String)

case class NodeResponse(nodeString: String, html: String)

case class ActionRequest(
  langName: String,
  modeName: String,
  actionName: String,
  nodeString: String,
  treePath: String,
  extraArgs: List[String]
)

case class LangSelectorRequest()

case class LangSelectorResponse(langSelectorHtml: String)

trait JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val evalRequestFormat: RootJsonFormat[EvalRequest] = jsonFormat1(EvalRequest.apply)
  implicit val nodeResponseFormat: RootJsonFormat[NodeResponse] = jsonFormat2(NodeResponse.apply)
  implicit val actionRequestFormat: RootJsonFormat[ActionRequest] = jsonFormat6(ActionRequest.apply)
  implicit val langSelectorRequestFormat: RootJsonFormat[LangSelectorRequest] = jsonFormat0(LangSelectorRequest.apply)
  implicit val langSelectorResponseFormat: RootJsonFormat[LangSelectorResponse] = jsonFormat1(LangSelectorResponse.apply)
}

val customExceptionHandler: ExceptionHandler = ExceptionHandler {
  case exception: Exception => extractUri { uri =>
    exception.printStackTrace()
    complete(
      HttpResponse(
        StatusCodes.InternalServerError,
        entity = exception.toString
      )
    )
  }
}


object WebServer extends JsonSupport {
  private val webappDirectory: String = "webapp"
  private val distDirectory: String = s"$webappDirectory/dist"
  private val imagesDirectory: String = s"$webappDirectory/images"
  private val indexPage: String = s"$distDirectory/index.html"

  private var portNumber: Int = 27019
  private var bindingAddress: String = "0.0.0.0"

  private var skipBundleScripts: Boolean = false

  def main(args: Array[String]): Unit = {
    parseArgs(args)

    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val route: Route = handleExceptions(customExceptionHandler) {
      post {
        path("start-node-blank") {
          entity(as[EvalRequest]) { request =>
            val lang = getLanguage(request.langName)
            val tree = lang.ExprChoiceNode()
            val response = NodeResponse(tree.toString, tree.toHtml(lang.DisplayMode.Edit).toString)
            complete(response)
          }
        } ~
        path("process-action") {
          entity(as[ActionRequest]) { request =>
            val lang = getLanguage(request.langName)
            val action = lang.createAction(
              request.actionName, request.nodeString, request.treePath, request.extraArgs, request.modeName
            )
            val updatedTree = action.newTree
            val displayMode: lang.DisplayMode = lang.DisplayMode.fromString(request.modeName)
            val response = NodeResponse(updatedTree.toString, updatedTree.toHtml(displayMode).toString)
            complete(response)
          }
        }
      } ~
      get {
        path("get-lang-selector") {
          val langSelector: TypedTag[String] = select(
            id := "lang-selector", name := "lang-name",
            knownLanguages.map(lang => option(value := getLanguageName(lang), getLanguageName(lang)))
          )
          val response = LangSelectorResponse(langSelector.toString)
          complete(response)
        } ~
        pathEndOrSingleSlash { getFromFile(indexPage) } ~
        pathPrefix("dist") { getFromDirectory(distDirectory) } ~
        pathPrefix("images") { getFromDirectory(imagesDirectory) } ~
        getFromDirectory(distDirectory)
      }
    }

    if (skipBundleScripts) {
      println("Script bundling was skipped\n")
    } else {
      if (!bundleScripts()) {
        println("Failed to bundle scripts")
        return
      }
      println("\nSuccessfully bundled scripts\n\n")
    }

    val defaultSettings = ServerSettings(system)
    val customSettings = defaultSettings.withTransparentHeadRequests(true)

    val bindingFuture = Http().newServerAt(bindingAddress, portNumber).withSettings(customSettings).bind(route)

    println(s"Server online at http://localhost:$portNumber/\nPress RETURN to stop...")
    StdIn.readLine()

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  private def parseArgs(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Unit]("WebServer") {
      opt[Int]("port")
        .action((x, _) => portNumber = x)
        .text("Port number to bind")

      opt[String]("address")
        .action((x, _) => bindingAddress = x)
        .text("Binding address")

      opt[Unit]("skip-bundle-scripts")
        .action((_, _) => skipBundleScripts = true)
        .text("Skip bundling scripts")
    }

    parser.parse(args, ())
  }

  private val knownLanguages: List[ClickDeduceLanguage] = List(LArith, LIf, LLet, LLam, LRec)

  private def getLanguage(langName: String): ClickDeduceLanguage = knownLanguages.find(getLanguageName(_) == langName) match {
    case Some(lang) => lang
    case None => throw new IllegalArgumentException(s"Unknown language: $langName")
  }

  private def getLanguageName(lang: ClickDeduceLanguage): String = lang.getClass.getSimpleName.stripSuffix("$")

  private def bundleScripts(): Boolean = {
    println("Bundling scripts...")
    val processBuilder = new ProcessBuilder("cmd.exe", "/c", "npm run build")

    // Redirect error stream to the standard output stream
    processBuilder.redirectErrorStream(true)

    val process = processBuilder.start()

    // Capture and print the output
    val inputStream = process.getInputStream
    val reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))

    var line: String = ""
    while ({line = reader.readLine(); line != null}) {
      println(line)
    }

    val exitCode = process.waitFor()
    exitCode == 0
  }
}
