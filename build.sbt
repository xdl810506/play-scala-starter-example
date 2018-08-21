name := """play-scala-starter-example"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += "Qunhe Repository" at "http://nexus.qunhequnhe.com/repository/maven-public/"

scalaVersion := "2.12.6"

crossScalaVersions := Seq("2.11.12", "2.12.6")

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies += "org.mongodb" %% "casbah" % "3.1.1" pomOnly()
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.14"
libraryDependencies += "com.typesafe.play" %% "play-slick" % "3.0.3"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "3.0.3"
libraryDependencies += "com.h2database" % "h2" % "1.4.197"
libraryDependencies += "com.h2database" % "h2" % "1.3.175" % Test
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"
libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.12"
libraryDependencies += "com.qunhe.diybe.utils" % "brep" % "0.10.11-SNAPSHOT"
libraryDependencies += "com.qunhe.diybe.module" % "parametric-engine" % "0.2.0" withSources() withJavadoc()
libraryDependencies += "org.mongojack" % "mongojack" % "2.3.0"
libraryDependencies += "com.qunhe.utils" % "mongoutil" % "1.1.0-SNAPSHOT" withSources() withJavadoc()
libraryDependencies += "com.qunhe.utils" % "log" % "1.1.0" withSources() withJavadoc()
libraryDependencies += "org.mongodb" % "mongodb-driver" % "3.8.0"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.6"
libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "0.16.0-play26"
