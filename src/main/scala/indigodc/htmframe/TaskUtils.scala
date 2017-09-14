package indigodc.htmframe

import org.apache.mesos._
import scala.collection.JavaConverters._
import java.util.UUID
import scala.io.Source

trait TaskUtils {

  protected def getQueueSize: Int = { 
     
    val role_url: String = "http://submitter."+FarmDescriptor.frameworkName+".mesos:5001/idle"
     try {
         scala.io.Source.fromURL(role_url).mkString.toInt
     } catch {
         case _: Throwable => -1
     }

  }
}
