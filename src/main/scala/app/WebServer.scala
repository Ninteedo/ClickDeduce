package app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.settings.ServerSettings
import convertors.{DisplayMode, HTMLConvertor}
import languages.*
import scalatags.Text.TypedTag
import scalatags.Text.all.*
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import java.io.File
import scala.concurrent.ExecutionContextExecutor
import scala.sys.process.Process

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
  implicit val langSelectorResponseFormat: RootJsonFormat[LangSelectorResponse] = jsonFormat1(
    LangSelectorResponse.apply
  )
}

val customExceptionHandler: ExceptionHandler = ExceptionHandler { case exception: Exception =>
  extractUri { uri =>
    exception.printStackTrace()

    val statusCode = exception match {
      case _: IllegalArgumentException => StatusCodes.BadRequest
      case _: ClickDeduceException     => StatusCodes.BadRequest
      case _                           => StatusCodes.InternalServerError
    }

    complete(HttpResponse(statusCode, entity = exception.toString))
  }
}

class WebServer extends JsonSupport {
  private val webappDirectory: String = "webapp"
  private val distDirectory: String = s"$webappDirectory/dist"
  private val imagesDirectory: String = s"$webappDirectory/images"
  private val indexPage: String = s"$distDirectory/index.html"

  private var _isOnline: Boolean = false

  def isOnline: Boolean = _isOnline

  private def isOnline_=(value: Boolean): Unit = _isOnline = value

  private var _portNumber: Int = 27019

  def portNumber: Int = _portNumber

  def portNumber_=(value: Int): Unit = {
    if (isOnline) throw new IllegalStateException("Cannot change port number while server is online")
    if (value <= 0 || value > 65535) {
      throw new IllegalArgumentException("Port number must be between 1 and 65535")
    }
    _portNumber = value
  }

  private var _bindingAddress: String = "0.0.0.0"

  def bindingAddress: String = _bindingAddress

  def bindingAddress_=(value: String): Unit = {
    def invalidOctet(octet: Int): Boolean = {
      octet < 0 || octet > 255 || octet.toString.toInt != octet
    }

    if (isOnline) throw new IllegalStateException("Cannot change binding address while server is online")
    val ipAddressPattern = """^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""".r
    ipAddressPattern.findFirstMatchIn(value) match {
      case Some(matchResult) =>
        val octets = matchResult.subgroups.map(_.toInt)
        if (octets.exists(invalidOctet)) {
          throw new IllegalArgumentException("Invalid IP address")
        }
        _bindingAddress = s"${octets.mkString(".")}"
      case None => throw new IllegalArgumentException("Invalid IP address")
    }
  }

  private var _skipBundleScripts: Boolean = false

  def skipBundleScripts: Boolean = _skipBundleScripts

  def skipBundleScripts_=(value: Boolean): Unit = {
    if (isOnline) throw new IllegalStateException("Cannot change skipBundleScripts while server is online")
    _skipBundleScripts = value
  }

  def runServer(args: Array[String]): Unit = {
    val parseSuccess: Boolean = parseArgs(args)
    if (!parseSuccess) {
      println("Failed to parse arguments")
      System.exit(1)
    }

    implicit val system: ActorSystem = ActorSystem("ClickDeduceWebServer")
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    if (skipBundleScripts) {
      println("Script bundling was skipped\n")
    } else {
      bundleScripts()
      println("\nSuccessfully bundled scripts\n\n")
    }

    val defaultSettings = ServerSettings(system)
    val customSettings = defaultSettings.withTransparentHeadRequests(true)

    isOnline = true
    val bindingFuture = Http().newServerAt(bindingAddress, portNumber).withSettings(customSettings).bind(requestRoute)

    println(s"Server online at http://localhost:$portNumber/")
    // StdIn.readLine()

    while (isOnline) {
      Thread.sleep(1000)
    }

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
    isOnline = false
  }

  def parseArgs(args: Array[String]): Boolean = {
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

    parser.parse(args, ()).isDefined
  }

  private def bundleScripts(): Unit = {
    println("Bundling scripts...")

    val osName: String = System.getProperty("os.name").toLowerCase
    val command: String = s"${if (osName.contains("win")) "cmd.exe /c " else ""}npm run build"

    val exitCode: Int = Process(command, new File("webapp")).!

    if (exitCode != 0) {
      System.exit(exitCode)
    }
  }

  private def resourceNotFoundResponse: HttpResponse =
    HttpResponse(StatusCodes.NotFound, entity = HttpEntity("The requested resource could not be found."))

  val requestRoute: Route = handleExceptions(customExceptionHandler) {
    post {
      path("start-node-blank") {
        entity(as[EvalRequest]) { request =>
          val lang = WebServer.getLanguage(request.langName)
          val tree = lang.ExprChoiceNode()
          val response = NodeResponse(tree.toString, tree.toHtml(DisplayMode.Edit).toString)
          complete(response)
        }
      } ~
        path("process-action") {
          entity(as[ActionRequest]) { request =>
            val lang = WebServer.getLanguage(request.langName)
            val action = lang.createAction(
              request.actionName,
              request.nodeString,
              request.treePath,
              request.extraArgs,
              request.modeName
            )
            val updatedTree = action.newTree
            val displayMode: lang.DisplayMode = lang.DisplayMode.fromString(request.modeName)
            val response = NodeResponse(updatedTree.toString, updatedTree.toHtml(displayMode).toString)
            val displayMode: DisplayMode = DisplayMode.fromString(request.modeName)
            complete(response)
          }
        }
    } ~
      get {
        path("get-lang-selector") {
          val langSelector: TypedTag[String] = select(
            id := "lang-selector",
            name := "lang-name",
            WebServer.knownLanguages.map(lang => option(value := WebServer.getLanguageName(lang), WebServer.getLanguageName(lang)))
          )
          val response = LangSelectorResponse(langSelector.toString)
          complete(response)
        } ~
          pathEndOrSingleSlash { getFromFile(indexPage) } ~
          pathPrefix("dist") { getFromDirectory(distDirectory) } ~
          pathPrefix("images") { getFromDirectory(imagesDirectory) } ~
          pathPrefix("scripts") { complete(resourceNotFoundResponse) } ~
          pathPrefix("styles") { complete(resourceNotFoundResponse) } ~
          pathPrefix("pages") { complete(resourceNotFoundResponse) } ~
          getFromDirectory(distDirectory)
      }
  }
}

object WebServer {
  def main(args: Array[String]): Unit = {
    val server = new WebServer()
    server.runServer(args)
  }

  val knownLanguages: List[ClickDeduceLanguage] = List(LArith(), LIf(), LLet(), LLam(), LRec(), LData(), LPoly())

  def getLanguage(langName: String): ClickDeduceLanguage = knownLanguages.find(getLanguageName(_) == langName) match {
    case Some(lang) => lang
    case None => throw new IllegalArgumentException(s"Unknown language: $langName")
  }

  def getLanguageName(lang: ClickDeduceLanguage): String = lang.getClass.getSimpleName.stripSuffix("$")
}
