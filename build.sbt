name := "ScalaHelloworld"

version := "0.1"

scalaVersion := "2.13.6"

val AkkaVersion = "2.6.14"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "com.lightbend.akka" %% "akka-stream-alpakka-file" % "3.0.1",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion
)