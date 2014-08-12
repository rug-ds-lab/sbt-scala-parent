package rugds.sbt

import sbt._
import sbt.Keys._

import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._


trait ExternalSbtPluginsSettings {
  val sbtReleaseV = "0.8.4"
  val sbtUpdatesV = "0.1.6"

  val sbtRelease = "com.github.gseitz" % "sbt-release" % sbtReleaseV
  val sbtUpdates = "com.timushev.sbt"  % "sbt-updates" % sbtUpdatesV

  val sbtReleaseSettings = addSbtPlugin(sbtRelease) ++ releaseSettings ++ (
    tagName <<= (version in ThisBuild) map (v => v)
  )

  val sbtUpdatesSettings = addSbtPlugin(sbtUpdates)

  val pluginSettings = sbtReleaseSettings ++ sbtUpdatesSettings
}