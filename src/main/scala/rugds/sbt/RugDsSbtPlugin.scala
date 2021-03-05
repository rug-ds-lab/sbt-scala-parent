package rugds.sbt

import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt.Keys._
import sbt._
import sbtbuildinfo.BuildInfoPlugin
import sbtrelease.ReleasePlugin


trait CommonSettings {
  this: Repositories =>

  val commonSettings = Seq(
    organization  := "rugds",
    javacOptions ++= Seq("-source", s"$javaV"),
    javacOptions in doc := Seq("-source", s"$javaV"),
    scalacOptions ++= Seq(
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
    Project(name, file(basedir))
      .enablePlugins(JavaAppPackaging, BuildInfoPlugin, ReleasePlugin, RugDsSbtPlugin)
      .settings(
        libraryDependencies ++= finalDependencies,
        pluginSettings,
        commonSettings
      )
  }

  def mainProject(name: String = "root") = genericProject(name, ".", Seq(), false) settings (
    publishLocal := {},
    publish      := {}
  )
  def javaProject (name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, javaOnly,         includeLog)
  def scalaProject(name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, scalaBasic,       includeLog)
  def akkaProject (name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, akkaDependencies, includeLog)

  def defineProject(projectType: (String, String, Boolean) => Project, projectName: String, includeLog: Boolean = false) = {
    projectType(projectName, projectName, includeLog)
  }
}

object RugDsSbtPlugin extends AutoPlugin