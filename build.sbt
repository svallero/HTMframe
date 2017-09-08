name := "htmframe"

scalaVersion := "2.12.2"

fork := false

libraryDependencies ++= Seq(
      "org.apache.mesos"   % "mesos" % "1.1.0", 
      "com.typesafe.play" % "play-json_2.12" % "2.6.1" 
    )

