moduleName := "test"

enablePlugins(LibisabellePlugin)

isabelleSessions in Compile := Seq("Test")
isabelleVersions := Seq("2016", "2016-1-RC1", "2016-1-RC2")
