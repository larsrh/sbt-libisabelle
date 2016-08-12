package edu.tum.cs.isabelle.sbt

import sbt._
import sbt.Keys._

import java.io.File

object LibisabellePlugin extends AutoPlugin {

  object autoImport {
    lazy val isabelleSource = settingKey[File]("Isabelle source directory")
    lazy val isabellePackage = settingKey[String]("Isabelle package name")
  }

  import autoImport._

  override def trigger = allRequirements
  override def requires = plugins.JvmPlugin

  def generatorTask(config: Configuration): Def.Initialize[Task[Seq[File]]] =
    (streams, isabellePackage, isabelleSource in config, resourceManaged in config) map { (streams, name, source, rawTarget) =>
      val log = streams.log
      val target = rawTarget / ".libisabelle"
      if (source.exists()) {
        def upToDate(in: File, out: File, testName: Boolean = true): Boolean = {
          (!testName || (in.getName == out.getName)) &&
            in.exists() &&
            out.exists() && {
              if (in.isDirectory && out.isDirectory) {
                val inFiles = in.listFiles()
                val outFiles = out.listFiles()
                (inFiles.size == outFiles.size) &&
                  inFiles.zip(outFiles).forall(in => upToDate(in._1, in._2))
              }
              else if (in.isFile && out.isFile)
                in.lastModified == out.lastModified
              else
                false
            }
        }
        if (!upToDate(source, target / name, testName = false)) {
          log.info(s"Copying Isabelle sources from $source to $target")
          IO.delete(target)
          IO.copyDirectory(source, target / name, preserveLastModified = true)
        }
        val files = ((target / name) ** "*").get.filter(_.isFile)
        val mapper = Path.rebase(target, "")
        val contents = files.map(mapper).map(_.get).mkString("\n")
        val list = target / ".files"
        IO.write(list, s"$contents")
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
    },
    isabellePackage := moduleName.value
  )

  override def projectSettings: Seq[Setting[_]] =
    Seq(Compile, Test).flatMap(isabelleSettings)

}
