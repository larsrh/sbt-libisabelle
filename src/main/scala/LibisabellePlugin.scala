package edu.tum.cs.isabelle.sbt

import sbt._
import sbt.Keys._

import java.io.File

object LibisabellePlugin extends AutoPlugin {

  object autoImport {
    lazy val isabelleSource = settingKey[File]("Isabelle source directory")
  }

  import autoImport._

  override def trigger = allRequirements
  override def requires = plugins.JvmPlugin

  def generatorTask(config: Configuration): Def.Initialize[Task[Seq[File]]] =
    (streams, moduleName, isabelleSource in config, resourceManaged in config) map { (streams, name, source, rawTarget) =>
      val log = streams.log
      val target = rawTarget / "isabelle"
      if (source.exists()) {
        var reasonBecauseNotUpToDate = ""
        implicit class B(b: Boolean) { @inline def -/>(msg: String): Boolean = { if(!b) reasonBecauseNotUpToDate = msg; b } }
        def upToDate(in: File, out: File, testName: Boolean = true): Boolean = {
          (!testName || (in.getName == out.getName) -/> s"$in != $out") &&
          in.exists() -/> s"$in does not exist" &&
          out.exists() -/>  s"$out does not exist" && (
          if (in.isDirectory && out.isDirectory) {
            val inFiles = in.listFiles()
            val outFiles = out.listFiles()
            (inFiles.size == outFiles.size)-/> s"$in has ${inFiles.size} files whereas $out has ${outFiles.size} files" &&
            inFiles.zip(outFiles).forall(in => upToDate(in._1, in._2))
          } else if(in.isFile && out.isFile) {
            (in.lastModified == out.lastModified) -/> s"$out is more recent than $in"
          } else false -/> s"$in and $out are not both directories or files")
        }
        if (!upToDate(source, target / name, testName = false)) {
          log.info(s"Copying Isabelle sources from $source to $target because $reasonBecauseNotUpToDate")
          IO.delete(target)
          IO.copyDirectory(source, target / name, preserveLastModified=true)
        }
        val files = ((target / name) ** "*").get.filter(_.isFile)
        val mapper = Path.rebase(target / name, "")
        val contents = files.map(mapper).map(_.get).mkString("\n")
        val list = target / ".libisabelle_files"
        IO.write(list, s"$name\n$contents")
        list +: files
      }
      else {
        Nil
      }
    }

  def isabelleSettings(config: Configuration): Seq[Setting[_]] = Seq(
    resourceGenerators in config <+= generatorTask(config),
    isabelleSource in config := (sourceDirectory in config).value / "isabelle",
    watchSources <++= (isabelleSource in config) map { src =>
      (src ** "*").get
    }
  )

  override def projectSettings: Seq[Setting[_]] =
    Seq(Compile, Test).flatMap(isabelleSettings)

}
