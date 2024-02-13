ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val ClickDeduce = (project in file("."))
  .settings(name := "ClickDeduce")
  .aggregate(crossProjectRoot.js, crossProjectRoot.jvm)

lazy val crossProjectRoot = crossProject(JSPlatform, JVMPlatform).in(file("scala"))
  .settings(
    name := "ClickDeduceCrossProject",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.12.0",
      "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.2.0"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.17" % "test",
      "com.github.scopt" %% "scopt" % "4.1.0",
      "net.ruippeixotog" %% "scala-scraper" % "3.1.1",
      "org.scalactic" %% "scalactic" % "3.2.17"
    )
  )
  .jsSettings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    artifactPath in (Compile, fastOptJS) := baseDirectory.value / ".." / ".." / "webapp" / "scripts" / "clickdeduce-opt.js",
    artifactPath in (Compile, fullOptJS) := baseDirectory.value / ".." / ".." / "webapp" / "scripts" / "clickdeduce-opt.js"
  )
