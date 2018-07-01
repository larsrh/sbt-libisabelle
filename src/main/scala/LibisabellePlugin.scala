package info.hupel.isabelle.sbt

import sbt._
import sbt.internal.io.Source
import sbt.Keys._

import java.io.File
import java.net.URLClassLoader
import java.nio.file.{Path => JPath}
import java.util.concurrent.Executors

import org.apache.commons.io.{FileUtils, FilenameUtils}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

import monix.execution.{ExecutionModel, Scheduler, UncaughtExceptionReporter}

import info.hupel.isabelle.{Platform, System}
import info.hupel.isabelle.api.{Configuration => IsabelleConfiguration, _}
import info.hupel.isabelle.setup.{Resources, Setup}

object LibisabellePlugin extends AutoPlugin {

  object autoImport {
    lazy val isabelleSources = settingKey[Seq[File]]("Isabelle source directories")
    lazy val isabelleSourceFilter = settingKey[FileFilter]("Isabelle source files filter")
    lazy val isabellePackage = settingKey[String]("Isabelle package name")
    lazy val isabelleVersions = taskKey[Seq[Version]]("Isabelle versions")
    lazy val isabelleSessions = settingKey[Seq[String]]("Isabelle sessions")
    lazy val isabelleSetup = taskKey[Seq[Setup]]("Setup Isabelle")
    lazy val isabelleBuild = taskKey[Unit]("Build Isabelle sessions")
    lazy val isabelleJEdit = inputKey[Unit]("Launch Isabelle/jEdit")

    type Version = info.hupel.isabelle.api.Version
    val Version = info.hupel.isabelle.api.Version
  }

  import autoImport._

  override def requires = plugins.JvmPlugin

  val Isabelle = Tags.Tag("isabelle")

  private def withScheduler[T](f: Scheduler => T): T = {
    val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
    val se = Executors.newSingleThreadScheduledExecutor()
    val reporter = UncaughtExceptionReporter(t => throw t)
    val scheduler = Scheduler(se, ec, reporter, ExecutionModel.AlwaysAsyncExecution)
    try {
      f(scheduler)
    }
    finally {
      ec.shutdownNow()
      se.shutdownNow()
      ()
    }
  }

  private def doSetup(v: Version, log: Logger) = {
    log.info(s"Creating setup for $v ...")
    Setup.default(v, false) match {
      case Right(setup) => setup
      case Left(reason) => sys.error(reason.explain)
    }
  }

  private def doDump(classpath: Seq[File], path: JPath, log: Logger) = {
    val classLoader = new URLClassLoader(classpath.map(_.toURI.toURL).toArray)
    Resources.dumpIsabelleResources(path, classLoader) match {
      case Right(resources) => resources
      case Left(reason) => sys.error(reason.explain)
    }
  }

  def isabelleSetupTask(config: Configuration): Def.Initialize[Task[Seq[Setup]]] = Def.task {
    val log = streams.value.log
    val setups = (isabelleVersions in config).value.map(v => doSetup(v, log))
    log.info("Done.")
    setups
  } tag(Isabelle)

  def isabelleBuildTask(config: Configuration): Def.Initialize[Task[Unit]] = Def.task {
    val classpath = (fullClasspath in config).value
    val sessions = (isabelleSessions in config).value
    val setups = (isabelleSetup in config).value
    val path = (taskTemporaryDirectory.value / "sbt-libisabelle" / isabellePackage.value / config.name).toPath
    val log = streams.value.log
    val resources = doDump(classpath.map(_.data), path, log)
    val configurations = sessions.map(IsabelleConfiguration.simple)

    withScheduler { implicit sched =>
      val envs = setups.foldLeft(Future.successful(List.empty[Environment])) { case (acc, setup) =>
        acc.flatMap { envs =>
          log.info(s"Creating environment for ${setup.version} ...")
          setup.makeEnvironment(resources, Nil).map(_ :: envs)
        }
      }

      for {
        env <- Await.result(envs, Duration.Inf)
        config <- configurations
      } {
        log.info(s"Building session ${config.session} for ${env.version} ...")
        if (!System.build(env, config)) {
          log.error(s"Build of session ${config.session} for ${env.version} failed")
          sys.error("build failed")
        }
      }
    }
  } tag(Isabelle)

  def generatorTask(config: Configuration): Def.Initialize[Task[Seq[File]]] = Def.task {
    val log = streams.value.log
    val name = isabellePackage.value
    val sources = (isabelleSources in config).value
    val filter = isabelleSourceFilter.value
    val target = (resourceManaged in config).value / ".libisabelle"
    val mapping = sources.filter(_.exists()).flatMap { source =>
      ((PathFinder(source) ** filter) --- source) pair (Path.rebase(source, target / name))
    }.sortBy(_._2)

    val upToDate = {
      val targetFiles = (PathFinder(target / name).allPaths --- (target / name)).get.sorted
      (mapping.map(_._2) == targetFiles) &&
        (mapping.map(_._1) zip targetFiles).forall { case (in, out) =>
          if (in.isFile && out.isFile)
            in.lastModified == out.lastModified
          else
            in.isDirectory && out.isDirectory
        }
    }

    if (!upToDate) {
      log.info(s"Copying ${mapping.length} Isabelle source(s) to $target ...")
      IO.delete(target)
      IO.copy(mapping, overwrite = false, preserveLastModified = true, preserveExecutable = false)
    }

    val files = ((target / name) ** "*").get.filter(_.isFile)
    val mapper = Path.rebase(target, "")
    val contents = files.map(file => FilenameUtils.separatorsToUnix(mapper(file).get)).mkString("\n")
    val list = target / ".files"
    IO.write(list, s"$contents")
    list +: files
  }

  def isabelleJEditTask(config: Configuration): Def.Initialize[InputTask[Unit]] = Def.inputTask {
    val log = streams.value.log
    val (logic, version) =
      Def.spaceDelimited().parsed match {
        case List(logic) =>
          isabelleVersions.value match {
            case v :: _ =>
              log.info(s"Choosing $v")
              (logic, v)
            case _ =>
              sys.error("No Isabelle version specified and none set")
          }
        case List(logic, v) =>
          Version.parse(v) match {
            case Right(v) => (logic, v)
            case Left(err) => sys.error(err)
          }
        case _ =>
          sys.error("Expected one or two arguments: LOGIC [VERSION]")
    }
    val setup = doSetup(version, log)
    log.info("Done.")

    val dump = Platform.guess match {
      case Some(platform) => platform.resourcesStorage(version)
      case None => sys.error("Could not store resources in standard directory")
    }
    FileUtils.deleteDirectory(dump.toFile)

    val resources = doDump((fullClasspath in config).value.map(_.data), dump, log)
    log.info(s"Creating environment for ${setup.version} ...")
    withScheduler { implicit sched =>
      val future = setup.makeEnvironment(resources, Nil).map { env =>
        env.exec("jedit", List("-l", logic))
        ()
      }
      Await.result(future, Duration.Inf)
    }
  } tag(Isabelle)

  def isabelleSettings(config: Configuration): Seq[Setting[_]] = Seq(
    resourceGenerators in config += generatorTask(config),
    isabelleSources in config := List((sourceDirectory in config).value / "isabelle"),
    watchSources ++= {
      val filter = isabelleSourceFilter.value
      (isabelleSources in config).value.map(base => new Source(base, filter, NothingFilter))
    },
    isabelleSessions in config := Nil,
    isabelleSetup in config := isabelleSetupTask(config).value,
    isabelleBuild in config := isabelleBuildTask(config).value,
    isabelleJEdit in config := isabelleJEditTask(config).evaluated
  )

  def globalIsabelleSettings: Seq[Setting[_]] = Seq(
    isabellePackage := moduleName.value,
    isabelleVersions := {
      val log = streams.value.log
      sys.env.get("ISABELLE_VERSION") match {
        case Some(v) =>
          Version.parse(v) match {
            case Right(v) =>
              log.info(s"Using $v")
              Seq(v)
            case Left(err) =>
              log.warn(err)
              Nil
          }
        case _ =>
          log.warn("No ISABELLE_VERSION set")
          Nil
      }
    },
    isabelleSourceFilter := (- ".*") && (- "*~")
  )

  override def projectSettings: Seq[Setting[_]] =
    Seq(Compile, Test).flatMap(isabelleSettings) ++
    globalIsabelleSettings

  override def globalSettings: Seq[Setting[_]] = Seq(
    logLevel in isabelleSetup := Level.Debug,
    logLevel in isabelleBuild := Level.Debug,
    logLevel in isabelleJEdit := Level.Debug,
    concurrentRestrictions += Tags.limit(Isabelle, 1)
  )

}
