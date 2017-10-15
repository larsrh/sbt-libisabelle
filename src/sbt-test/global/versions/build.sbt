moduleName := "test"

enablePlugins(LibisabellePlugin)

isabelleSessions in Compile := Seq("Test")

isabelleVersions := {
  val official = Seq(Version.Stable("2016"), Version.Stable("2016-1"), Version.Stable("2017"))
  if (sys.env.contains("APPVEYOR"))
    official
  else
    official :+ Version.Stable("2015")
}
