package indigodc.htmframe

import org.apache.mesos
import mesos._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }

import scala.concurrent.duration.Duration
import scala.util.Try

class Scheduler() extends mesos.Scheduler {

  // master role
  private[this] val masterRunning = mutable.Set[Protos.TaskID]();
  private[this] val masterPending = mutable.Set[Protos.TaskID]();
  // submitter role
  private[this] val submitterRunning = mutable.Set[Protos.TaskID]();
  private[this] val submitterPending = mutable.Set[Protos.TaskID]();
  // executor role
  // TODO 
  
  // general
  private[this] val pendingInstances = mutable.Set[Protos.TaskID]();
  private[this] val runningInstances = mutable.Set[Protos.TaskID]();
  
  private[this] var tasksCreated = 0
  private[this] var tasksRunning = 0
  private[this] var shuttingDown: Boolean = false

  def waitForRunningTasks(): Unit = {
    while (tasksRunning > 0) {
      println(s"Shutting down but still have $tasksRunning tasks running.")
      Thread.sleep(3000)
    }
  }

  def shutdown[T](maxWait: Duration)(callback: => T): Unit = {
    println(s"Scheduler shutting down...")
    shuttingDown = true

    val f = Future { waitForRunningTasks() }
    Try { Await.ready(f, maxWait) }

    callback
  }

  def disconnected(driver: SchedulerDriver): Unit =
    println(s"Disconnected from the Mesos master...")

  def error(driver: SchedulerDriver, msg: String): Unit =
    println(s"ERROR: [$msg]")

  def executorLost(
    driver: SchedulerDriver,
    executorId: Protos.ExecutorID,
    slaveId: Protos.SlaveID,
    status: Int): Unit =
    println(s"EXECUTOR LOST: [${executorId.getValue}]")

  def frameworkMessage(
    driver: SchedulerDriver,
    executorId: Protos.ExecutorID,
    slaveId: Protos.SlaveID,
    data: Array[Byte]): Unit = {

    println(s"Received a framework message from [${executorId.getValue}]")

  }

  def offerRescinded(
    driver: SchedulerDriver,
    offerId: Protos.OfferID): Unit =
    println(s"Offer [${offerId.getValue}] has been rescinded")

  def registered(
    driver: SchedulerDriver,
    frameworkId: Protos.FrameworkID,
    masterInfo: Protos.MasterInfo): Unit = {
    val host = masterInfo.getHostname
    val port = masterInfo.getPort
    println(s"Registered with Mesos master [$host:$port]")
  }

  def reregistered(
    driver: SchedulerDriver,
    masterInfo: Protos.MasterInfo): Unit = ???

  def resourceOffers(
    driver: SchedulerDriver,
    offers: java.util.List[Protos.Offer]): Unit = {

    println(s"resourceOffers() with " + offers.size() + " offers")
 
    // this will be valid for executor only
    //val desiredInstances = FarmDescriptor.executorInstances;

    for (offer <- offers.asScala) {

      val tasks = mutable.Buffer[Protos.TaskInfo]()

      if (shuttingDown) {
        println(s"Shutting down: declining offer on [${offer.getHostname}]")
        driver.declineOffer(offer.getId)
      }
      else {
          // launch master
          if ( masterRunning.isEmpty &&  masterPending.isEmpty ) {
              val masterRole: MasterBuilder = new MasterBuilder(offer);
              val masterTask: Protos.TaskInfo = masterRole.taskInfo;
              tasks += masterTask
              println(s"Creating task ${masterTask}" )        
              tasksCreated = tasksCreated + 1
              masterPending.add(masterTask.getTaskId);
          } else if ( submitterRunning.isEmpty &&  submitterPending.isEmpty ) {
          // launch submitter
              val submitterRole: SubmitterBuilder = new SubmitterBuilder(offer);
              val submitterTask: Protos.TaskInfo = submitterRole.taskInfo;
              tasks += submitterTask
              println(s"Creating task ${submitterTask}" )        
              tasksCreated = tasksCreated + 1
              submitterPending.add(submitterTask.getTaskId);
          } else {
              // in this case we deal with executors according to condor queue
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
    println(s"SLAVE LOST: [${slaveId.getValue}]")

  def statusUpdate(
    driver: SchedulerDriver,
    taskStatus: Protos.TaskStatus): Unit = {
    val taskId = taskStatus.getTaskId
    val state = taskStatus.getState
    println(s"Task [${taskId.getValue}] is in state [$state]")
    if (state == Protos.TaskState.TASK_RUNNING){
        if (taskId.toString contains "master") {
            masterPending.remove(taskId);                           
            masterRunning.add(taskId);
        } else if (taskId.toString contains "submitter") {
            submitterPending.remove(taskId);                           
            submitterRunning.add(taskId);
        }
    } else if (state == Protos.TaskState.TASK_FINISHED) {
        if (taskId.toString contains "master") {
            masterPending.remove(taskId);                           
            masterRunning.remove(taskId); 
        } else if (taskId.toString contains "submitter") {
            submitterPending.remove(taskId);                           
            submitterRunning.remove(taskId);
        }
    }
  }

}
