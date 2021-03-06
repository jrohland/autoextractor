name := "autoextractor"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "org.apache.logging.log4j" %% "log4j-api-scala" % "11.0",
  "org.apache.logging.log4j" % "log4j-api" % "2.8.2",
  "org.apache.logging.log4j" % "log4j-core" % "2.8.2" % Runtime,
  "com.typesafe.akka" %% "akka-actor" % "2.5.6",
  "org.asm-labs" % "junrar" % "0.8"
)
