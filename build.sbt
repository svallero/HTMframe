name := "htmframe"

scalaVersion := "2.12.2"

fork := false

libraryDependencies ++= Seq(
      "org.apache.mesos"   % "mesos" % "1.1.0", 
      "com.typesafe.play" % "play-json_2.12" % "2.6.1",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.akka" %% "akka-http" % "10.0.10"  
    )

