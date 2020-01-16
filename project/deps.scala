import sbt.{CrossVersion, compilerPlugin, _}

object deps {

  import versions._

  lazy val testing = Seq(
    "org.scalatest"  %% "scalatest"  % "3.0.+"  % "test",
    "org.scalacheck" %% "scalacheck" % "1.14.+" % "test"
  )

  lazy val cats = Seq(
    "org.typelevel" %% "cats-core"           % CatsVersion,
    "org.typelevel" %% "cats-effect"         % CatsEffectVersion,
    "org.typelevel" %% "cats-mtl-core"       % CatsMtlVersion,
    "org.typelevel" %% "cats-tagless-macros" % CatsTaglessVersion,
    "org.typelevel" %% "cats-tagless-core"   % CatsTaglessVersion,
    "com.olegpy"    %% "meow-mtl-core"       % CatsMeowMtl,
  )

  lazy val tofu = Seq(
    "ru.tinkoff" %% "tofu-core" % Tofu
  )

  lazy val monix = Seq(
    "io.monix" %% "monix" % Monix
  )

  lazy val zio = Seq(
    "dev.zio" %% "zio"              % "1.0.0-RC15",
    "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC9"
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
