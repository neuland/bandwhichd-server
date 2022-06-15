lazy val root = (project in file("."))
  .settings(
    organization := "de.neuland-bfi",
    name := "bandwhichd-server",
    version := "0.4.0",
    scalaVersion := "3.1.2",
    Compile / scalaSource := baseDirectory.value / "src" / "main" / "scala",
    Test / scalaSource := baseDirectory.value / "src" / "test" / "scala",
    libraryDependencies += "com.comcast" %% "ip4s-core" % "3.1.3",
    libraryDependencies += "io.circe" %% "circe-core" % "0.14.2",
    libraryDependencies += "io.circe" %% "circe-parser" % "0.14.2",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.3.12",
    libraryDependencies += "org.http4s" %% "http4s-circe" % "1.0.0-M32",
    libraryDependencies += "org.http4s" %% "http4s-core" % "1.0.0-M32",
    libraryDependencies += "org.http4s" %% "http4s-dsl" % "1.0.0-M32",
    libraryDependencies += "org.http4s" %% "http4s-ember-server" % "1.0.0-M32",
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.36" % "runtime",
    libraryDependencies += "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % "test",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % "test"
  )
