import sbt.{CrossVersion, compilerPlugin, _}

object deps {

  import versions._

  lazy val testingDeps = Seq(
    "org.scalatest"  %% "scalatest"  % "3.0.+"  % "test",
    "org.scalacheck" %% "scalacheck" % "1.14.+" % "test"
  )

  lazy val catsDeps = Seq(
    "org.typelevel" %% "cats-core"           % CatsVersion,
    "org.typelevel" %% "cats-effect"         % CatsEffectVersion,
    "org.typelevel" %% "cats-mtl-core"       % CatsMtlVersion,
    "org.typelevel" %% "cats-tagless-macros" % CatsTaglessVersion,
    "org.typelevel" %% "cats-tagless-core"   % CatsTaglessVersion,
  )

  lazy val simulacrum = Seq(
    "com.github.mpilquist" %% "simulacrum" % SimulacrumVersion
  )

  lazy val compilerPlugins: List[ModuleID] =
    List(
      compilerPlugin(
        "org.typelevel" %% "kind-projector" % KindProjector cross CrossVersion.full
      )
    )

}
