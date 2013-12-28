scalaVersion := "2.10.2"

name := "Effective Akka"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.2.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test",
  "ch.qos.logback" % "logback-classic" % "1.0.10",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test"
)

