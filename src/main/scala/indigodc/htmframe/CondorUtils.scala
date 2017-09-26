package indigodc.htmframe

import org.apache.mesos._
import scala.collection.JavaConverters._
import java.util.UUID
import scala.io.Source

trait CondorUtils {

  protected def getQueueSize: Int = { 
     
     val role_url: String = "http://submitter."+FarmDescriptor.frameworkName+".mesos:5001/idle_jobs"
    // val role_url: String = "http://submitter."+FarmDescriptor.frameworkName+".mesos:5001/idle"
    getUrl(role_url)
  }

  protected def getIdleNodes: Int = { 
     
    val idle_url: String = "http://submitter."+FarmDescriptor.frameworkName+".mesos:5001/idle_nodes"
    getUrl(idle_url)
  }

  protected def getUrl(url: String): Int = { 

     try {
         scala.io.Source.fromURL(url).mkString.toInt
     } catch {
         case _: Throwable => -1
     }
  }
}
