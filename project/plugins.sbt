// Not yet available for 1.0.x
//addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.4")
//addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.0")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0-M1")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
