ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(name := "ClickDeduce")
  .aggregate(clickDeduce.js, clickDeduce.jvm)

lazy val clickDeduce = crossProject(JSPlatform, JVMPlatform)
  .in(file("scala"))
  .settings(
    name := "ClickDeduceShared",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.12.0",
      "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.2.0"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.18" % Test,
      "com.github.scopt" %% "scopt" % "4.1.0",
      "net.ruippeixotog" %% "scala-scraper" % "3.1.1",
      "org.scalactic" %% "scalactic" % "3.2.18"
    )
  )
  .jsSettings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    Compile / fastOptJS / artifactPath := baseDirectory.value / ".." / ".." / "webapp" / "dist" / "clickdeduce-opt.js",
    Compile / fullOptJS / artifactPath := baseDirectory.value / ".." / ".." / "webapp" / "dist" / "clickdeduce-opt.js"
  )
