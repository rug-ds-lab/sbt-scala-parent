package rugds.sbt

import sbt._
import sbt.Keys._

trait Dependencies {
  // versions
  val junitV          = "4.12"
  val slf4jV          = "1.7.10"
  val jodaTimeV       = "2.7"
  val jodaConvertV    = "1.7"
  val logbackV        = "1.1.3"

  val akkaV           = "2.3.9"
  val sprayV          = "1.3.2"
  val sprayJsonV      = "1.3.1"
  val playV           = "2.3.8"
  val sshV            = "0.7.0"

  val javaV           = "1.7"
  val scalaV          = "2.11.6"
  val specsV          = "2.4.16"
  val scalaTestV      = "2.2.4"
  val typesafeConfigV = "1.2.1"
  val grizzledLogV    = "1.0.2"
  val scalazV         = "7.1.1"
  val scalaCheckV     = "1.12.2"

  // libraries
  val slf4j       = "org.slf4j" % "slf4j-api"    % slf4jV       withSources() withJavadoc()
  val jodaTime    = "joda-time" % "joda-time"    % jodaTimeV    withSources() withJavadoc()
  val jodaConvert = "org.joda"  % "joda-convert" % jodaConvertV withSources() withJavadoc()

  val ssh        = "com.decodified"      %% "scala-ssh"         % sshV        withSources() withJavadoc()

  val akkaActor    = "com.typesafe.akka" %% "akka-actor"        % akkaV       withSources() withJavadoc()
  val akkaSlf4j    = "com.typesafe.akka" %% "akka-slf4j"        % akkaV       withSources() withJavadoc()
  val sprayClient  = "io.spray"          %% "spray-client"      % sprayV      withSources() withJavadoc()
  val sprayCan     = "io.spray"          %% "spray-can"         % sprayV      withSources() withJavadoc()
  val sprayRouting = "io.spray"          %% "spray-routing"     % sprayV      withSources() withJavadoc()
  val sprayHttpx   = "io.spray"          %% "spray-httpx"       % sprayV      withSources() withJavadoc()
  val sprayIO      = "io.spray"          %% "spray-io"          % sprayV      withSources() withJavadoc()
  val sprayJson    = "io.spray"          %% "spray-json"        % sprayJsonV  withSources() withJavadoc()

  val logbackClassic = "ch.qos.logback" % "logback-classic"  % logbackV withSources() withJavadoc()
  val logbackCore    = "ch.qos.logback" % "logback-core"     % logbackV withSources() withJavadoc()
  val jclOverSlf4j   = "org.slf4j"      % "jcl-over-slf4j"   % slf4jV   withSources() withJavadoc()
  val log4jOverSlf4j = "org.slf4j"      % "log4j-over-slf4j" % slf4jV   withSources() withJavadoc()

  val typesafeConfig = "com.typesafe" %  "config"         % typesafeConfigV withSources() withJavadoc()
  val grizzledLog    = "org.clapper"  %% "grizzled-slf4j" % grizzledLogV    withSources() withJavadoc()

  // play sbt plugin
  val playPlugin = "com.typesafe.play" %  "sbt-plugin"  % playV  withSources() withJavadoc()

  // test libraries
  val junit      = "junit"          %  "junit"      % junitV      % Test withSources() withJavadoc()
  val specs      = "org.specs2"     %% "specs2"     % specsV      % Test withSources() withJavadoc()
  val scalaTest  = "org.scalatest"  %% "scalatest"  % scalaTestV  % Test withSources() withJavadoc()
  val scalaCheck = "org.scalacheck" %% "scalacheck" % scalaCheckV % Test withSources() withJavadoc()

  // aggregated dependencies
  val logViaLog4j         = Seq(logbackClassic, logbackCore, jclOverSlf4j, log4jOverSlf4j)
  val logViaLog4jTestOnly = logViaLog4j map (_ % Test)
  val javaOnly            = Seq(slf4j, jodaTime, jodaConvert, junit)
  val scalaBasic          = javaOnly         ++ Seq(typesafeConfig, grizzledLog, specs, scalaTest, scalaCheck)
  val akkaDependencies    = scalaBasic       ++ Seq(akkaActor, akkaSlf4j)
  val sprayDependencies   = akkaDependencies ++ Seq(sprayCan, sprayClient, sprayRouting, sprayHttpx, sprayIO, sprayJson)
}

trait Repositories {
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

  // external repositories
  val sprayRepo    = "spray.io"            at "http://repo.spray.io"
  val typesafeRepo = "typesafe"            at "http://repo.typesafe.com/typesafe/releases"
}