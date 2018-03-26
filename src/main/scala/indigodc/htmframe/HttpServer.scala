package indigodc.htmframe

import org.apache.mesos
import mesos._
import play.api.libs.json._
import com.typesafe.scalalogging._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.RouteResult 

class HttpServer(scheduler: Scheduler) {
    val logger = Logger("HttpServer")
    logger.info("Starting Akka server...")

    // TODO: should be configurable    
    val hostname = "localhost"
    val port = 8080

    implicit val system = ActorSystem("htmframe")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher


    var bindingFuture: Future[ServerBinding] = _;
    
    // Start the server
    def start(): Boolean = {
        val routes = 
          pathPrefix(FarmDescriptor.frameworkName) {
            pathEnd {
              complete("TODO: put config.json here!!!")
            } ~
            path("masterHealthy") {
              get {
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, if (scheduler.masterHealthy) "TRUE" else "FALSE"))
              }
            } ~
            path("submitterHealthy") {
             get {
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, if (scheduler.submitterHealthy) "TRUE" else "FALSE"))
              }
            } ~ 
            path("staticExecutorsHealthy") {
              get {
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, scheduler.staticExecutorsHealthy.size.toString))
              }
            } ~ 
            path("executorsHealthy") {
              get {
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, scheduler.executorsHealthy.size.toString))
              }
            }  
        }
        // TODO: put try and catch here
        bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)
        logger.info(s"Akka server online at http://localhost:8080")
        true 
    }

    def stop(): Boolean = {
        bindingFuture
          .flatMap(_.unbind()) // trigger unbinding from the port
          .onComplete(_ => system.terminate()) // and shutdown when done
        true
    }
  
}

