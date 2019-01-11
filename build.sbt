name := "macaw"

version := "0.1.0"

scalaVersion := "2.12.8"

organization := "com.github.oskin1"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.+" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.+" % "test",
  "com.storm-enroute" %% "scalameter" % "0.8.+" % "test"
)

licenses in ThisBuild := Seq("GNU GPL 3.0" -> url("https://github.com/oskin1/macaw/blob/master/LICENSE"))

homepage in ThisBuild := Some(url("https://github.com/oskin1/macaw"))

publishMavenStyle in ThisBuild := true

publishArtifact in Test := false

publishTo in ThisBuild :=
  Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging)

pomExtra in ThisBuild :=
  <scm>
    <url>git@github.com:oskin1/macaw.git</url>
    <connection>scm:git:git@github.com:oskin1/macaw.git</connection>
  </scm>
    <developers>
      <developer>
        <id>Oskin1</id>
        <name>Ilya Oskin</name>
      </developer>
    </developers>