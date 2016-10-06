package rugds.sbt

import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import play.sbt.PlayScala
import sbt.Keys._
import sbt._
import sbtbuildinfo.BuildInfoPlugin
import sbtrelease.ReleasePlugin


trait CommonSettings {
  this: Repositories =>

  val commonSettings = Seq(
    organization  := "rugds",
    javacOptions ++= Seq("-source", s"$javaV", "-target", s"$javaV"),
    javacOptions in doc := Seq("-source", s"$javaV"),
    scalacOptions ++= Seq(
      s"-target:jvm-$javaV", // enforce java8 in scala
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",    // warning and location for usages of features that should be imported explicitly
      "-unchecked",  // additional warnings where generated code depends on assumptions
      "-Xlint",      // recommended additional warnings
      "-Xcheckinit"  // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
    ),
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
      resolvers ++= Seq(typesafeRepo, scalazRepo),
      libraryDependencies ++= finalDependencies
    )).enablePlugins(JavaAppPackaging, BuildInfoPlugin, ReleasePlugin)
  }

  def mainProject(name: String = "root") = genericProject(name, ".", Seq(), false) settings (
    publishLocal := {},
    publish      := {}
  )
  def javaProject (name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, javaOnly,          includeLog)
  def scalaProject(name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, scalaBasic,        includeLog)
  def akkaProject (name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, akkaDependencies,  includeLog)
  def playProject (name: String, basedir: String = ".", includeLog: Boolean = false) = scalaProject  (name, basedir, includeLog).enablePlugins(PlayScala)


  def defineProject(projectType: (String, String, Boolean) => Project, projectName: String, includeLog: Boolean = false) = {
    projectType(projectName, projectName, includeLog)
  }
}

object RugDsSbtPlugin extends AutoPlugin