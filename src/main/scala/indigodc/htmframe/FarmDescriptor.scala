package indigodc.htmframe

import org.apache.mesos
import mesos._

import java.io._
import play.api.libs.json._
import scala.collection.mutable._

object FarmDescriptor { 

    // THIS IS A SINGLETON

    // default values
    var frameworkName:   String = "DummyFramework" 
    var mesosMaster:     String = "localhost:5050"
    var baseImage:       String = "nginx" 
    var waitCycles:         Int = 10 
    var executorsMax:       Int = 10
    var executorsBatch:     Int = 5
    var staticExecutors:    Int = 3
    var networkName:     String = "caliconet" 
    var mesosDNS:        String = "localhost" 
    var dnsDomain:       String = "mesos" 
    var sharedVolumes: List[JsValue] = List()
    var exposeDevice:    String = ""
    var setUlimit:      String = ""
    var condorConfig:    String = "condor_config"
    val roleCpus:   Map[String, Double] = 
                    Map[String, Double](
                                  "master"          -> 0.1,
                                  "submitter"       -> 0.1,
                                  "static_executor" -> 0.1,
                                  "executor"        -> 0.1 
                                  )     
    val roleMem:    Map[String, Int] = 
                    Map[String, Int](
                                  "master"          -> 512,
                                  "submitter"       -> 512,
                                  "static_executor" -> 512,
                                  "executor"        -> 512 
                                  )     
    val roleConfig: Map[String, String] = 
                    Map[String, String](
                                  "master"          -> "",
                                  "submitter"       -> "",
                                  "static_executor" -> "",
                                  "executor"        -> "" 
                                  )     
    val requestAttributes:   Map[String, List[JsValue]] = 
                    Map[String, List[JsValue]](
                                  "master"          -> List(),
                                  "submitter"       -> List(),
                                  "static_executor" -> List(),
                                  "executor"        -> List() 
                                  )     
    val customParameters:   Map[String, List[JsValue]] = 
                    Map[String, List[JsValue]](
                                  "master"          -> List(),
                                  "submitter"       -> List(),
                                  "static_executor" -> List(),
                                  "executor"        -> List() 
                                  )     

    val healthGracePeriodSeconds: Map[String, Int] = 
                    Map[String, Int](
                                  "master"          -> 100,
                                  "submitter"       -> 100,
                                  "static_executor" -> 100,
                                  "executor"        -> 100 
                                  )     
    val healthIntervalSeconds: Map[String, Int] = 
                    Map[String, Int](
                                  "master"          -> 30, 
                                  "submitter"       -> 30,
                                  "static_executor" -> 30,
                                  "executor"        -> 30 
                                  )     
    val healthConsecutiveFailures: Map[String, Int] = 
                    Map[String, Int](
                                  "master"          -> 30,
                                  "submitter"       -> 30,
                                  "static_executor" -> 30,
                                  "executor"        -> 30 
                                  )     

    // READ THE CONFIG FILE
    def loadConfig(configFile: String) = {
      val stream = new FileInputStream(configFile)
      val json: JsValue =  try { Json.parse(stream) } finally { stream.close() }
      // values
      // GENERAL
      try { frameworkName = ((json \ "name").get).as[String] } 
      catch { case _: Throwable => }

      try { mesosMaster = ((json \ "mesos_endpoint").get).as[String] }
      catch { case _: Throwable => }

      try { baseImage = ((json \ "base_image").get).as[String] }
      catch { case _: Throwable => }

      // ELASTICITY
      try { waitCycles = ((json \ "wait_cycles").get).as[Int] }
      catch { case _: Throwable => }

      try { executorsMax = ((json \ "executors_max").get).as[Int] }
      catch { case _: Throwable => }

      try { executorsBatch = ((json \ "executors_batch").get).as[Int] }
      catch { case _: Throwable => }

      try { staticExecutors = ((json \ "static_executors").get).as[Int] }
      catch { case _: Throwable => }

      // NETWORK
      try { networkName = ((json \ "network_name").get).as[String] }
      catch { case _: Throwable => }

      try { mesosDNS = ((json \ "mesos_dns").get).as[String] }
      catch { case _: Throwable => }
       
      try { dnsDomain = ((json \ "dns_domain").get).as[String] }
      catch { case _: Throwable => }

      // SHARED VOLUMES
      try { sharedVolumes = ((json \ "shared_volumes").get).as[List[JsValue]] }
      catch { case _: Throwable => }

      // INFINIBAND 
      try { exposeDevice = ((json \ "device").get).as[String] }
      catch { case _: Throwable => }
      
      try { setUlimit = ((json \ "ulimit").get).as[String] }
      catch { case _: Throwable => }

      // CONDOR CONFIG
      try { condorConfig = ((json \ "condor_config").get).as[String] }
      catch { case _: Throwable => }

      // ROLES
      val roles = Vector("master", "submitter", "static_executor", "executor")

      for (role <- roles) {
          try { roleCpus += (role -> ((json \ role \ "cpus").get).as[Double]) }
          catch { case _: Throwable => }
      
          try { roleMem += (role -> ((json \ role \ "mem").get).as[Int]) }
          catch { case _: Throwable => }

          try { roleConfig += (role -> ((json \ role \ "config").get).as[String]) }
          catch { case _: Throwable => }

          try { healthGracePeriodSeconds += 
            (role -> ((json \ role \ "health_checks" \ "grace_period_seconds").get).as[Int]) }
          catch { case _: Throwable => }

          try { healthIntervalSeconds += 
            (role -> ((json \ role \ "health_checks" \ "interval_seconds").get).as[Int]) }
          catch { case _: Throwable => }

          try { healthConsecutiveFailures += 
            (role -> ((json \ role \ "health_checks" \ "consecutive_failures").get).as[Int]) }
          catch { case _: Throwable => }

          try { requestAttributes += 
            (role -> ((json \ role \ "request_attributes").get.as[List[JsValue]])) }
          catch { case _: Throwable => }

          try { customParameters += 
            (role -> ((json \ role \ "custom_parameters").get.as[List[JsValue]])) }
          catch { case _: Throwable => }

       } 
 
    }
}
