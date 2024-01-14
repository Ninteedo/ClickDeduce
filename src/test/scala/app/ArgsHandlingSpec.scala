package app

import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.TableFor1
import org.scalatest.wordspec.AnyWordSpec

class ArgsHandlingSpec extends AnyWordSpec with Matchers {
  "Port number" should {
    "have a default value" in {
      val server = WebServer()
      server.portNumber should be > 0
      server.portNumber should be < 65536
    }

    "be settable" in {
      val server = WebServer()

      val validPorts = TableFor1("port", 1, 65535, 12345, 56, 1024, 2048, 4096, 8192, 16384, 32768, 49152)
      forAll(validPorts) { port =>
        server.portNumber = port
        server.portNumber shouldBe port
      }
    }

    "not allow invalid port values" in {
      val server = WebServer()

      val invalidPorts = TableFor1("port", -1, 0, 65536, 65537, -10561857, 7985668)
      forAll(invalidPorts) { port =>
        an[IllegalArgumentException] should be thrownBy {
          server.portNumber = port
        }
      }
    }

    "be settable by command line argument" in {
      val server = WebServer()
      val portNumber = 6771
      server.parseArgs(Array("--port", portNumber.toString))
      server.portNumber shouldBe portNumber
    }

    "cause an error with invalid command line argument" in {
      val server = WebServer()
      val invalidPorts =
        TableFor1("port", "-1", "0", "65536", "65537", "-10561857", "7985668", "x", "test", "123test", "test123", "2e3")
      forAll(invalidPorts) { port =>
        server.parseArgs(Array("--port", port)) shouldBe false
      }
    }
  }

  "Binding address" should {
    val validAddresses: TableFor1[String] =
      TableFor1("address", "0.0.0.0", "255.255.255.255", "127.0.0.1", "56.132.245.78", "21.76.236.85")

    val invalidAddresses: TableFor1[String] = TableFor1(
      "address",
      "256.100.50.25",
      "192.168.1",
      "192.168.1.1.1",
      "192.168.a.10",
      "192.168.-1.1",
      "192..168.1.1",
      "192.168. 1.1"
    )

    "have a default value" in {
      val server = WebServer()
      server.bindingAddress shouldBe "0.0.0.0"
    }

    "be settable" in {
      val server = WebServer()
      forAll(validAddresses) { address =>
        server.bindingAddress = address
        server.bindingAddress shouldBe address
      }
    }

    "not allow invalid addresses" in {
      val server = WebServer()
      forAll(invalidAddresses) { address =>
        an[IllegalArgumentException] should be thrownBy {
          server.bindingAddress = address
        }
      }
    }

    "remove leading zeroes" in {
      val server = WebServer()
      server.bindingAddress = "012.006.000.1"
      server.bindingAddress shouldBe "12.6.0.1"
    }

    "be settable by command line argument" in {
      val server = WebServer()
      forAll(validAddresses) { address =>
        server.parseArgs(Array("--address", address))
        server.bindingAddress shouldBe address
      }
    }

    "cause an error with invalid command line argument" in {
      val server = WebServer()
      forAll(invalidAddresses) { address =>
        server.parseArgs(Array("--address", address)) shouldBe false
      }
    }
  }

  "Skip bundle scripts option" should {
    "default to false" in {
      val server = WebServer()
      server.skipBundleScripts shouldBe false
    }

    "be settable" in {
      val server = WebServer()
      server.skipBundleScripts = true
      server.skipBundleScripts shouldBe true

      server.skipBundleScripts = false
      server.skipBundleScripts shouldBe false
    }

    "be settable by command line argument" in {
      val server = WebServer()
      server.parseArgs(Array("--skip-bundle-scripts"))
      server.skipBundleScripts shouldBe true
    }
  }

  "Multiple options should be be settable by command line arguments" in {
    var server = WebServer()
    server.parseArgs(Array("--port", "1234", "--address", "5.6.7.89", "--skip-bundle-scripts"))
    server.portNumber shouldBe 1234
    server.bindingAddress shouldBe "5.6.7.89"
    server.skipBundleScripts shouldBe true

    server = WebServer()
    server.parseArgs(Array("--skip-bundle-scripts", "--address", "90.123.5.63"))
    server.bindingAddress shouldBe "90.123.5.63"
    server.skipBundleScripts shouldBe true

    server = WebServer()
    server.parseArgs(Array("--port", "8525", "--skip-bundle-scripts"))
    server.portNumber shouldBe 8525
    server.skipBundleScripts shouldBe true

    server = WebServer()
    server.parseArgs(Array("--address", "34.0.0.1", "--port", "40512"))
    server.portNumber shouldBe 40512
    server.bindingAddress shouldBe "34.0.0.1"
  }
}
