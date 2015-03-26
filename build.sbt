import sbtrelease.ReleasePlugin.ReleaseKeys._

name := "sbt-scala-parent"

organization := "rugds"

scalaVersion := "2.10.4"

sbtPlugin := true


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


addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.13")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.8")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.4.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.0-RC1")


publishArtifact in Test := false

releaseSettings

aetherSettings

buildInfoKeys := Seq[BuildInfoKey](name, organization, version, scalaVersion, sbtVersion, BuildInfoKey.action("buildTime") { System.currentTimeMillis })

buildInfoPackage <<= name { _.replace("-", "") }


tagName <<= (version in ThisBuild) map (v => v)

// to be removed later (there are some incomplete work below that will be (?) reused)

//lazy val versionReport = TaskKey[String]("version-report")
//lazy val versionLatest = TaskKey[String]("versionLatest")
//
//lazy val showConfigurations = taskKey[Unit]("Shows all configurations")
//lazy val inAnyProjectAndConfiguration = ScopeFilter(inAnyProject, inAnyConfiguration)
//
//val latestDependency = config("latestDependency") // essentially, we have everything already, we just need to modify the dependencies, and then update!
//
//inConfig(latestDependency)(Seq())
//
//libraryDependencies in latestDependency := libraryDependencies.value.map(x => ModuleID(x.organization, x.name + "NNOOO", x.revision, x.configurations, x.isChanging, x.isTransitive, x.isForce, x.explicitArtifacts, x.exclusions, x.extraAttributes, x.crossVersion))
//
//val updateX = TaskKey[UpdateReport]("updateX") in latestDependency
//
//inConfig(latestDependency)(Seq(updateX <<= update))
//
//lazy val myCommand = TaskKey[Unit]("myCommand")
//
//// myCommand in latestDependency <<= (libraryDependencies in latestDependency, updateX, streams) map { (ld, u, l) => {
////  l.log.info(">>> " + ld.mkString(" // "))
////  l.log.info(u.allModules.filter(m => m.name.contains("scala")).mkString("\n"))
////  ""
////}}
//
//myCommand in latestDependency <<= (dependencyUpdates.asInstanceOf[TaskKey[_]], streams) map ((du, streams) => {streams.log.info("WOKRING!")})
//
////myCommand in latestDependency <<= (libraryDependencies in latestDependency, ivyModule, streams) map { (ld, ivy, l) => {
////  ivy.owner.withIvy(l.log)(r => {
////    val md = DefaultModuleDescriptor.newDefaultInstance(
////      // give it some related name (so it can be cached)
////      ModuleRevisionId.newInstance(
////        "junit",
////        "junit",
////        "1.11"
////      )
////    );
////    val ro = new ResolveOptions();
////    // this seems to have no impact, if you resolve by module descriptor (in contrast to resolve by ModuleRevisionId)
////    ro.setTransitive(true);
////    // if set to false, nothing will be downloaded
////    ro.setDownload(true);
////    val report = r.resolve(md, ro)
////    l.log.info(report.getModuleDescriptor.getDescription)
//////    val mri: ModuleRevisionId = null
//////    val ro : RetrieveOptions  = null
//////    r.retrieve(mri, ro)
////    l.log.info("   CANNOT BELIEVE IT!")
////  })
////  ""
////}}
//
//
//showConfigurations := {
//  val configs = configuration.all(inAnyProjectAndConfiguration).value.toSet
//  configs.filter(_.isPublic).foreach(c => println(s"${c.name} ${c.description}"))
//}
//
//
//versionLatest <<= (libraryDependencies, streams) map {
//  (dependencies, streams) => {
//    val report = (dependencies map (d => s"${d.organization}:${d.name}:${d.revision}")).mkString("\n")
//    streams.log.info(report)
//    report
//  }
//}
//
//// Add this setting to your project.
//versionReport <<= (externalDependencyClasspath in Compile, streams) map {
//  (cp: Seq[Attributed[File]], streams) =>
//    val report = cp.map {
//      attributed =>
//        attributed.get(Keys.moduleID.key) match {
//          case Some(moduleId) if moduleId.organization == "org.scala-lang" => // do nothing
//          case Some(moduleId) if moduleId.organization == "org.scala-sbt"  => // do nothing
//          case Some(moduleId) => "%40s %20s %10s %10s".format(
//            moduleId.organization,
//            moduleId.name,
//            moduleId.revision,
//            moduleId.configurations.getOrElse("")
//          )
//          case None           =>
//            // unmanaged JAR, just
//            attributed.data.getAbsolutePath
//        }
//    }.mkString("\n")
//    streams.log.info(report)
//    report
//}
