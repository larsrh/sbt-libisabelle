package info.hupel.isabelle.sbt

import sbt._

import sbtassembly.AssemblyPlugin
import sbtassembly.AssemblyPlugin.autoImport._

object LibisabelleAssemblyPlugin extends AutoPlugin {

  override def requires = LibisabellePlugin && AssemblyPlugin
  override def trigger = allRequirements

  override def projectSettings: Seq[Setting[_]] = Seq(
    assemblyMergeStrategy in assembly := {
      case PathList(".libisabelle", ".files") => MergeStrategy.concat
      case path => (assemblyMergeStrategy in assembly).value(path)
    }
  )

}
