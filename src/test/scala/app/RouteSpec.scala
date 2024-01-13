package app

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ContentTypes, MessageEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import languages.ClickDeduceLanguage
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class RouteSpec extends AnyWordSpec with Matchers with ScalatestRouteTest with JsonSupport {
  val route: Route = WebServer.requestRoute
  val startNodeBlank: String = "/start-node-blank"

  "The start-node-blank endpoint" should {
    def createRequest(lang: ClickDeduceLanguage): Future[MessageEntity] = {
      val request = EvalRequest(WebServer.getLanguageName(lang))
      Marshal(request).to[MessageEntity]
    }

    val langRequestList: List[Future[MessageEntity]] = WebServer.knownLanguages.map(createRequest)

    val arithRequest = createRequest(WebServer.knownLanguages.head)
    "return a successful response for POST requests" in {
      langRequestList.foreach { request =>
        Post(startNodeBlank, request) ~> route ~> check {
          status shouldBe StatusCodes.OK
        }
      }
    }

    "return a response with nodeString and html fields for POST requests with a langName parameter" in {
      langRequestList.foreach { request =>
        Post(startNodeBlank, request) ~> route ~> check {
          contentType shouldBe ContentTypes.`application/json`
          responseAs[NodeResponse].nodeString should not be empty
          responseAs[NodeResponse].html should not be empty
        }
      }
    }

    "return a response where nodeString is ExprChoiceNode() for POST requests with a valid langName parameter" in {
      langRequestList.foreach { request =>
        Post(startNodeBlank, request) ~> route ~> check {
          responseAs[NodeResponse].nodeString shouldBe "ExprChoiceNode()"
        }
      }
    }
  }
}
