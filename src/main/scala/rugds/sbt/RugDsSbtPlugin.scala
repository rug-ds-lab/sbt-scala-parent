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
  val slf4j       = "org.slf4j" % "slf4j-api"    % slf4jV       withSources() withJavadoc()
  val jodaTime    = "joda-time" % "joda-time"    % jodaTimeV    withSources() withJavadoc()
  val jodaConvert = "org.joda"  % "joda-convert" % jodaConvertV withSources() withJavadoc()

  val logbackClassic = "ch.qos.logback" % "logback-classic"  % logbackV withSources() withJavadoc()
  val logbackCore    = "ch.qos.logback" % "logback-core"     % logbackV withSources() withJavadoc()
  val jclOverSlf4j   = "org.slf4j"      % "jcl-over-slf4j"   % slf4jV   withSources() withJavadoc()
  val log4jOverSlf4j = "org.slf4j"      % "log4j-over-slf4j" % slf4jV   withSources() withJavadoc()

  val junit = "junit" % "junit" % junitV % Test withSources() withJavadoc()

  val typesafeConfig = "com.typesafe" %  "config"         % typesafeConfigV withSources() withJavadoc()
  val grizzledLog    = "org.clapper"  %% "grizzled-slf4j" % grizzledLogV    withSources() withJavadoc()
  val scalaz         = "org.scalaz"   %% "scalaz-core"    % scalazV         withSources() withJavadoc()

  val specs      = "org.specs2"     %% "specs2"     % specsV      % Test withSources() withJavadoc()
  val scalaTest  = "org.scalatest"  %% "scalatest"  % scalaTestV  % Test withSources() withJavadoc()
  val scalaCheck = "org.scalacheck" %% "scalacheck" % scalaCheckV % Test withSources() withJavadoc()

  // groups of libraries
  val logViaLog4j         = Seq(logbackClassic, logbackCore, jclOverSlf4j, log4jOverSlf4j)
  val logViaLog4jTestOnly = logViaLog4j map (_ % Test)
  val javaOnly            = Seq(slf4j, jodaTime, jodaConvert, junit)
  val scalaBasic          = javaOnly ++ Seq(typesafeConfig, grizzledLog, scalaz, specs, scalaTest, scalaCheck)
}


trait SprayAkkaDependencies {
  this: Dependencies =>

  // versions
  val sprayV      = "1.3.1"
  val sprayJsonV  = "1.2.6"
  val akkaV       = "2.3.4"

  // libraries
  val sprayClient  = "io.spray"          %% "spray-client"      % sprayV      withSources() withJavadoc()
  val sprayCan     = "io.spray"          %% "spray-can"         % sprayV      withSources() withJavadoc()
  val sprayRouting = "io.spray"          %% "spray-routing"     % sprayV      withSources() withJavadoc()
  val sprayHttpx   = "io.spray"          %% "spray-httpx"       % sprayV      withSources() withJavadoc()
  val sprayIO      = "io.spray"          %% "spray-io"          % sprayV      withSources() withJavadoc()
  val sprayJson    = "io.spray"          %% "spray-json"        % sprayJsonV  withSources() withJavadoc()
  val akkaActor    = "com.typesafe.akka" %% "akka-actor"        % akkaV       withSources() withJavadoc()
  val akkaSlf4j    = "com.typesafe.akka" %% "akka-slf4j"        % akkaV       withSources() withJavadoc()

  // repositories
  val sprayRepo = "spray.io" at "http://repo.spray.io"

  // aggregated dependency
  val akkaDependencies  = scalaBasic       ++ Seq(akkaActor, akkaSlf4j)
  val sprayDependencies = akkaDependencies ++ Seq(sprayCan, sprayClient, sprayRouting, sprayHttpx, sprayIO, sprayJson)
}

trait NotUsedDependencies {
  val amqpClientV = "1.4"
  val cassandraV  = "2.1.0-rc1"

  val amqpClient = "com.github.sstone"      %% "amqp-client"           % amqpClientV withSources() withJavadoc()
  val cassandra  = "com.datastax.cassandra" %  "cassandra-driver-core" % cassandraV  withSources() withJavadoc()
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
    scalaVersion := scalaV,
    publishSetting,
    publishArtifact in (Compile, packageSrc) := false, // disable publishing the main sources jar
    publishArtifact in (Compile, packageDoc) := false, // disable publishing the main API jar
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    resolvers ++= repositories
  )
}

trait Projects extends Dependencies with CommonSettings with SprayAkkaDependencies with NotUsedDependencies with ExternalSbtPluginsSettings {
  private def logDependency(includeLog: Boolean) = (if (includeLog) logViaLog4j else logViaLog4jTestOnly)

  private def genericProject(name: String, basedir: String, dependencies: Seq[ModuleID], includeLog: Boolean) = {
    val finalDependencies = dependencies ++ logDependency(includeLog)
    Project(name, file(basedir), settings = pluginSettings ++ commonSettings ++ Seq(
      libraryDependencies ++= finalDependencies
    ))
  }

  def javaProject (name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, javaOnly,          includeLog)
  def scalaProject(name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, scalaBasic,        includeLog)
  def sprayProject(name: String, basedir: String = ".", includeLog: Boolean = false) = genericProject(name, basedir, sprayDependencies, includeLog) settings {
    resolvers += sprayRepo
  }
}

object RugDsSbtPlugin extends AutoPlugin
