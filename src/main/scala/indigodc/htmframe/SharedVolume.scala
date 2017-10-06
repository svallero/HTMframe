package indigodc.htmframe

import org.apache.mesos
import mesos._
import play.api.libs.json._
import com.typesafe.scalalogging._
import scala.collection.JavaConversions._


class SharedVolume(json: JsValue) {

    val logger = Logger("SharedVolume") 
    var source:      String = ""
    var mount_point: String = ""
    var permissions: Protos.Volume.Mode = Protos.Volume.Mode.RO

    val allowed_permissions: List[String] = List("RW","RO") 

    try { source = ((json \ "source").get).as[String] }
    catch { case _: Throwable => logger.error("Volume source not defined!")}

    try { mount_point = ((json \ "mount_point").get).as[String] }
    catch { case _: Throwable => logger.error("Volume mount point not defined!")}

    try { permissions = Protos.Volume.Mode.valueOf(((json \ "permissions").get).as[String]) }
    catch { case _: Throwable => logger.error("Volume permissions not defined!")}
   
    if (!(allowed_permissions.contains(permissions))) { logger.error("Permissions must be one of: RO, RW!")}

}

object SharedVolume {

    val logger = Logger("SharedVolume") 

    def makeVolumeBuilder(sharedVolume: JsValue): Protos.Volume = {
        logger.info("Creating volume builder")

        val volume = new SharedVolume(sharedVolume)
        logger.debug(s"Source: ${volume.source} Mount-point: ${volume.mount_point} Permissions: ${volume.permissions.toString}")

        val volumeBuilder: Protos.Volume.Builder = Protos.Volume.newBuilder();
        volumeBuilder.setMode(volume.permissions);
        volumeBuilder.setHostPath(volume.source);
        volumeBuilder.setContainerPath(volume.mount_point);
        
        volumeBuilder.build 
    } 
 
}
