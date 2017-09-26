package indigodc.htmframe

import org.apache.mesos
import mesos._
import play.api.libs.json._
import com.typesafe.scalalogging._
import scala.collection.JavaConversions._


class RequestAttribute(json: JsValue) {

    val logger = Logger("RequestAttribute") 
    var name: String = ""
    var tipe: String = ""
    var value: String = ""

    val allowed_types: List[String] = List("TEXT") 

    try { name = ((json \ "name").get).as[String] }
    catch { case _: Throwable => logger.error("Attribute name not defined!")}

    try { tipe = ((json \ "type").get).as[String] }
    catch { case _: Throwable => logger.error("Attribute type not defined!")}
   
    if (!(allowed_types.contains(tipe))) { logger.error("Only type TEXT allowed!")}

    try { value = ((json \ "value").get).as[String] }
    catch { case _: Throwable => logger.error("Attribute value not defined!")}
}

object RequestAttribute {

    val logger = Logger("RequestAttribute") 
    def check_attributes(attributes: List[JsValue], offer: Protos.Offer): Boolean = {
        logger.info("Checking request attributes")
        if (attributes.isEmpty) {
            logger.debug("no attribute requested")
            return true 
        }
        var found: Boolean = false
        for (attribute <- attributes) {
            val attr = new RequestAttribute(attribute)
            found = false
            for (a <- offer.getAttributesList()) {
                logger.debug(s"Name: ${attr.name} Value: ${attr.value}")
                if (a.getName.equals(attr.name)) {
                    found = true
                    if (a.getText.getValue != attr.value) {
                        logger.debug(s"Offer: ${a.getText.getValue} Request: ${attr.value}")
                        logger.debug("Requested attribute not matched!") 
                        return false
                    } else logger.debug("Requested attribute matched!")
                } 
            }  
        } 
        
        if (! found) logger.debug("Requested attribute not found!") 
        found
    } 
 
}
