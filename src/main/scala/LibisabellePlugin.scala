package info.hupel.isabelle.sbt

import sbt._
import sbt.Keys._

import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.Executors

import org.apache.commons.io.FilenameUtils

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

import com.vast.sbtlogger.SbtLogger

import info.hupel.isabelle.System
import info.hupel.isabelle.api.{Configuration => _, _}
import info.hupel.isabelle.setup.{Resources, Setup}

object LibisabellePlugin extends AutoPlugin {

  object autoImport {
    lazy val isabelleSource = settingKey[File]("Isabelle source directory")
    lazy val isabelleSourceFilter = settingKey[FileFilter]("Isabelle source files filter")
    lazy val isabellePackage = settingKey[String]("Isabelle package name")
    lazy val isabelleVersions = settingKey[Seq[String]]("Isabelle versions")
    lazy val isabelleSessions = settingKey[Seq[String]]("Isabelle sessions")
    lazy val isabelleSetup = taskKey[Seq[Setup]]("Setup Isabelle")
    lazy val isabelleBuild = taskKey[Unit]("Build Isabelle sessions")
  }

  import autoImport._

  override def requires = plugins.JvmPlugin

  val Isabelle = Tags.Tag("isabelle")

  private def withExecutionContext[T](f: ExecutionContext => T): T = {
    val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
    val result = f(ec)
    ec.shutdownNow()
    result
  }

  def isabelleSetupTask(config: Configuration): Def.Initialize[Task[Seq[Setup]]] =
    (streams, isabelleVersions in config) map { (streams, vs) =>
      SbtLogger.withLogger(streams.log) {
        val versions = vs.map(Version(_))
        val setups = versions.map { v =>
          streams.log.info(s"Creating setup for $v ...")
          Setup.default(v) match {
            case Right(setup) => setup
            case Left(reason) => sys.error(reason.explain)
          }
        }
        streams.log.info("Done.")
        setups
      }
    } tag(Isabelle)

  def isabelleBuildTask(config: Configuration): Def.Initialize[Task[Unit]] =
    (streams, isabelleSetup in config, isabelleSessions in config, fullClasspath in config, taskTemporaryDirectory, name) map {
      (streams, setups, sessions, classpath, tmp, name) =>
        SbtLogger.withLogger(streams.log) {
          val classLoader = new URLClassLoader(classpath.map(_.data.toURI.toURL).toArray)
          val path = (tmp / "sbt-libisabelle" / name / config.name).toPath
          val resources = Resources.dumpIsabelleResources(path, classLoader) match {
            case Right(resources) => resources
            case Left(reason) => sys.error(reason.explain)
          }
          val configurations = sessions.map(resources.makeConfiguration(Nil, _))

          withExecutionContext { implicit ec =>
            val envs = setups.foldLeft(Future.successful(List.empty[Environment])) { case (acc, setup) =>
              acc.flatMap { envs =>
                streams.log.info(s"Creating environment for ${setup.version} ...")
                setup.makeEnvironment.map(_ :: envs)
              }
            }

            for {
              env <- Await.result(envs, Duration.Inf)
              config <- configurations
            } {
              streams.log.info(s"Building session ${config.session} for ${env.version} ...")
              if (!System.build(env, config)) {
                streams.log.error(s"Build of session ${config.session} for ${env.version} failed")
                sys.error("build failed")
              }
            }
          }
        }
      } tag(Isabelle)

  def generatorTask(config: Configuration): Def.Initialize[Task[Seq[File]]] =
    (streams, isabellePackage, isabelleSource in config, isabelleSourceFilter, resourceManaged in config) map { (streams, name, source, filter, rawTarget) =>
      val log = streams.log
      val target = rawTarget / ".libisabelle"
      if (source.exists()) {
        def upToDate(in: File, out: File, testName: Boolean = true): Boolean = {
          (!testName || (in.getName == out.getName)) &&
            filter.accept(in) &&
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
          val files = IO.listFiles(source, filter)
          log.info(s"Copying ${files.length} Isabelle source(s) from $source to $target ...")
          val paths = files.map(f => (f, Path.rebase(source, target / name)(f).get))
          IO.delete(target)
          IO.copy(paths, preserveLastModified = true)
        }
        val files = ((target / name) ** "*").get.filter(_.isFile)
        val mapper = Path.rebase(target, "")
        val contents = files.map(file => FilenameUtils.separatorsToUnix(mapper(file).get)).mkString("\n")
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
    isabelleSessions in config := Nil,
    isabelleSetup in config <<= isabelleSetupTask(config),
    isabelleBuild in config <<= isabelleBuildTask(config)
  )

  def globalIsabelleSettings: Seq[Setting[_]] = Seq(
    isabellePackage := moduleName.value,
    isabelleVersions := Nil,
    logLevel in isabelleSetup := Level.Debug,
    logLevel in isabelleBuild := Level.Debug,
    concurrentRestrictions in Global += Tags.limit(Isabelle, 1),
    isabelleSourceFilter := - ".*"
  )

  override def projectSettings: Seq[Setting[_]] =
    Seq(Compile, Test).flatMap(isabelleSettings) ++
    globalIsabelleSettings

}
