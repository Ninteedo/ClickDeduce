ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.3"

resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases"
resolvers += "Akka library repository" at "https://repo.akka.io/maven"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.0"
libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.13.5"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.17"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.17" % "test"
libraryDependencies += "com.lihaoyi" %% "scalatags" % "0.11.1"
libraryDependencies += "com.github.scopt" %% "scopt" % "4.1.0"

val AkkaHttpVersion = "10.5.0"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.8.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.8.0" % Test

lazy val root = (project in file("."))
  .settings(
    name := "ClickDeduce"
  )
