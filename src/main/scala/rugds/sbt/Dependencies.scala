package rugds.sbt

import sbt._
import sbt.Keys._

trait Dependencies {
  // versions
  val junitV          = "4.13.2"
  val slf4jV          = "1.7.30"
  val jodaTimeV       = "2.10.10"
  val jodaConvertV    = "2.2.1"
  val logbackV        = "1.2.3"

  val akkaV           = "2.6.3"

  val javaV           = "15"
  val scalaV          = "2.12.13"
  val specsV          = "4.10.6"
  val scalaTestV      = "3.2.5"
  val typesafeConfigV = "1.4.1"
  val grizzledLogV    = "1.3.4"
  val scalaCheckV     = "1.15.3"

  // libraries
  val slf4j       = "org.slf4j" % "slf4j-api"    % slf4jV       
  val jodaTime    = "joda-time" % "joda-time"    % jodaTimeV    
  val jodaConvert = "org.joda"  % "joda-convert" % jodaConvertV 

  val akkaActor    = "com.typesafe.akka" %% "akka-actor"        % akkaV       
  val akkaSlf4j    = "com.typesafe.akka" %% "akka-slf4j"        % akkaV       

  val logbackClassic = "ch.qos.logback" % "logback-classic"  % logbackV 
  val logbackCore    = "ch.qos.logback" % "logback-core"     % logbackV 
  val jclOverSlf4j   = "org.slf4j"      % "jcl-over-slf4j"   % slf4jV   
  val log4jOverSlf4j = "org.slf4j"      % "log4j-over-slf4j" % slf4jV   

  val typesafeConfig = "com.typesafe" %  "config"         % typesafeConfigV 
  val grizzledLog    = "org.clapper"  %% "grizzled-slf4j" % grizzledLogV    

  // test libraries
  val junit      = "junit"          %  "junit"        % junitV      % Test 
  val specs      = "org.specs2"     %% "specs2-core"  % specsV      % Test 
  val specsJUnit = "org.specs2"     %% "specs2-junit" % specsV      % Test 
  val scalaTest  = "org.scalatest"  %% "scalatest"    % scalaTestV  % Test 
  val scalaCheck = "org.scalacheck" %% "scalacheck"   % scalaCheckV % Test 

  // aggregated dependencies
  val logViaLog4j         = Seq(logbackClassic, logbackCore, jclOverSlf4j, log4jOverSlf4j)
  val logViaLog4jTestOnly = logViaLog4j map (_ % Test)
  val javaOnly            = Seq(slf4j, jodaTime, jodaConvert, junit)
  val scalaBasic          = javaOnly         ++ Seq(typesafeConfig, grizzledLog, specs, specsJUnit, scalaTest, scalaCheck)
  val akkaDependencies    = scalaBasic       ++ Seq(akkaActor, akkaSlf4j)
}

trait Repositories {

  val nexus     = "http://nexus.rugds.org"
  val snapshots = nexus + "/repository/rugds.snapshot.private"
  val releases  = nexus + "/repository/rugds.release.private"

  val publishSetting = publishTo := {
    val repo = if (version.value.trim.contains("-")) Some("snapshots" at snapshots) else Some("releases" at releases)
    repo.map(_.withAllowInsecureProtocol(true))
  }

  val repositories = Seq(
    "RugDS Snapshots" at snapshots,
    "RugDS Releases"  at releases
  ).map(_.withAllowInsecureProtocol(true))
}
