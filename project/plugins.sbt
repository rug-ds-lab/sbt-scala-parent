addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.13")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.6")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

// needed for play sbt plugin
// sbt itself will need it (as potentially one may have play projects), ignored for other projects
//    for other projects it is needed during sbt update/reload phase; then ignored
resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/releases"
