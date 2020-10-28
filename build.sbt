lazy val sbtScalaParentProject = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    name := "sbt-scala-parent",
    organization := "rugds",
    scalaVersion := "2.12.10",
    sbtPlugin := true,

    // buildinfo plugin
    buildInfoKeys := Seq[BuildInfoKey](name, organization, version, scalaVersion, sbtVersion, BuildInfoKey.action("buildTime") { System.currentTimeMillis }),
    buildInfoPackage := name.value.replace("-", "")
  )

publishTo := {
  val nexus     = "http://nexus.rugds.org"

  val snapshots = nexus + "/repository/rugds.snapshot.oss"
  val releases  = nexus + "/repository/rugds.release.oss"
  
  if (version.value.trim.contains("-")) Some("snapshots" at snapshots) else Some("releases" at releases)
}


// disable publishing the main sources jar
publishArtifact in (Compile, packageSrc) := true

// disable publishing the main API jar
publishArtifact in (Compile, packageDoc) := true

publishMavenStyle := true

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// needed for play sbt plugin
resolvers ++= Seq (
  "typesafe"            at "https://repo.typesafe.com/typesafe/releases",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)

addSbtPlugin("com.typesafe.play" %  "sbt-plugin" % "2.8.1")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.6.1")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")


publishArtifact in Test := false


releaseTagName := (version in ThisBuild).value
