ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.3"

resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases"

//libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.2.0"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.17"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.17" % "test"

lazy val root = (project in file("."))
  .settings(
    name := "ClickDeduce"
  )
