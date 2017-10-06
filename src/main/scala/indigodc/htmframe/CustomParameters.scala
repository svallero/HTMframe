package indigodc.htmframe

import org.apache.mesos
import mesos._
import play.api.libs.json._
import com.typesafe.scalalogging._
import scala.collection.JavaConversions._


class CustomParameters(json: JsValue) {

    val logger = Logger("CustomParameters") 
    var key: String = ""
    var value: String = ""

    try { key = ((json \ "key").get).as[String] }
    catch { case _: Throwable => logger.error("Parameter key not defined!")}

    try { value = ((json \ "value").get).as[String] }
    catch { case _: Throwable => logger.error("Parameter value not defined!")}
   
}

object CustomParameters {

    val logger = Logger("CustomParameters") 

    def dockerParameter(key: String, value: String): Protos.Parameter =
      Protos.Parameter.newBuilder
        .setKey(key)
        .setValue(value)
        .build
  
    def dockerParameter(json: JsValue): Protos.Parameter = {
      val param = new CustomParameters(json)
      dockerParameter(param.key, param.value)
    } 
 
}
