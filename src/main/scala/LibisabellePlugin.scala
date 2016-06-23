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
        log.info(s"Copying Isabelle sources from $source to $target")
        IO.delete(target)
        IO.copyDirectory(source, target / name)
        val files = (target ** "*").get.filter(_.isFile)
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
