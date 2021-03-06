package indigodc.htmframe


import org.apache.mesos._
import com.typesafe.scalalogging._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

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

    val logger = Logger("HTMframe")
    // println(s"""
    logger.info(s"""
      |HTMframe
      |=======
      |
      |frameworkName:                  [${FarmDescriptor.frameworkName}]
      |mesosMaster:                    [${FarmDescriptor.mesosMaster}]
      |baseImage:                      [${FarmDescriptor.baseImage}]
      |waitCycles:                     [${FarmDescriptor.waitCycles}]
      |executorsMax:                   [${FarmDescriptor.executorsMax}]
      |executorsBatch:                 [${FarmDescriptor.executorsBatch}]
      |staticExecutors:                [${FarmDescriptor.staticExecutors}]
      |networkName:                    [${FarmDescriptor.networkName}]
      |mesosDNS:                       [${FarmDescriptor.mesosDNS}]
      |dnsDomain:                      [${FarmDescriptor.dnsDomain}]
      |sharedVolumes:                  [${FarmDescriptor.sharedVolumes}]
      |exposeDevice:                   [${FarmDescriptor.exposeDevice}]
      |setUlimit:                      [${FarmDescriptor.setUlimit}]
      |condorConfig:                   [${FarmDescriptor.condorConfig}]
      |masterCpus:                     [${FarmDescriptor.roleCpus("master")}]
      |masterMem:                      [${FarmDescriptor.roleMem("master")}]
      |masterConfig:                   [${FarmDescriptor.roleConfig("master")}]
      |masterRequestAttributes:        [${FarmDescriptor.requestAttributes("master")}]
      |masterCustomParameters:         [${FarmDescriptor.customParameters("master")}]
      |masterHcGracePeriod:            [${FarmDescriptor.healthGracePeriodSeconds("master")}]
      |masterHcInterval:               [${FarmDescriptor.healthIntervalSeconds("master")}]
      |masterHcConsecutiveFailures:    [${FarmDescriptor.healthConsecutiveFailures("master")}]
      |submitterCpus:                  [${FarmDescriptor.roleCpus("submitter")}]
      |submitterMem:                   [${FarmDescriptor.roleMem("submitter")}]
      |submitterConfig:                [${FarmDescriptor.roleConfig("submitter")}]
      |SubmitterRequestAttributes:     [${FarmDescriptor.requestAttributes("submitter")}]
      |submitterCustomParameters:      [${FarmDescriptor.customParameters("submitter")}]
      |submitterHcGracePeriod:         [${FarmDescriptor.healthGracePeriodSeconds("submitter")}]
      |submitterHcInterval:            [${FarmDescriptor.healthIntervalSeconds("submitter")}]
      |submitterHcConsecutiveFailures: [${FarmDescriptor.healthConsecutiveFailures("submitter")}]
      |executorCpus:                   [${FarmDescriptor.roleCpus("executor")}]
      |executorMem:                    [${FarmDescriptor.roleMem("executor")}]
      |executorConfig:                 [${FarmDescriptor.roleConfig("executor")}]
      |executorRequestAttributes:      [${FarmDescriptor.requestAttributes("executor")}]
      |executorCustomParameters:       [${FarmDescriptor.customParameters("executor")}]
      |executorHcGracePeriod:          [${FarmDescriptor.healthGracePeriodSeconds("executor")}]
      |executorHcInterval:             [${FarmDescriptor.healthIntervalSeconds("executor")}]
      |executorHcConsecutiveFailures:  [${FarmDescriptor.healthConsecutiveFailures("executor")}]
      |staticExecutorCpus:                   [${FarmDescriptor.roleCpus("static_executor")}]
      |staticExecutorMem:                    [${FarmDescriptor.roleMem("static_executor")}]
      |staticExecutorConfig:                 [${FarmDescriptor.roleConfig("static_executor")}]
      |staticExecutorRequestAttributes:      [${FarmDescriptor.requestAttributes("static_executor")}]
      |staticExecutorCustomParameters:       [${FarmDescriptor.customParameters("static_executor")}]
      |staticExecutorHcGracePeriod:          [${FarmDescriptor.healthGracePeriodSeconds("static_executor")}]
      |staticExecutorHcInterval:             [${FarmDescriptor.healthIntervalSeconds("static_executor")}]
      |staticExecutorHcConsecutiveFailures:  [${FarmDescriptor.healthConsecutiveFailures("static_executor")}]
      |
    """.stripMargin)

    val scheduler = new Scheduler()

    // SV: bisognerebbe usare l'HTTP API al posto del driver...
    val driver: SchedulerDriver =
      new MesosSchedulerDriver(scheduler, frameworkInfo, FarmDescriptor.mesosMaster)

    // Start the Akka server
    val webServer = new HttpServer(scheduler);

    webServer.start

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

    // Akka server
    webServer.stop

    // Scheduler
    scheduler.shutdown(5.minutes) { driver.stop() }
    sys.exit(0)
  }
}
