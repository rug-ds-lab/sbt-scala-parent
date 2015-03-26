import sbtrelease.ReleasePlugin.ReleaseKeys._

lazy val sbtScalaParentProject = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    name := "sbt-scala-parent",
    organization := "rugds",
    scalaVersion := "2.10.4",
    sbtPlugin := true,

    // buildinfo plugin
    buildInfoKeys := Seq[BuildInfoKey](name, organization, version, scalaVersion, sbtVersion, BuildInfoKey.action("buildTime") { System.currentTimeMillis }),
    buildInfoPackage <<= name { _.replace("-", "") }
  )


val nexus     = "http://sm4all-project.eu/nexus"
val snapshots = nexus + "/content/repositories/rug.snapshot"
val releases  = nexus + "/content/repositories/rug.release"

publishTo <<= version { (v: String) =>
  if (v.trim.contains("-")) Some("snapshots" at snapshots) else Some("releases" at releases)
}

// disable publishing the main sources jar
publishArtifact in (Compile, packageSrc) := false

// disable publishing the main API jar
publishArtifact in (Compile, packageDoc) := false

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// needed for play sbt plugin
resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/releases"

addSbtPlugin("com.typesafe.play" %  "sbt-plugin" % "2.3.8")


addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.8")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.4.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.0-RC1")


publishArtifact in Test := false

releaseSettings


tagName <<= (version in ThisBuild) map (v => v)