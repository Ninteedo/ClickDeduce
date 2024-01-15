package app

import org.scalatest.concurrent.TimeLimits.failAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Span}

import java.net.{ServerSocket, Socket}

class ServerHostTest extends AnyFunSuite with Matchers {
  def canEstablishConnections: Boolean = {
    try {
      val socket = new Socket("8.8.8.8", 53)
      socket.close()
      true
    } catch {
      case e: Exception => false
    }
  }

  def canHostServer: Boolean = {
    var serverSocket: ServerSocket = null
    try {
      serverSocket = new ServerSocket(0) // 0 lets the system pick an available port
      true
    } catch {
      case e: Exception => false
    } finally {
      if (serverSocket != null) serverSocket.close()
    }
  }

  test("Server binds to correct address and port and can be connected to") {
    if (!canHostServer || !canEstablishConnections) {
      cancel("Unable to host a server on this environment. Test skipped.")
    }

    val port = 27019
    val args: Array[String] = Array("--address", "0.0.0.0", "--port", port.toString, "--skip-bundle-scripts")

    val server = new WebServer()
    val serverThread = new Thread(() => server.runServer(args))
    serverThread.start()

    Thread.sleep(1000) // allows time for the server to start

    failAfter(Span(1000, Millis)) {
      noException should be thrownBy {
        val socket = new Socket("localhost", port)
        val request = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n"
        socket.getOutputStream.write(request.getBytes)
        socket.getOutputStream.flush()
        socket.setSoTimeout(1000)
        val in = socket.getInputStream
        val reader = new java.io.BufferedReader(new java.io.InputStreamReader(in))
        val response = new StringBuilder()
        var line = reader.readLine()
        while (line != null) {
          if (line.contains("HTTP/1.1 200 OK")) {
            response.append(line)
            line = null
          } else {
            response.append(line)
            line = reader.readLine()
          }
        }
        response.toString should include("HTTP/1.1 200 OK")
        socket.close()
      }
    }

    serverThread.interrupt()
  }
}
