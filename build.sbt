ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "ClickDeduce",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.2.0",
      "com.lihaoyi" %% "scalatags" % "0.12.0",
      "org.scalatest" %% "scalatest" % "3.2.17" % "test",
      "com.github.scopt" %% "scopt" % "4.1.0",
      "net.ruippeixotog" %% "scala-scraper" % "3.1.1",
      "org.scalactic" %% "scalactic" % "3.2.17"
    )
  )

lazy val scalaJS = (project in file("scalajs"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(root)
  .settings(
    name := "ClickDeduceScalaJS",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.12.0",
      "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.2.0"
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    artifactPath in (Compile, fastOptJS) := baseDirectory.value / ".." / "webapp" / "scripts" / "clickdeduce-opt.js",
    artifactPath in (Compile, fullOptJS) := baseDirectory.value / ".." / "webapp" / "scripts" / "clickdeduce-opt.js"
  )
