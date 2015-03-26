package rugds.sbt

import sbt._
import sbt.Keys._
import sbtbuildinfo.BuildInfoPlugin.autoImport._

import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._


trait ExternalSbtPluginsSettings {

  val sbtReleaseSettings = releaseSettings ++ (
    tagName <<= (version in ThisBuild) map (v => v)
  )

  val sbtBuildInfoSettings = Seq (
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      organization,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("buildTime") {
        System.currentTimeMillis
      }
    ),
    buildInfoPackage <<= name { _.replace("-", "") }
  )

  val nativePackagerSettings = Seq (
    maintainer in Docker := "RuG Distributed Systems <rug.ds.dev@gmail.com>",
    dockerBaseImage      := "dockerfile/java",
    dockerExposedVolumes in Docker := Seq("/opt/docker/logs")
  )

  val pluginSettings = sbtReleaseSettings ++ sbtBuildInfoSettings ++ nativePackagerSettings
}