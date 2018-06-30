moduleName := "test"

enablePlugins(LibisabellePlugin)

isabelleSessions in Compile := Seq("Test")

isabelleVersions := {
  val official = Seq(Version.Stable("2018-RC0"))
  if (sys.env.contains("APPVEYOR"))
    official
  else
    official :+ Version.Stable("2016")
}
