package indigodc.htmframe


import org.apache.mesos._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object HTMframe {

  lazy val frameworkInfo: Protos.FrameworkInfo =
    Protos.FrameworkInfo.newBuilder
      .setName(FarmDescriptor.frameworkName)
      .setFailoverTimeout(60.seconds.toMillis) // for PRODUCTION: 1 week 
      .setCheckpoint(false) // for PRODUCTION: true
      .setUser("root") // user must exist in the Mesos container
      .build

  def printUsage(): Unit = {
    println("""
      |Usage:
      |  run <config_file> 
    """.stripMargin)
  }

  def main(args: Array[String]): Unit = {

    if (args.length != 1) {
      printUsage()
      sys.exit(1)
    }

    // READ THE CONFIG FILE
    val Seq(configFile) = args.toSeq
    
    // CONSTRUCT FARM DESCRIPTOR 
    // singleton
    FarmDescriptor.loadConfig(configFile)

    println(s"""
      |HTMframe
      |=======
      |
      |frameworkName:      [${FarmDescriptor.frameworkName}]
      |mesosMaster:        [${FarmDescriptor.mesosMaster}]
      |baseImage:          [${FarmDescriptor.baseImage}]
      |networkName:        [${FarmDescriptor.networkName}]
      |mesosDNS:           [${FarmDescriptor.mesosDNS}]
      |sharedVolume:       [${FarmDescriptor.sharedVolume}]
      |sharedMount:        [${FarmDescriptor.sharedMount}]
      |masterCpus:         [${FarmDescriptor.masterCpus}]
      |masterMem:          [${FarmDescriptor.masterMem}]
      |submitterCpus:      [${FarmDescriptor.submitterCpus}]
      |submitterMem:       [${FarmDescriptor.submitterMem}]
      |executorCpus:       [${FarmDescriptor.executorCpus}]
      |executorMem:        [${FarmDescriptor.executorMem}]
      |executorInstances:  [${FarmDescriptor.executorInstances}]
      |
    """.stripMargin)

    val scheduler = new Scheduler()

    // SV: bisognerebbe usare l'HTTP API al posto del driver...
    val driver: SchedulerDriver =
      new MesosSchedulerDriver(scheduler, frameworkInfo, FarmDescriptor.mesosMaster)

    // driver.run blocks; therefore run in a separate thread
    // SV: in real life we want to run this in a container, 
    // so blocking will be ok. 
    Future { driver.run }

    // wait for the enter key
    val NEWLINE = '\n'.toInt
    while (System.in.read != NEWLINE) {
    Thread.sleep(1000)
    }

    // graceful shutdown
    scheduler.shutdown(5.minutes) { driver.stop() }
    sys.exit(0)
  }
}