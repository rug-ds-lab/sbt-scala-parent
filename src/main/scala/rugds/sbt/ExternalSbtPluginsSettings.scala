package rugds.sbt

import sbt._
import sbt.Keys._
import sbtbuildinfo.BuildInfoPlugin.autoImport._

import sbtrelease.ReleasePlugin.autoImport.releaseTagName

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._


trait ExternalSbtPluginsSettings {

  val sbtReleaseSettings = releaseTagName := (version in ThisBuild).value map (v => v)


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
    buildInfoPackage := name.value.replace("-", ""),
    buildInfoOptions += BuildInfoOption.ToMap,
    buildInfoOptions += BuildInfoOption.ToJson
  )

  val nativePackagerSettings = Seq (
    maintainer in Docker := "RuG Distributed Systems <rug.ds.dev@gmail.com>",
    dockerBaseImage      := "rugdsdev/java-oracle-ubuntu:16.04-LTS-8u101-jre",

    daemonUser in Docker := "root",

    version in Docker := name.value + "-" + (version in ThisBuild).value,
    packageName in Docker := "private",
    dockerRepository in Docker := Some("rugdsdev")
  )

  val pluginSettings: Seq[Setting[_]] = sbtReleaseSettings ++ sbtBuildInfoSettings ++ nativePackagerSettings
}