import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, Uri}
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

import java.nio.file.{Files, Paths}
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

object SimpleHttpServerApp1 extends HttpApp {
  def readFileAsString(file: String): String =
    new String(Files.readAllBytes(Paths.get(file)))
  override def routes: Route =
    pathPrefix("simple") {
      path("num" / IntNumber) { num =>
        get {
          implicit val system = ActorSystem()
          implicit val materializer = ActorMaterializer()
          import system.dispatcher

          val file: String = "src/test/resources/myFile.json"
          val json: String = readFileAsString(file)
          val num = json.getBytes().length
          //prepare requests
          val baseUrl = "http://127.0.0.1:9000/simple/num/"
          val data = Seq(("POST", 1, json))
          val reqs = data.map { case (a, b, c) =>
            HttpRequest(POST, Uri(baseUrl + b), Nil, HttpEntity(ContentTypes.`text/plain(UTF-8)`, ByteString(c)))
          }
          Future.traverse(reqs)(Http().singleRequest(_)) andThen {
            case Success(resps) => resps.foreach(resp =>
              resp.entity.toStrict(5 seconds).map(_.data.utf8String).andThen {
                case Success(content) => {
                  if (Integer.parseInt(content) == num) {
                    println("Response : Content transferred successfully")
                  }
                  else {
                    println("Response : Content transfer failed")
                  }
                }
                case _ => println("Error")
              })
            case Failure(err) => println(s"Request failed $err")
          }
          println("Received request")
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "sending response"))
        }
      }
    }
}

object SimpleHttpServer1 extends App {
  SimpleHttpServerApp1 startServer("localhost", 8000, ServerSettings(ConfigFactory.load()))
}                                                                                                                               
                                                                                                                                