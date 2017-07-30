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

libraryDependencies += "info.hupel" %% "libisabelle-setup" % "0.8.3"

resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")

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

// not yet available for 1.0.x
/*
import ReleaseTransformations._

releaseVcsSign := true

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeRelease", _))
)
*/

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)
