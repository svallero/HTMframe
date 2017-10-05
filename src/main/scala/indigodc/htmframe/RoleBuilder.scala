package indigodc.htmframe

import org.apache.mesos._
import scala.collection.JavaConverters._
import java.util.UUID
import scala.annotation.switch

class RoleBuilder(role: String, offer: Protos.Offer) {


   val uuid: UUID = UUID.randomUUID();
   lazy val id = FarmDescriptor.frameworkName + "_" + role + "_" + uuid;
   // variable
   var submitted: Boolean = false;
 
   val dnsName: String = if (!(role contains "executor")) role else id; 
   // InfoBuilders
   lazy val containerInfoBuilder: Protos.ContainerInfo.Builder = 
                                           RoleBuilder.makeContainerBuilder(role);
   lazy val discoveryInfoBuilder: Protos.DiscoveryInfo.Builder = 
                                           //RoleBuilder.makeDiscoveryBuilder(role);
                                           RoleBuilder.makeDiscoveryBuilder(dnsName);
   lazy val healthCheckBuilder: Protos.HealthCheck.Builder = 
                                            //RoleBuilder.makeHealthCheckHttpBuilder(role);
                                           //RoleBuilder.makeHealthCheckBuilder(role);
                                           RoleBuilder.makeHealthCheckBuilder(role, dnsName);
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
            RoleBuilder.scalarResource("cpus", FarmDescriptor.roleCpus(role)),
            RoleBuilder.scalarResource("mem", FarmDescriptor.roleMem(role))
          ).asJava
        )
        .setContainer(containerInfoBuilder)
        .setCommand(commandInfoBuilder)
        .setDiscovery(discoveryInfoBuilder)
        .setHealthCheck(healthCheckBuilder)
        .build


   // argument to entrypoint
   val command: Seq[String] = 
         Seq( "/root/config.json");
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

    // additional device
    // uses a generic parameter builder
    // TODO: add the possibility to add generic parameters to builder
    if (!FarmDescriptor.exposeDevice.isEmpty)
    dockerInfoBuilder.addParameters(dockerParameter("device", FarmDescriptor.exposeDevice)); 
    // ulimits needed for InfiniBand
    if (!FarmDescriptor.setUlimit.isEmpty)
    dockerInfoBuilder.addParameters(dockerParameter("ulimit", FarmDescriptor.setUlimit)); 
    
    // dns
    dockerInfoBuilder.addParameters(dockerParameter("dns", FarmDescriptor.mesosDNS)); 
    // hostname should be the same as DNS name, or healthchecks won't work! 
    if (! (role contains "executor")) dockerInfoBuilder.addParameters(dockerParameter("hostname", s"${role}.${FarmDescriptor.frameworkName}.${FarmDescriptor.dnsDomain}"));

    // NETWORK INFO BUILDER
    // this is needed if you use docker USER network
    val networkInfoBuilder: Protos.NetworkInfo.Builder = 
                   Protos.NetworkInfo.newBuilder.setName(FarmDescriptor.networkName) 

    // VOLUME BUILDER
    val volumeBuilder: Protos.Volume.Builder = Protos.Volume.newBuilder();
    volumeBuilder.setMode(Protos.Volume.Mode.RW );
    volumeBuilder.setHostPath(FarmDescriptor.sharedVolume);
    volumeBuilder.setContainerPath(FarmDescriptor.sharedMount);

    val volumeCondorConfigBuilder: Protos.Volume.Builder = Protos.Volume.newBuilder(); 
    volumeCondorConfigBuilder.setMode(Protos.Volume.Mode.RW );
    volumeCondorConfigBuilder.setHostPath(FarmDescriptor.condorConfig);
    volumeCondorConfigBuilder.setContainerPath("/etc/condor/config.d/condor_custom_config");
    
    val volumeRoleConfigBuilder: Protos.Volume.Builder = Protos.Volume.newBuilder(); 
    volumeRoleConfigBuilder.setMode(Protos.Volume.Mode.RO );
    // var role_tmp = role
    // if (role == "static_executor") role_tmp = "executor"
    volumeRoleConfigBuilder.setHostPath(FarmDescriptor.roleConfig(role));

    volumeRoleConfigBuilder.setContainerPath("/root/config.json");

    // CONTAINER INFO BUILDER 
    val containerInfoBuilder: Protos.ContainerInfo.Builder = 
                                                  Protos.ContainerInfo.newBuilder();
    containerInfoBuilder.setType(Protos.ContainerInfo.Type.DOCKER);
    containerInfoBuilder.setDocker(dockerInfoBuilder.build());
    containerInfoBuilder.addNetworkInfos(networkInfoBuilder);
    containerInfoBuilder.addVolumes(volumeCondorConfigBuilder);
    containerInfoBuilder.addVolumes(volumeRoleConfigBuilder);
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

   
  def makeHealthCheckBuilder(role: String, id: String): Protos.HealthCheck.Builder = { 
    val commandBuilder: Protos.CommandInfo.Builder = Protos.CommandInfo.newBuilder()
       .setValue(
         s"curl -f -X GET http://${id}.${FarmDescriptor.frameworkName}.${FarmDescriptor.dnsDomain}:5000/health"
       ) 
   
    // val gracePeriod = role match{
    //    case "master" => FarmDescriptor.masterGracePeriod
    //    case "submitter" => FarmDescriptor.submitterGracePeriod
    //    case "executor" => FarmDescriptor.executorGracePeriod
    //    case _ => -1
    //} 
            
    // parameters should be in config
    Protos.HealthCheck.newBuilder()
          .setCommand(commandBuilder)
          // .setGracePeriodSeconds(gracePeriod)
          .setGracePeriodSeconds(FarmDescriptor.healthGracePeriodSeconds(role))
          .setIntervalSeconds(FarmDescriptor.healthIntervalSeconds(role))
          .setConsecutiveFailures(FarmDescriptor.healthConsecutiveFailures(role))
  }

  def makeHealthCheckHttpBuilder(role: String): Protos.HealthCheck.Builder = { 
    // looks like it does not work... 
    val httpBuilder: Protos.HealthCheck.HTTPCheckInfo.Builder = 
           Protos.HealthCheck.HTTPCheckInfo.newBuilder()
                    .setPort(5000)
                    .setPath("/health")
                    .setScheme("http")
    
    Protos.HealthCheck.newBuilder()
          .setType(Protos.HealthCheck.Type.HTTP)
          .setHttp(httpBuilder) 
          .setGracePeriodSeconds(100)
          .setIntervalSeconds(30)
          .setConsecutiveFailures(3)
  }

  def makeCommandBuilder(command: Seq[String]): Protos.CommandInfo.Builder = { 

    Protos.CommandInfo.newBuilder()
          .setShell(false) 
          .addAllArguments(command.asJava)
  }
  
}
