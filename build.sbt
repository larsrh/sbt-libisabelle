sbtPlugin := true

organization := "info.hupel"
name := "sbt-libisabelle"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-encoding", "UTF-8",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfatal-warnings"
)

homepage := Some(url("http://lars.hupel.info/libisabelle/"))

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

libraryDependencies += "info.hupel" %% "libisabelle-setup" % "0.6.4"

resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("info.hupel.fork.com.vast.sbt" %% "sbt-slf4j" % "0.3")

pomExtra := (
  <developers>
    <developer>
      <id>larsrh</id>
      <name>Lars Hupel</name>
      <url>http://lars.hupel.info</url>
    </developer>
  </developers>
  <scm>
    <connection>scm:git:github.com/larsrh/sbt-libisabelle.git</connection>
    <developerConnection>scm:git:git@github.com:larsrh/sbt-libisabelle.git</developerConnection>
    <url>https://github.com/larsrh/sbt-libisabelle</url>
  </scm>
)

credentials += Credentials(
  Option(System.getProperty("build.publish.credentials")) map (new File(_)) getOrElse (Path.userHome / ".ivy2" / ".credentials")
)

scriptedSettings
scriptedLaunchOpts <+= version apply { v => "-Dproject.version="+v }

scriptedBufferLog := false

// Release stuff

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true)
)
