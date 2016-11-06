moduleName := "test"

enablePlugins(LibisabellePlugin)

TaskKey[Unit]("checkFilter") := {
  val files = (managedResources in Compile).value
  if (!files.exists(_.getName == "Test.thy"))
    sys.error("couldn't find Test.thy")
  if (files.exists(_.getName == ".ignore"))
    sys.error("found .ignore")
}
