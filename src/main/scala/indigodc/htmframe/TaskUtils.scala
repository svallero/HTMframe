package indigodc.htmframe

import org.apache.mesos._
import scala.collection.JavaConverters._
import java.util.UUID

trait TaskUtils {

  protected def dockerParameter(key: String, value: String): Protos.Parameter =
    Protos.Parameter.newBuilder
      .setKey(key)
      .setValue(value)
      .build

  protected def scalarResource(name: String, value: Double): Protos.Resource =
    Protos.Resource.newBuilder
      .setType(Protos.Value.Type.SCALAR)
      .setName(name)
      .setScalar(Protos.Value.Scalar.newBuilder.setValue(value))
      .build

  def makeMasterTask(offer: Protos.Offer): Protos.TaskInfo = {
    //val id = "mastertask"
    val role = "master" 
    // generate UUID
    val uuid: UUID = UUID.randomUUID();
    val id = FarmDescriptor.frameworkName + "_" + role + "_" + uuid; 

    // create the containerInfoBuilder for role
    val containerInfoBuilder: Protos.ContainerInfo.Builder = makeContainerBuilder(role); 
 

    // create commandBuilder
    // here we store the arguments to the container's entrypoint
    val commandInfoBuilder: Protos.CommandInfo.Builder = makeMasterCommandBuilder; 

    val discoveryInfoBuilder: Protos.DiscoveryInfo.Builder = makeDiscoveryBuilder(role);
    
    val healthCheckBuilder: Protos.HealthCheck.Builder = makeHealthCheckBuilder(role);

    // CREATE TASK     
    Protos.TaskInfo.newBuilder
      .setTaskId(Protos.TaskID.newBuilder.setValue(id))
      .setName(id)
      .setSlaveId((offer.getSlaveId))
      .addAllResources(
        Seq(
          scalarResource("cpus", FarmDescriptor.masterCpus),
          scalarResource("mem", FarmDescriptor.masterMem)
        ).asJava
       )
      .setContainer(containerInfoBuilder)
      .setCommand(commandInfoBuilder)
      .setDiscovery(discoveryInfoBuilder)
      .setHealthCheck(healthCheckBuilder)
      .build

  }

  protected def makeHealthCheckBuilder(role: String): Protos.HealthCheck.Builder = { 
    // TODO: put domain in config file 
    val domain = "mesos"
    val commandBuilder: Protos.CommandInfo.Builder = Protos.CommandInfo.newBuilder()
       .setValue(
         s"curl -f -X GET http://${role}.${FarmDescriptor.frameworkName}.${domain}:5000/health"
       ) 

    Protos.HealthCheck.newBuilder()
          .setCommand(commandBuilder)
          .setGracePeriodSeconds(300)
          .setIntervalSeconds(30)
          .setConsecutiveFailures(3)
  }

  protected def makeDiscoveryBuilder(name: String): Protos.DiscoveryInfo.Builder = { 

    Protos.DiscoveryInfo.newBuilder()
          .setName(name)
          .setVisibility(Protos.DiscoveryInfo.Visibility.EXTERNAL)
  }

  protected def makeMasterCommandBuilder: Protos.CommandInfo.Builder = { 

    Protos.CommandInfo.newBuilder()
          .setShell(false) 
          .addAllArguments(
              Seq(
                 "-m",
                 "-r",
                 "https://gitlab.c3s.unito.it/htadmin/AdminKey/raw/master/occam_htadmin.pub",
                 "-S",
                 "pinzillacchero" 
              ).asJava
          )
  }
 
  protected def makeContainerBuilder(role: String): Protos.ContainerInfo.Builder = { 

    // TODO: we could use the parameter "role" here, 
    // for instance to set the container hostname

    // DOCKER INFO BUILDER 
    val dockerInfoBuilder: Protos.ContainerInfo.DockerInfo.Builder = 
                             Protos.ContainerInfo.DockerInfo.newBuilder();
    // image
    dockerInfoBuilder.setImage(FarmDescriptor.baseImage); 
    // network
    // for the time being we support only USER networks
    val net = Protos.ContainerInfo.DockerInfo.Network.USER;
    dockerInfoBuilder.setNetwork(net);
    //dockerInfoBuilder.setForcePullImage(true);
    // dns
    // TODO: domain should be taken from config
    val domain = "mesos"
    dockerInfoBuilder.addAllParameters(
       Seq(
          dockerParameter("dns", FarmDescriptor.mesosDNS), 
          // hostname should be the same as DNS name, or healthchecks won't work! 
          dockerParameter("hostname", s"${role}.${FarmDescriptor.frameworkName}.${domain}")
       ).asJava
    );

    // NETWORK INFO BUILDER
    // this is needed if you use docker USER network
    val networkInfoBuilder: Protos.NetworkInfo.Builder = 
                   Protos.NetworkInfo.newBuilder.setName(FarmDescriptor.networkName) 

    // 
    val containerInfoBuilder: Protos.ContainerInfo.Builder = 
                                                  Protos.ContainerInfo.newBuilder();
    containerInfoBuilder.setType(Protos.ContainerInfo.Type.DOCKER);
    containerInfoBuilder.setDocker(dockerInfoBuilder.build());
    containerInfoBuilder.addNetworkInfos(networkInfoBuilder);
  }
}
