package indigodc.htmframe

import org.apache.mesos._
import scala.collection.JavaConverters._
import java.util.UUID

abstract class RoleBuilder(offer: Protos.Offer) {

   val uuid: UUID = UUID.randomUUID();
   lazy val id = FarmDescriptor.frameworkName + "_" + role + "_" + uuid;
   // variable
   var submitted: Boolean = false;
  
   // InfoBuilders
   lazy val containerInfoBuilder: Protos.ContainerInfo.Builder = 
                                           RoleBuilder.makeContainerBuilder(role);
   lazy val discoveryInfoBuilder: Protos.DiscoveryInfo.Builder = 
                                           RoleBuilder.makeDiscoveryBuilder(role);
   lazy val healthCheckBuilder: Protos.HealthCheck.Builder = 
                                           RoleBuilder.makeHealthCheckBuilder(role);
   lazy val commandInfoBuilder: Protos.CommandInfo.Builder = 
                                           RoleBuilder.makeCommandBuilder(command); 

   // TaskInfo
   lazy val taskInfo: Protos.TaskInfo = 
      Protos.TaskInfo.newBuilder
        .setTaskId(Protos.TaskID.newBuilder.setValue(id))
        .setName(id)
        .setSlaveId((offer.getSlaveId))
        .addAllResources(
          Seq(
            RoleBuilder.scalarResource("cpus", cpus),
            RoleBuilder.scalarResource("mem", mem)
          ).asJava
        )
        .setContainer(containerInfoBuilder)
        .setCommand(commandInfoBuilder)
        .setDiscovery(discoveryInfoBuilder)
        .setHealthCheck(healthCheckBuilder)
        .build


   // abstract methods to be implemented for each role
   def role: String;
   def command: Seq[String];
   def cpus: Double;
   def mem: Int;
}

object RoleBuilder {

  def makeContainerBuilder(role: String): Protos.ContainerInfo.Builder = { 

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

    // VOLUME BUILDER
    val volumeBuilder: Protos.Volume.Builder = Protos.Volume.newBuilder();
    volumeBuilder.setMode(Protos.Volume.Mode.RW );
    volumeBuilder.setHostPath(FarmDescriptor.sharedVolume);
    volumeBuilder.setContainerPath(FarmDescriptor.sharedMount);
    
    

    // CONTAINER INFO BUILDER 
    val containerInfoBuilder: Protos.ContainerInfo.Builder = 
                                                  Protos.ContainerInfo.newBuilder();
    containerInfoBuilder.setType(Protos.ContainerInfo.Type.DOCKER);
    containerInfoBuilder.setDocker(dockerInfoBuilder.build());
    containerInfoBuilder.addNetworkInfos(networkInfoBuilder);
    if (role != "master") containerInfoBuilder.addVolumes(volumeBuilder);
    containerInfoBuilder // expected return value
  }
         
  def dockerParameter(key: String, value: String): Protos.Parameter =
    Protos.Parameter.newBuilder
      .setKey(key)
      .setValue(value)
      .build

  def scalarResource(name: String, value: Double): Protos.Resource =
    Protos.Resource.newBuilder
      .setType(Protos.Value.Type.SCALAR)
      .setName(name)
      .setScalar(Protos.Value.Scalar.newBuilder.setValue(value))
      .build

  def makeDiscoveryBuilder(name: String): Protos.DiscoveryInfo.Builder =
    Protos.DiscoveryInfo.newBuilder()
      .setName(name)
      .setVisibility(Protos.DiscoveryInfo.Visibility.EXTERNAL) 

   
  def makeHealthCheckBuilder(role: String): Protos.HealthCheck.Builder = { 
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

  def makeCommandBuilder(command: Seq[String]): Protos.CommandInfo.Builder = { 

    Protos.CommandInfo.newBuilder()
          .setShell(false) 
          .addAllArguments(command.asJava)
  }
 
}
