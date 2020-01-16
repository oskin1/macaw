lazy val commonSettings = Seq(
  scalacOptions ++= commonScalacOptions,
  organization := "com.github.oskin1",
  scalaVersion := "2.13.1",
  version := "0.1.0"
)

lazy val macaw = project
  .in(file("."))
  .withId("macaw")
  .settings(commonSettings)
  .settings(moduleName := "macaw", name := "Macaw")
  .aggregate(core, data, demo)

lazy val core = utils
  .mkModule("macaw-core", "MacawCore")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= deps.cats ++ deps.simulacrum ++ deps.testing ++ deps.compilerPlugins
  )

lazy val data = utils
  .mkModule("macaw-data", "MacawData")
  .settings(commonSettings)
  .settings(libraryDependencies ++= deps.testing)

lazy val demo = utils
  .mkModule("demo", "Demo")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= deps.cats ++ deps.tofu ++ deps.zio ++ deps.monix ++ deps.testing ++ deps.compilerPlugins
  )
  .dependsOn(core, data)

lazy val commonScalacOptions = List(
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-language:higherKinds",
  "-language:existentials",
  "-language:implicitConversions"
)
