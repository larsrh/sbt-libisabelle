lazy val Fail = config("fail") extend(Compile)

lazy val root = project.in(file("."))
  .configs(Fail)
  .enablePlugins(LibisabellePlugin)
  .settings(
    moduleName := "test",
    isabelleVersions := List(Version.Stable("2018")),
    LibisabellePlugin.isabelleSettings(Fail),
    isabelleSessions in Compile := List("Pure", "Test"),
    isabelleSessions in Fail := List("Test_Fail")
  )
