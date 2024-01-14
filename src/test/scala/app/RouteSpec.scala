package app

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ContentTypes, MessageEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import languages.{ClickDeduceLanguage, LArith}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import net.ruippeixotog.scalascraper.model.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.concurrent.Future

class RouteSpec extends AnyWordSpec with Matchers with ScalatestRouteTest with JsonSupport {
  val server: WebServer = new WebServer()
  val route: Route = server.requestRoute

  "The start-node-blank endpoint should handle POST requests" should {
    val startNodeBlank: String = "/start-node-blank"

    def createRequest(lang: ClickDeduceLanguage): Future[MessageEntity] = {
      val request = EvalRequest(server.getLanguageName(lang))
      Marshal(request).to[MessageEntity]
    }

    val langRequestList: List[Future[MessageEntity]] = server.knownLanguages.map(createRequest)

    def checkOnAllRequests(test: Future[MessageEntity] => Unit): Unit = {
      langRequestList.foreach { request =>
        Post(startNodeBlank, request) ~> route ~> check {
          test(request)
        }
      }
    }

    "return a successful response" in {
      checkOnAllRequests { request =>
        status shouldBe StatusCodes.OK
      }
    }

    "return a response with nodeString and html fields for requests with a langName parameter" in {
      checkOnAllRequests { request =>
        contentType shouldBe ContentTypes.`application/json`
        responseAs[NodeResponse].nodeString should not be empty
        responseAs[NodeResponse].html should not be empty
      }
    }

    "return a response where nodeString is ExprChoiceNode() for requests with a valid langName parameter" in {
      checkOnAllRequests { request =>
        responseAs[NodeResponse].nodeString shouldBe LArith.ExprChoiceNode().toString
      }
    }

    "return valid HTML that" should {
      def checkHtmlDoc(test: Document => Unit): Unit = {
        checkOnAllRequests { request =>
          val browser = JsoupBrowser()
          val doc: Document = browser.parseString(responseAs[NodeResponse].html)

          test(doc)
        }
      }

      "has at least 1 div" in {
        checkHtmlDoc { doc =>
          doc >> elementList("div") should not be empty
        }
      }

      "has a subtree + axiom div with the correct node string data" in {
        checkHtmlDoc { doc =>
          val nodeString = responseAs[NodeResponse].nodeString
          doc >> elementList(s"div.subtree.axiom[data-node-string='$nodeString']") should not be empty
        }
      }

      "has only one subtree div" in {
        checkHtmlDoc { doc =>
          doc >> elementList("div.subtree") should have size 1
        }
      }

      "the subtree has a blank tree-path" in {
        checkHtmlDoc { doc =>
          doc >> elementList("div.subtree[data-tree-path='']") should not be empty
        }
      }

      "the subtree has a child .expr div" in {
        checkHtmlDoc { doc =>
          doc >> elementList("div.subtree > div.expr") should not be empty
        }
      }

      "the subtree has a child .expr div which has a child .expr-dropdown select" in {
        checkHtmlDoc { doc =>
          doc >> elementList("div.subtree > div.expr > select.expr-dropdown") should not be empty
        }
      }

      "there is only one .expr-dropdown select" in {
        checkHtmlDoc { doc =>
          doc >> elementList("select.expr-dropdown") should have size 1
        }
      }

      "the .expr-dropdown select's first option has a blank value" in {
        checkHtmlDoc { doc =>
          doc >> elementList("select.expr-dropdown > option") should not be empty
          (doc >> elementList("select.expr-dropdown > option")).head.attr("value") shouldBe ""
        }
      }

      "the .expr-dropdown select has multiple options" in {
        checkHtmlDoc { doc =>
          (doc >> elementList("select.expr-dropdown > option")).size should be > 1
        }
      }

      "the contents of the .scoped-variables div should be empty" in {
        checkHtmlDoc { doc =>
          doc >> elementList("div.scoped-variables") should not be empty
          val scopedVariablesDiv = (doc >> elementList("div.scoped-variables")).head
          scopedVariablesDiv.text shouldBe empty
          scopedVariablesDiv.children shouldBe empty
        }
      }
    }
  }

  "The get-lang-selector endpoint should handle GET requests" should {
    val getLangSelector: String = "/get-lang-selector"

    def checkOnRequest(test: LangSelectorResponse => Unit): Unit = {
      Get(getLangSelector) ~> route ~> check {
        test(responseAs[LangSelectorResponse])
      }
    }

    "return a successful response" in {
      checkOnRequest { response =>
        status shouldBe StatusCodes.OK
      }
    }

    "return a response with langSelectorHtml field" in {
      checkOnRequest { response =>
        contentType shouldBe ContentTypes.`application/json`
        responseAs[LangSelectorResponse].langSelectorHtml should not be empty
      }
    }

    "return a response with a valid langSelectorHtml field" should {
      def checkHtmlDoc(test: Document => Unit): Unit = {
        checkOnRequest { response =>
          val browser = JsoupBrowser()
          val doc: Document = browser.parseString(responseAs[LangSelectorResponse].langSelectorHtml)

          test(doc)
        }
      }

      "has no divs" in {
        checkHtmlDoc { doc =>
          doc >> elementList("div") shouldBe empty
        }
      }

      "has a single select element" in {
        checkHtmlDoc { doc =>
          (doc >> elementList("select")).size shouldBe 1
        }
      }

      "the select element has an option for each language" in {
        checkHtmlDoc { doc =>
          val select = (doc >> elementList("select")).head
          val options = select >> elementList("option")
          options.size shouldBe server.knownLanguages.size
        }
      }

      "each option in the select has value and text matching the languages in order" in {
        checkHtmlDoc { doc =>
          val select = (doc >> elementList("select")).head
          val options = select >> elementList("option")
          options.zipWithIndex.foreach { case (option, index) =>
            option.attr("value") shouldBe server.getLanguageName(server.knownLanguages(index))
            option.text shouldBe server.getLanguageName(server.knownLanguages(index))
          }
        }
      }
    }
  }

  "The process-action endpoint should handle POST requests" should {
    val processAction: String = "/process-action"

    def createRequest(
      lang: ClickDeduceLanguage,
      modeName: String,
      actionKind: String,
      nodeString: String,
      treePath: List[Int],
      extraArgs: List[String]
    ): Future[MessageEntity] = {
      val treePathString = treePath.mkString("-")
      val request =
        ActionRequest(server.getLanguageName(lang), modeName, actionKind, nodeString, treePathString, extraArgs)
      Marshal(request).to[MessageEntity]
    }

    def checkOnRequest(request: Future[MessageEntity], test: Future[MessageEntity] => Unit): Unit = {
      Post(processAction, request) ~> route ~> check {
        test(request)
      }
    }

    val simpleIdentityRequest = createRequest(LArith, "edit", "IdentityAction", "ExprChoiceNode()", List(), List())

    "return a successful response" in {
      checkOnRequest(
        simpleIdentityRequest,
        request => {
          status shouldBe StatusCodes.OK
        }
      )
    }

    "return a response with nodeString and html fields" in {
      checkOnRequest(
        simpleIdentityRequest,
        request => {
          contentType shouldBe ContentTypes.`application/json`
          responseAs[NodeResponse].nodeString should not be empty
          responseAs[NodeResponse].html should not be empty
        }
      )
    }

    "return a response with correct nodeString" in {
      checkOnRequest(
        simpleIdentityRequest,
        request => {
          responseAs[NodeResponse].nodeString shouldBe LArith.ExprChoiceNode().toString
        }
      )
    }

    "return consistent responses" in {
      checkOnRequest(
        simpleIdentityRequest,
        request1 => {
          val firstResponse = responseAs[NodeResponse]
          checkOnRequest(
            simpleIdentityRequest,
            request2 => {
              val secondResponse = responseAs[NodeResponse]
              firstResponse shouldBe secondResponse
            }
          )
        }
      )
    }

    "accept requests with 'edit', 'type-check', and 'eval' modes" in {
      val modes = List("edit", "type-check", "eval")
      for (mode <- modes) {
        val request = createRequest(LArith, mode, "IdentityAction", "ExprChoiceNode()", List(), List())
        checkOnRequest(
          request,
          request => {
            status shouldBe StatusCodes.OK
          }
        )
      }
    }

    def errorOnInvalidRequest(request: ActionRequest): Unit = {
      checkOnRequest(
        Marshal(request).to[MessageEntity],
        response => {
          status should not be StatusCodes.OK
        }
      )
    }

    "return an error response for an invalid language" in {
      val request = ActionRequest("NonsenseLanguageName", "edit", "IdentityAction", "ExprChoiceNode()", "", List())
      errorOnInvalidRequest(request)
    }

    "return an error response for an invalid display mode" in {
      val request =
        ActionRequest(server.getLanguageName(LArith), "nonsense", "IdentityAction", "ExprChoiceNode()", "", List())
      errorOnInvalidRequest(request)
    }

    "return an error response for an invalid action kind" in {
      val request =
        ActionRequest(server.getLanguageName(LArith), "edit", "NonsenseAction", "ExprChoiceNode()", "", List())
      errorOnInvalidRequest(request)
    }

    "return an error response for an invalid node string" in {
      val invalidNodeStrings = List(
        "NonsenseNode()",
        "ExprChoiceNode",
        "ExprChoiceNode(ExprChoiceNode())",
        "ExprChoiceNode(1, 2, 3)",
        """VariableNode("Num")""",
        """VariableNode("Num", "Num")""",
        """VariableNode("Num", List(SubExprNode(ExprChoiceNode())))"""
      )
      for (nodeString <- invalidNodeStrings) {
        val request = ActionRequest(server.getLanguageName(LArith), "edit", "IdentityAction", nodeString, "", List())
        errorOnInvalidRequest(request)
      }
    }
  }

  "The GET requests should return appropriate files" should {
    def createDistFile(fileName: String, contents: String): Unit = {
      Files.createDirectories(Paths.get("webapp/dist/"))
      Files.write(Paths.get(s"webapp/dist/$fileName"), contents.getBytes(StandardCharsets.UTF_8))
    }

    "return the index.html file for the '/' path" in {
      createDistFile("index.html", "<title>ClickDeduce</title>")
      Get("/") ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType shouldBe ContentTypes.`text/html(UTF-8)`

        responseAs[String] should include("<title>ClickDeduce</title>")
      }
    }

    "return the 'images/zoom_to_fit.svg' file" in {
      Get("/images/zoom_to_fit.svg") ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType.toString shouldBe "image/svg+xml"
      }
    }

    "return the contents of 'dist/bundle.js'" in {
      val testBundleJs = "alert('test bundle.js');"
      createDistFile("bundle.js", testBundleJs)
      Get("/dist/bundle.js") ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType.toString shouldBe "application/javascript; charset=UTF-8"
        responseAs[String] shouldBe testBundleJs
      }
    }

    "return 'dist/bundle.js' when requesting 'bundle.js'" in {
      val testBundleJs = "alert('test bundle.js 2');"
      createDistFile("bundle.js", testBundleJs)
      Get("/bundle.js") ~> route ~> check {
        status shouldBe StatusCodes.OK
        contentType.toString shouldBe "application/javascript; charset=UTF-8"
        responseAs[String] shouldBe testBundleJs
      }
    }

    "does not return contents from '/scripts/'" in {
      Get("/scripts/script.js") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "does not return contents from '/styles/'" in {
      Get("/styles/stylesheet.css") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "does not return contents from '/pages/'" in {
      Get("/pages/index.html") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }
}
