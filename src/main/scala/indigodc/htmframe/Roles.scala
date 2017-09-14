package indigodc.htmframe

import org.apache.mesos._

class MasterBuilder(offer: Protos.Offer) extends RoleBuilder(offer) {

   // abstract methods implementation
   val role: String = "master";
   val command: Seq[String] = 
       Seq( "-m",
            "-r",
            "https://gitlab.c3s.unito.it/htadmin/AdminKey/raw/master/occam_htadmin.pub",
            "-S",
            "pinzillacchero" 
       )
   ;
   val cpus: Double = FarmDescriptor.masterCpus;
   val mem: Int = FarmDescriptor.masterMem;
}

class SubmitterBuilder(offer: Protos.Offer) extends RoleBuilder(offer) {

   // abstract methods implementation
   val role: String = "submitter";
   val command: Seq[String] = 
       Seq( "-s",
            "master."+FarmDescriptor.frameworkName+".mesos",
            "-r",
            "https://gitlab.c3s.unito.it/htadmin/AdminKey/raw/master/occam_htadmin.pub",
            "-S",
            "pinzillacchero" 
       )
   ;
   val cpus: Double = FarmDescriptor.submitterCpus;
   val mem: Int = FarmDescriptor.submitterMem;
}

class ExecutorBuilder(offer: Protos.Offer) extends RoleBuilder(offer) {

   // abstract methods implementation
   val role: String = "executor";
   val command: Seq[String] = 
       Seq( "-e",
            "master."+FarmDescriptor.frameworkName+".mesos",
            "-r",
            "https://gitlab.c3s.unito.it/htadmin/AdminKey/raw/master/occam_htadmin.pub",
            "-S",
            "pinzillacchero" 
       )
   ;
   val cpus: Double = FarmDescriptor.executorCpus;
   val mem: Int = FarmDescriptor.executorMem;
}
