package rugds.sbt

import sbt._
import sbt.Keys._

import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._

import aether.Aether._

trait ExternalSbtPluginsSettings {

  val sbtReleaseSettings = releaseSettings ++ (
    tagName <<= (version in ThisBuild) map (v => v)
  )

  val pluginSettings = sbtReleaseSettings ++ aetherSettings
}