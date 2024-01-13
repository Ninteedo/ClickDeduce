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

import scala.concurrent.Future

class RouteSpec extends AnyWordSpec with Matchers with ScalatestRouteTest with JsonSupport {
  val route: Route = WebServer.requestRoute

  "The start-node-blank endpoint should handle POST requests" should {
    val startNodeBlank: String = "/start-node-blank"

    def createRequest(lang: ClickDeduceLanguage): Future[MessageEntity] = {
      val request = EvalRequest(WebServer.getLanguageName(lang))
      Marshal(request).to[MessageEntity]
    }

    val langRequestList: List[Future[MessageEntity]] = WebServer.knownLanguages.map(createRequest)

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
}
