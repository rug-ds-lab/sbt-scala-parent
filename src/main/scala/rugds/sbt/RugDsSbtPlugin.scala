package rugds.sbt

import play.PlayScala
import sbt.Keys._
import sbt._


trait CommonSettings {
  this: Repositories =>

  val commonSettings = Seq(
    organization := "rugds",
    scalaVersion := scalaV,
    publishSetting,
    publishArtifact in (Compile, packageSrc) := false, // disable publishing the main sources jar
    publishArtifact in (Compile, packageDoc) := false, // disable publishing the main API jar
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    resolvers ++= repositories
  )
}

trait Projects extends Dependencies with Repositories with CommonSettings with ExternalSbtPluginsSettings {
  private def logDependency(includeLog: Boolean) = if (includeLog) logViaLog4j else logViaLog4jTestOnly

  private def genericProject(name: String, basedir: String, dependencies: Seq[ModuleID], includeLog: Boolean) = {
    val finalDependencies = dependencies ++ logDependency(includeLog)
    Project(name, file(basedir), settings = pluginSettings ++ commonSettings ++ Seq(
      resolvers ++= Seq(typesafeRepo, scalazRepo),
      libraryDependencies ++= finalDependencies
    ))
  }

  def mainProject = genericProject("root", ".", Seq(), false) // for root projects (same settings, no dependencies)
  def javaProject (name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, javaOnly,          includeLog)
  def scalaProject(name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, scalaBasic,        includeLog)
  def sprayProject(name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, sprayDependencies, includeLog) settings {
    resolvers += sprayRepo
  }
  def playProject (name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, Seq.empty[ModuleID], includeLog).enablePlugins(PlayScala)
}

object RugDsSbtPlugin extends AutoPlugin