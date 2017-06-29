package indigodc.htmframe

import org.apache.mesos._
import com.google.protobuf.ByteString
import scala.collection.JavaConverters._
import java.io.File

trait TaskUtils {

  //def rendlerHome(): File

  //val TASK_CPUS = 1.0
  //val TASK_MEM = 32.0

  //protected[this] val rendlerUris: Seq[Protos.CommandInfo.URI] =
  //  Seq(
  //    "render.js",
  //    "python/crawl_executor.py",
  //    "python/export_dot.py",
  //    "python/render_executor.py",
  //    "python/results.py",
  //    "python/task_state.py"
  //  ).map {
  //      fName =>
  //        Protos.CommandInfo.URI.newBuilder
  //          .setValue(new File(rendlerHome, fName).getAbsolutePath)
  //          .setExtract(false)
  //          .build
  //    }

  //lazy val crawlExecutor: Protos.ExecutorInfo = {
  //  val command = Protos.CommandInfo.newBuilder
  //    .setValue("python crawl_executor.py")
  //    .addAllUris(rendlerUris.asJava)
  //  Protos.ExecutorInfo.newBuilder
  //    .setExecutorId(Protos.ExecutorID.newBuilder.setValue("crawl-executor"))
  //    .setName("Crawler")
  //    .setCommand(command)
  //    .build
  //}

  //lazy val renderExecutor: Protos.ExecutorInfo = {
  //  val command = Protos.CommandInfo.newBuilder
  //    .setValue("python render_executor.py --local")
  //    .addAllUris(rendlerUris.asJava)
  //  Protos.ExecutorInfo.newBuilder
  //    .setExecutorId(Protos.ExecutorID.newBuilder.setValue("render-executor"))
  //    .setName("Renderer")
  //    .setCommand(command)
  //    .build
  //}

  def makeTaskPrototype(id: String, offer: Protos.Offer): Protos.TaskInfo = {
    // CONTAINER SPECS
    val imageName = "alpine"
    val network = Protos.ContainerInfo.DockerInfo.Network.BRIDGE
    val cpus = 1.0
    val mem = 32.0

    val dockerInfoBuilder: Protos.ContainerInfo.DockerInfo.Builder = 
                             Protos.ContainerInfo.DockerInfo.newBuilder();
    // image
    dockerInfoBuilder.setImage(imageName); 
    // network
    dockerInfoBuilder.setNetwork(network);
    // container
    val containerInfoBuilder: Protos.ContainerInfo.Builder = Protos.ContainerInfo.newBuilder();
    containerInfoBuilder.setType(Protos.ContainerInfo.Type.DOCKER);
    containerInfoBuilder.setDocker(dockerInfoBuilder.build());

    // CREATE TASK     
    Protos.TaskInfo.newBuilder
      .setTaskId(Protos.TaskID.newBuilder.setValue(id))
      .setName("")
      .setSlaveId((offer.getSlaveId))
      .addAllResources(
        Seq(
          scalarResource("cpus", cpus),
          scalarResource("mem", mem)
        ).asJava
       )
      .setContainer(containerInfoBuilder)
      .setCommand(Protos.CommandInfo.newBuilder().setShell(false))
      .build
  }

  protected def scalarResource(name: String, value: Double): Protos.Resource =
    Protos.Resource.newBuilder
      .setType(Protos.Value.Type.SCALAR)
      .setName(name)
      .setScalar(Protos.Value.Scalar.newBuilder.setValue(value))
      .build

  def makeSimpleTask(
    id: String,
    offer: Protos.Offer): Protos.TaskInfo =
    makeTaskPrototype(id, offer).toBuilder
      .setName(s"simple_$id")
      //.setData(ByteString.copyFromUtf8(url))
      .build

  //def makeRenderTask(
  //  id: String,
  //  url: String,
  //  offer: Protos.Offer): Protos.TaskInfo =
  //  makeTaskPrototype(id, offer).toBuilder
  //   .setName(s"render_$id")
  //    .setExecutor(renderExecutor)
  //    .setData(ByteString.copyFromUtf8(url))
  //    .build

  //def maxTasksForOffer(
  //  offer: Protos.Offer,
   // cpusPerTask: Double = TASK_CPUS,
    //memPerTask: Double = TASK_MEM): Int = {
  //  var count = 0
  //  var cpus = 0.0
  //  var mem = 0.0

  //  for (resource <- offer.getResourcesList.asScala) {
  //    resource.getName match {
  //      case "cpus" => cpus = resource.getScalar.getValue
  //      case "mem"  => mem = resource.getScalar.getValue
  //      case _      => ()
  //    }
  //  }

  //  while (cpus >= TASK_CPUS && mem >= TASK_MEM) {
  //    count = count + 1
  //    cpus = cpus - TASK_CPUS
  //    mem = mem - TASK_MEM
  //  }

  //  count
  //}

  //def isTerminal(state: Protos.TaskState): Boolean = {
  //  import Protos.TaskState._
  //  state match {
  //    case TASK_FINISHED | TASK_FAILED | TASK_KILLED | TASK_LOST =>
  //      true
  //    case _ =>
  //      false
  //  }
 // }

}
