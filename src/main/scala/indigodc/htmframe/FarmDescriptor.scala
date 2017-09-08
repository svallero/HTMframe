package indigodc.htmframe

import org.apache.mesos
import mesos._

import java.io._
import play.api.libs.json._

object FarmDescriptor { 

    // THIS IS A SINGLETON

    // default values
    var frameworkName:  String = "DummyFramework" 
    var mesosMaster:    String = "localhost:5050"
    var baseImage:      String = "nginx" 
    var networkName:    String = "caliconet" 
    var mesosDNS:       String = "localhost" 
    var masterCpus:     Double = 0.1 
    var masterMem:         Int = 512 
    var submitterCpus:  Double = 0.1 
    var submitterMem:      Int = 512 
    var executorCpus:   Double = 0.1 
    var executorMem:       Int = 512 
    var executorInstances: Int = 3
    var sharedVolume:   String = "/home/"
    var sharedMount:    String = "/home/"

    // READ THE CONFIG FILE
    def loadConfig(configFile: String) = {
      val stream = new FileInputStream(configFile)
      val json: JsValue =  try { Json.parse(stream) } finally { stream.close() }
      // values
      try { frameworkName = ((json \ "name").get).as[String] } 
      catch { case _: Throwable => }

      try { mesosMaster = ((json \ "mesos_endpoint").get).as[String] }
      catch { case _: Throwable => }

      try { baseImage = ((json \ "base_image").get).as[String] }
      catch { case _: Throwable => }

      try { networkName = ((json \ "network_name").get).as[String] }
      catch { case _: Throwable => }

      try { mesosDNS = ((json \ "mesos_dns").get).as[String] }
      catch { case _: Throwable => }

      try { masterCpus = ((json \ "master" \ "cpus").get).as[Double] }
      catch { case _: Throwable => }

      try { masterMem = ((json \ "master" \ "mem").get).as[Int] }
      catch { case _: Throwable => }
  
      try { submitterCpus = ((json \ "submitter" \ "cpus").get).as[Double] }
      catch { case _: Throwable => }

      try { submitterMem = ((json \ "submitter" \ "mem").get).as[Int] }
      catch { case _: Throwable => }

      try { executorCpus = ((json \ "executor" \ "cpus").get).as[Double] }
      catch { case _: Throwable => }

      try { executorMem = ((json \ "executor" \ "mem").get).as[Int] }
      catch { case _: Throwable => }
                               
      try { executorInstances = ((json \ "executor" \ "num_instances").get).as[Int] }
      catch { case _: Throwable => }

      try { sharedVolume = ((json \ "shared_volume").get).as[String] }
      catch { case _: Throwable => }

      try { sharedMount = ((json \ "shared_mount").get).as[String] }
      catch { case _: Throwable => }
    }
}
