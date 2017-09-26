package indigodc.htmframe

import org.apache.mesos
import mesos._
import com.typesafe.scalalogging._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }

import scala.concurrent.duration.Duration
import scala.util.Try
import scala.io.Source

class Scheduler() extends mesos.Scheduler with CondorUtils {

  // logging system
  val logger = Logger("Scheduler")
  // master role
  private[this] val masterRunning = mutable.Set[Protos.TaskID]();
  private[this] val masterPending = mutable.Set[Protos.TaskID]();
  private[this] var masterHealthy = false;
  // submitter role
  private[this] val submitterRunning = mutable.Set[Protos.TaskID]();
  private[this] val submitterPending = mutable.Set[Protos.TaskID]();
  private[this] var submitterHealthy = false;
  // executor role 
  private[this] val executorsRunning = mutable.Set[Protos.TaskID]();
  private[this] val executorsPending = mutable.Set[Protos.TaskID]();
  private[this] val executorsIdle    = mutable.Set[Protos.TaskID]();
  private[this] var executorsWait = 0;   
  
  // general
  private[this] val pendingInstances = mutable.Set[Protos.TaskID]();
  private[this] val runningInstances = mutable.Set[Protos.TaskID]();
  
  private[this] var tasksCreated = 0
  private[this] var tasksRunning = 0
  private[this] var shuttingDown: Boolean = false
 
   
  def waitForRunningTasks(): Unit = {
    while (tasksRunning > 0) {
      logger.info(s"Shutting down but still have $tasksRunning tasks running.")
      Thread.sleep(3000)
    }
  }

  def shutdown[T](maxWait: Duration)(callback: => T): Unit = {
    logger.info(s"Scheduler shutting down...")
    shuttingDown = true

    val f = Future { waitForRunningTasks() }
    Try { Await.ready(f, maxWait) }

    callback
  }

  def disconnected(driver: SchedulerDriver): Unit =
    logger.info(s"Disconnected from the Mesos master...")

  def error(driver: SchedulerDriver, msg: String): Unit =
    logger.error(s"ERROR: [$msg]")

  def executorLost(
    driver: SchedulerDriver,
    executorId: Protos.ExecutorID,
    slaveId: Protos.SlaveID,
    status: Int): Unit =
    logger.info(s"EXECUTOR LOST: [${executorId.getValue}]")

  def frameworkMessage(
    driver: SchedulerDriver,
    executorId: Protos.ExecutorID,
    slaveId: Protos.SlaveID,
    data: Array[Byte]): Unit = {

    logger.info(s"Received a framework message from [${executorId.getValue}]")

  }

  def offerRescinded(
    driver: SchedulerDriver,
    offerId: Protos.OfferID): Unit =
    logger.info(s"Offer [${offerId.getValue}] has been rescinded")

  def registered(
    driver: SchedulerDriver,
    frameworkId: Protos.FrameworkID,
    masterInfo: Protos.MasterInfo): Unit = {
    val host = masterInfo.getHostname
    val port = masterInfo.getPort
    logger.info(s"Registered with Mesos master [$host:$port]")
  }

  def reregistered(
    driver: SchedulerDriver,
    masterInfo: Protos.MasterInfo): Unit = ???

  def resourceOffers(
    driver: SchedulerDriver,
    offers: java.util.List[Protos.Offer]): Unit = {

    logger.debug(s"resourceOffers() with " + offers.size() + " offers")
 
    // this will be valid for executor only
    //val desiredInstances = FarmDescriptor.executorInstances;

    for (offer <- offers.asScala) {
      // val res = offer.getAttributesList()
      // println(res)
      val tasks = mutable.Buffer[Protos.TaskInfo]()

      if (shuttingDown) {
        logger.info(s"Shutting down: declining offer on [${offer.getHostname}]")
        driver.declineOffer(offer.getId)
      }
      else {
          // launch master
          if ( masterRunning.isEmpty &&  masterPending.isEmpty ) {
              // check if offer fulfills requested attributes
              if (RequestAttribute.check_attributes(FarmDescriptor.requestAttributes("master"), offer)) {
                  val masterRole: RoleBuilder = new RoleBuilder("master", offer);
                  val masterTask: Protos.TaskInfo = masterRole.taskInfo;
                  tasks += masterTask
                  logger.info(s"Creating task ${masterTask.getName}" )        
                  logger.debug(s"${masterTask}" )        
                  tasksCreated = tasksCreated + 1
                  masterPending.add(masterTask.getTaskId);
              }  
          } else if ( submitterRunning.isEmpty &&  submitterPending.isEmpty ) {
          // launch submitter
          // only if master healthy
              if (masterHealthy){
                  if (RequestAttribute.check_attributes(FarmDescriptor.requestAttributes("submitter"), offer)) { 
                      // println(s"MASTER IS OK!" )        
                      val submitterRole: RoleBuilder = new RoleBuilder("submitter", offer);
                      val submitterTask: Protos.TaskInfo = submitterRole.taskInfo;
                      tasks += submitterTask
                      logger.info(s"Creating task ${submitterTask}" )        
                      tasksCreated = tasksCreated + 1
                      submitterPending.add(submitterTask.getTaskId);
                  }
              } 
          } else {
              // in this case we deal with executors according to condor queue
              // check if submitter is healthy
              //  println(s"SUBMITTER_STATUS: ${submitterHealthy}" )        
              if (submitterHealthy) {
                  // how many jobs in queue
                  val queued_jobs: Int = getQueueSize
                  val idle_nodes: Int = getIdleNodes
                  logger.info(s"Queued jobs: ${queued_jobs}" ) 
                  logger.info(s"Idle nodes: ${idle_nodes}" ) 

                  executorsWait +=1
                  if ( queued_jobs > 0 && executorsWait > FarmDescriptor.waitCycles) {
                      if (RequestAttribute.check_attributes(FarmDescriptor.requestAttributes("executor"), offer)) { 
                          executorsWait = 0 
                          var new_instances = queued_jobs - executorsPending.size - idle_nodes
                          logger.debug(s"Instantiating ${new_instances} new nodes")
                          if (new_instances > FarmDescriptor.executorsBatch)
                              new_instances = FarmDescriptor.executorsBatch
                          if (new_instances < 0)  new_instances = 0
                          for( a <- 1 to new_instances ) {
                              val executorRole: RoleBuilder = new RoleBuilder("executor", offer);
                              val executorTask: Protos.TaskInfo = executorRole.taskInfo;
                              tasks += executorTask
                              logger.info(s"Creating task ${executorTask}" )        
                              tasksCreated = tasksCreated + 1
                              executorsPending.add(executorTask.getTaskId);
                          }
                      }
                  }
              }
          }

        if (tasks.nonEmpty) {
          val filters: Protos.Filters = Protos.Filters.newBuilder().setRefuseSeconds(1).build(); 
          driver.launchTasks(offer.getId, tasks.asJava)
        } else
          driver.declineOffer(offer.getId)
      }
    }
  }

  def slaveLost(
    driver: SchedulerDriver,
    slaveId: Protos.SlaveID): Unit =
    logger.info(s"SLAVE LOST: [${slaveId.getValue}]")

  def statusUpdate(
    driver: SchedulerDriver,
    taskStatus: Protos.TaskStatus): Unit = {
    val taskId = taskStatus.getTaskId
    val state = taskStatus.getState
    val health = taskStatus.getHealthy
    logger.debug(s"Task [${taskId.getValue}] is in state [$state]")
    logger.debug(s"Task [${taskId.getValue}] is in health [$health]")
    if (state == Protos.TaskState.TASK_RUNNING){
        if (taskId.toString contains "master") {
            masterPending.remove(taskId);                           
            masterRunning.add(taskId);
            masterHealthy = health
        } else if (taskId.toString contains "submitter") {
            submitterPending.remove(taskId);                           
            submitterRunning.add(taskId);
            submitterHealthy = health
        } else if (taskId.toString contains "executor") {
            executorsPending.remove(taskId);                           
            executorsRunning.add(taskId);
        }
    } else if (state == Protos.TaskState.TASK_FINISHED || state == Protos.TaskState.TASK_FAILED || state == Protos.TaskState.TASK_KILLED) {
        if (taskId.toString contains "master") {
            masterPending.remove(taskId);                           
            masterRunning.remove(taskId); 
            masterHealthy = false
        } else if (taskId.toString contains "submitter") {
            submitterPending.remove(taskId);                           
            submitterRunning.remove(taskId);
            submitterHealthy = false
        } else if (taskId.toString contains "executor") {
            executorsPending.remove(taskId);                           
            executorsRunning.remove(taskId);
        }
    }
  }

}
