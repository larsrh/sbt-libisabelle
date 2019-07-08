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

licenses := Seq("Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0"))

libraryDependencies += "info.hupel" %% "libisabelle-setup" % "1.0.1"

resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")

enablePlugins(ScriptedPlugin)

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

scriptedLaunchOpts += s"-Dproject.version=${version.value}"

scriptedBufferLog := false

// Release stuff

import ReleaseTransformations._

releaseVcsSign := true

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeRelease")
)

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)
