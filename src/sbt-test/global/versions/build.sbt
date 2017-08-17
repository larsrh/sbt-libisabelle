moduleName := "test"

enablePlugins(LibisabellePlugin)

isabelleSessions in Compile := Seq("Test")
isabelleVersions := Seq(Version.Stable("2016"), Version.Stable("2016-1"), Version.Stable("2017-RC0"))
