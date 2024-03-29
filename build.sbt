lazy val root = (project in file("."))
  .settings(
    organization := "de.neuland-bfi",
    name := "bandwhichd-server",
    version := "0.6.0-rc10",
    scalaVersion := "3.1.3",
    Compile / scalaSource := baseDirectory.value / "src" / "main" / "scala",
    Test / scalaSource := baseDirectory.value / "src" / "test" / "scala",
    Test / fork := true,
    run / fork := true,
    run / connectInput := true,
    javaOptions := Seq(
      "-Dorg.slf4j.simpleLogger.log.de.neuland.bandwhichd=debug"
    ),
    ThisBuild / assemblyMergeStrategy := {
      case PathList(ps @ _*) if ps.last endsWith "module-info.class" =>
        MergeStrategy.discard
      case PathList(ps @ _*)
          if ps.last endsWith "io.netty.versions.properties" =>
        MergeStrategy.discard
      case path =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(path)
    },
    libraryDependencies += "co.fs2" %% "fs2-core" % "3.3.0",
    libraryDependencies += "co.fs2" %% "fs2-reactive-streams" % "3.3.0",
    libraryDependencies += "com.comcast" %% "ip4s-core" % "3.2.0",
    libraryDependencies += "com.comcast" %% "ip4s-test-kit" % "3.2.0" % "test",
    libraryDependencies += "com.datastax.oss" % "java-driver-core" % "4.15.0",
    libraryDependencies += "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.40.11" % "test",
    libraryDependencies += "io.circe" %% "circe-core" % "0.14.3",
    libraryDependencies += "io.circe" %% "circe-parser" % "0.14.3",
    libraryDependencies += "org.http4s" %% "http4s-circe" % "1.0.0-M32",
    libraryDependencies += "org.http4s" %% "http4s-core" % "1.0.0-M32",
    libraryDependencies += "org.http4s" %% "http4s-dsl" % "1.0.0-M32",
    libraryDependencies += "org.http4s" %% "http4s-ember-server" % "1.0.0-M32",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % "test",
    libraryDependencies += "org.scalatestplus" %% "scalacheck-1-16" % "3.2.14.0" % "test",
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.3" % "runtime",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.3.14",
    libraryDependencies += "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % "test",
    libraryDependencies += "org.typelevel" %% "log4cats-slf4j" % "2.5.0"
  )
