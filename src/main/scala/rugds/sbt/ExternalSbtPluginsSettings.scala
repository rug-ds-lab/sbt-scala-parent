package rugds.sbt

import sbt._
import sbt.Keys._

import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._

import aether.Aether._

import sbtbuildinfo.Plugin._

trait ExternalSbtPluginsSettings {

  val sbtReleaseSettings = releaseSettings ++ (
    tagName <<= (version in ThisBuild) map (v => v)
  )

  val sbtBuildInfoSettings = buildInfoSettings ++ Seq (
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      organization,
      version,
      scalaVersion,
      sbtVersion,
      buildInfoBuildNumber,
      BuildInfoKey.action("buildTime") {
        System.currentTimeMillis
      }
    ),
    buildInfoPackage <<= name { _.replace("-", "") }
  )

  val pluginSettings = sbtReleaseSettings ++ aetherSettings ++ sbtBuildInfoSettings
}