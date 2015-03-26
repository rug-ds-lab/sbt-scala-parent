package rugds.sbt

import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import play.PlayScala
import sbt.Keys._
import sbt._
import sbtbuildinfo.BuildInfoPlugin


trait CommonSettings {
  this: Repositories =>

  val commonSettings = Seq(
    organization  := "rugds",
    javacOptions ++= Seq("-source", s"$javaV", "-target", s"$javaV"),
    javacOptions in doc := Seq("-source", s"$javaV"),
    scalacOptions += s"-target:jvm-$javaV",  // enforce java7 in scala
    scalaVersion  := scalaV,
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
      resolvers ++= Seq(typesafeRepo),
      libraryDependencies ++= finalDependencies
    )).enablePlugins(JavaAppPackaging, BuildInfoPlugin)
  }

  def mainProject = genericProject("root", ".", Seq(), false) settings (
    publishLocal := {},
    publish      := {}
  )
  def javaProject (name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, javaOnly,          includeLog)
  def scalaProject(name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, scalaBasic,        includeLog)
  def akkaProject (name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, akkaDependencies,  includeLog)
  def sprayProject(name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, sprayDependencies, includeLog) settings {
    resolvers += sprayRepo
  }
  def playProject (name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, Seq.empty[ModuleID], includeLog).enablePlugins(PlayScala)


  def defineProject(projectType: (String, String, Boolean) => Project, projectName: String, includeLog: Boolean = false) = {
    projectType(projectName, projectName, includeLog)
  }
}

object RugDsSbtPlugin extends AutoPlugin