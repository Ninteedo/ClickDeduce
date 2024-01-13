ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases"
resolvers += "Akka library repository" at "https://repo.akka.io/maven"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.2.0"
libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.13.10"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.17"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.17" % "test"
libraryDependencies += "com.lihaoyi" %% "scalatags" % "0.12.0"
libraryDependencies += "com.github.scopt" %% "scopt" % "4.1.0"

val AkkaHttpVersion = "10.5.0"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.8.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.8.0" % Test

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "3.1.1"

lazy val root = (project in file("."))
  .settings(
    name := "ClickDeduce"
  )
