package rugds.sbt

import sbt.Keys._
import sbt._


trait Dependencies {
  // versions
  val junitV          = "4.11"
  val slf4jV          = "1.7.7"
  val jodaTimeV       = "2.4"
  val jodaConvertV    = "1.7"
  val logbackV        = "1.1.2"

  val scalaV          = "2.11.2"
  val specsV          = "2.4"
  val scalaTestV      = "2.2.1"
  val typesafeConfigV = "1.2.1"
  val grizzledLogV    = "1.0.2"
  val scalazV         = "7.1.0"
  val scalaCheckV     = "1.11.5"


  // libraries
  val slf4j       = "org.slf4j" % "slf4j-api"    % slf4jV
  val jodaTime    = "joda-time" % "joda-time"    % jodaTimeV
  val jodaConvert = "org.joda"  % "joda-convert" % jodaConvertV

  val logbackClassic = "ch.qos.logback" % "logback-classic"  % logbackV
  val logbackCore    = "ch.qos.logback" % "logback-core"     % logbackV
  val jclOverSlf4j   = "org.slf4j"      % "jcl-over-slf4j"   % slf4jV
  val log4jOverSlf4j = "org.slf4j"      % "log4j-over-slf4j" % slf4jV

  val junit = "junit" % "junit" % junitV % Test

  val typesafeConfig = "com.typesafe" %  "config"         % typesafeConfigV
  val grizzledLog    = "org.clapper"  %% "grizzled-slf4j" % grizzledLogV
  val scalaz         = "org.scalaz"   %% "scalaz-core"    % scalazV

  val specs      = "org.specs2"     %% "specs2"     % specsV      % Test
  val scalaTest  = "org.scalatest"  %% "scalatest"  % scalaTestV  % Test
  val scalaCheck = "org.scalacheck" %% "scalacheck" % scalaCheckV % Test

  // groups of libraries
  val logViaLog4j         = Seq(logbackClassic, logbackCore, jclOverSlf4j, log4jOverSlf4j)
  val logViaLog4jTestOnly = logViaLog4j map (_ % Test)
  val javaOnly            = Seq(slf4j, jodaTime, jodaConvert, junit)
  val scalaBasic          = javaOnly ++ Seq(typesafeConfig, grizzledLog, scalaz, specs, scalaTest, scalaCheck)
}

trait CommonSettings {
  val nexus     = "http://sm4all-project.eu/nexus"
  val snapshots = nexus + "/content/repositories/rug.snapshot"
  val releases  = nexus + "/content/repositories/rug.release"

  val publishSetting = publishTo <<= version { (v: String) =>
    if (v.trim.contains("-")) Some("snapshots" at snapshots) else Some("releases" at releases)
  }

  val repositories = Seq(
    "RugDS Snapshots" at snapshots,
    "RugDS Releases"  at releases
  )

  val commonSettings = Seq(
    organization := "rugds",
    publishSetting,
    publishArtifact in (Compile, packageSrc) := false, // disable publishing the main sources jar
    publishArtifact in (Compile, packageDoc) := false, // disable publishing the main API jar
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    resolvers ++= repositories
  )

  // check the latest versions of all dependencies (wrapped around sbt-updates plugin)
//  val dependencyUpdates    = TaskKey[Unit]("dependencyUpdates")
//  val dependencyUpdatesDef = dependencyUpdates <<= (dependencyUpdates.asInstanceOf[TaskKey[_]]) map (_) // we just reuse existing plugin functionality
//  val rugdsConfig = config("rugds") // separate namespace for custom commands
}

trait Projects extends Dependencies with CommonSettings {
  private def logDependency(includeLog: Boolean) = (if (includeLog) logViaLog4j else logViaLog4jTestOnly)

  def javaProject(name: String, basedir: String = ".", includeLog: Boolean = false) = {
    val dependencies = javaOnly ++ logDependency(includeLog)
    Project(name, file(basedir), settings = commonSettings ++ Seq(
      libraryDependencies ++= dependencies
    ))
  }

  def scalaProject(name: String, basedir: String = ".", includeLog: Boolean = false) = {
    val dependencies = scalaBasic ++ logDependency(includeLog)
    Project(name, file(basedir), settings = commonSettings ++ Seq(
      scalaVersion := scalaV,
      libraryDependencies ++= dependencies
    ) /* ++ inConfig(rugdsConfig)(Seq(dependencyUpdates)) */)
  }
}

object RugDsSbtPlugin extends AutoPlugin
