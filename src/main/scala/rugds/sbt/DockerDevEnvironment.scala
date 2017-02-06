package rugds.sbt

import sbt._
import sbt.Keys._
import java.util.jar._

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConversions._
import net.virtualvoid.sbt.graph.DependencyGraphKeys._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging.autoImport.projectDependencyArtifacts

import scala.util.Try


trait DockerDevEnvironment {
  lazy val envProjectDependencyTree = taskKey[Seq[(EnvModule, Set[EnvModule])]]("tree of all project dependencies starting from root")
  lazy val envDockerDependencyTree  = taskKey[Seq[(EnvDocker, Set[EnvDocker])]]("tree of all docker dependencies starting from root")
  lazy val envPullDockerImages      = taskKey[Set[String]]   ("pulls all docker images from the central repository")

  // single vm (localhost by default?) single instance for each image, links between containers for dependencies (we should probably be more careful for jars, and build a tree of docker files, not just list as of now)
  lazy val envCreateDev             = taskKey[Set[EnvDocker]]("create development environment")
  lazy val envStartDev              = taskKey[Set[EnvDocker]]("start development environment")
  lazy val envStopDev               = taskKey[Set[EnvDocker]]("stop development environment")
  lazy val envRemoveDev             = taskKey[Set[EnvDocker]]("removes development environment")

  lazy val envDockerList            = taskKey[Set[EnvDocker]]("list of project docker images (with Dockerfile defined)")
  lazy val envDockerBuild           = taskKey[Set[EnvDocker]]("builds all project docker images (excluding sub-projects)")

  private val networkId = "rugds-dev"

  val dockerDevSettings = Seq (
    envProjectDependencyTree := {
      val sV  = scalaVersion.value
      val sbV = scalaBinaryVersion.value
      val mg  = (moduleGraph in Compile).value
      val projectJarDependencies = projectDependencyArtifacts.value.map(file =>
        file.metadata.get(moduleID.key)
          .map(m => CrossVersion(sV, sbV)(m))
          .map(m => toEnvModule(m, Some(new JarFile(file.data.getAbsoluteFile))))
          .getOrElse(throw new IllegalStateException(s"moduleId.key is not defined for $file")))
      val projectId = toEnvModule(CrossVersion(sV, sbV)(projectID.value))
      val jarDependencies = mg.dependencyMap.toList.filter {
        case (module, dependencies) => mg.module(module).isUsed // remove evicted modules
      }
      .map {
        case (module, dependencies) =>
          val envModule = EnvModule(module.name, module.organisation, module.version, mg.module(module).jarFile.map(x => new JarFile(x)))
          envModule -> dependencies
            .filter(_.isUsed) // evicted modules should not be used!
            .map(m => EnvModule(m.id.name, m.id.organisation, m.id.version, m.jarFile.map(x => new JarFile(x))))
            .toSet
      }
      .filter {
        // remove dependencies that do not have docker (module)
        case (module, dependencies) => module.jarFile.forall(containsDockerDependency)
      }
      .map {
        // remove dependencies that do not have docker (dependencies)
        case (module, dependencies) => module -> dependencies.filter(_.jarFile.forall(containsDockerDependency))
      }
      .map {
        // replace project dependencies (with no jar) with its jar equivalents
        case (module, dependencies) =>
          val m = projectJarDependencies.find(_ == module).getOrElse(module)
          val d = dependencies.map(dm => projectJarDependencies.find(_ == dm).getOrElse(dm))
          m -> d
      }
      .filter {
        // remove projects without jar
        case (module, dependencies) => module.jarFile.isDefined
      }
      .map {
        // remove projects without jar in dependencies
        case (module, dependencies) => module -> dependencies.filter(_.jarFile.isDefined)
      }
    jarDependencies.map {
      case (module, dependencies) => if (module == projectId) module -> ((jarDependencies.toMap.keySet ++ dependencies) - module) else module -> dependencies
    }.sortWith {
      case ((m1, d1), (m2, d2)) => d2.contains(m1) || !d1.contains(m2)
    }},

    envDockerDependencyTree := envProjectDependencyTree.value.flatMap {
      case (module, dependencies) => module.toEnvDocker.map(_ -> dependencies.flatMap(_.toEnvDocker))
    },

    envPullDockerImages := {
      val log = streams.value.log
      envDockerDependencyTree.value.map {
        case (image, _) => image.toString
      }
      .map { image => {
        log.info(s"docker images -q $image")
        if (s"docker images -q $image".!!.isEmpty) {
          log.info(s"docker pull $image")
          if (s"docker pull $image".! != 0) {
            throw new IllegalStateException(s"Cannot found or pull docker image: $image")
          }
        }
        image
      }}.toSet
    },

    envCreateDev := {
      val log = streams.value.log
      val pdi = envPullDockerImages.value // Forces Docker images to be pulled (downloaded)

      val networkLsCmd = Seq("docker", "network", "ls", "--format", "{{ .Name }}"); log.info(networkLsCmd.mkString(" "))
      if(!networkLsCmd.lines.contains(networkId)) { // if it does not exist => create it
        val networkCreateCmd = Seq("docker", "network", "create", networkId); log.info(networkCreateCmd.mkString(" "))
        networkCreateCmd.!!
      }

      envDockerDependencyTree.value.map {
        case (docker, dependencies) =>
          val dockerPsCmd = Seq("docker", "ps", "-aq", "--format", "{{ .Names}}"); log.info(dockerPsCmd.mkString(" "))
          if (!dockerPsCmd.lines.contains(docker.name)) { // if it does not exist => create it
            val dockerInspectCmd = Seq("docker", "inspect", "-f", "{{range $key, $item := .Config.ExposedPorts}}{{$key}} {{end}}", docker.toString); log.info(dockerInspectCmd.mkString(" "))
            val dockerExposedPorts = dockerInspectCmd.!!
            val exposedPorts = dockerExposedPorts.trim.split(' ').map(port => port.split('/')).map(list => (list.head, list.last)).map {
              case (port, protocol) => s"-p $port:$port/$protocol"
            }.mkString(" ")
            val dockerRunCmd = s"docker create --network=$networkId $exposedPorts ${docker.options} --name ${docker.name} ${docker.toString}"; log.info(dockerRunCmd)
            dockerRunCmd.!!
          }
          docker
      }.toSet
    },

    envStartDev := {
      val log = streams.value.log
      envDockerDependencyTree.value.map {
        case (docker, dependencies) =>
          val dockerRunCmd = s"docker start ${docker.name}"; log.info(dockerRunCmd)
          dockerRunCmd.!!
          docker
      }.toSet
    },

    envStopDev := {
      val log = streams.value.log
      envDockerDependencyTree.value.reverse.map {
        case (docker, dependencies) =>
          val dockerRunCmd = s"docker stop ${docker.name}"; log.info(dockerRunCmd)
          dockerRunCmd.!!
          docker
      }.toSet
    },

    envRemoveDev := {
      val log = streams.value.log

      val result = envDockerDependencyTree.value.reverse.map {
        case (docker, dependencies) =>
          val dockerPsCmd = Seq("docker", "ps", "-aq", "--format", "{{ .Names}}"); log.info(dockerPsCmd.mkString(" "))
          if (dockerPsCmd.lines.contains(docker.name)) { // if it does exist => remove it
            // TODO: Dangling volumes and images?
            val dockerRunCmd = s"docker rm ${docker.name}"; log.info(dockerRunCmd)
            dockerRunCmd.!!
          }
          docker
      }.toSet

      val networkLsCmd = Seq("docker", "network", "ls", "--format", "{{ .Name }}"); log.info(networkLsCmd.mkString(" "))
      if(networkLsCmd.lines.contains(networkId)) { // if it does exist => remove it
        val networkRmCmd = Seq("docker", "network", "rm", networkId); log.info(networkRmCmd.mkString(" "))
        networkRmCmd.!!
      }

      result
    },

    envDockerList := (resources in Compile).value
      .filter(_.isDirectory)
      .filter(_.name == "docker-dependencies")
      .flatMap(_.listFiles)
      .flatMap(repo => {
        repo.listFiles.map(image => {
          val configFile  = new File(image.getAbsolutePath + "/env.conf")
          val envConfig   = ConfigFactory.parseFile(configFile)
          val dockerBuild = if (new File(image.getAbsolutePath + "/Dockerfile").exists()) Some(image) else None
          EnvDocker(repo.name, image.name, envConfig, dockerBuild)
        })
      })
      .filter(_.file.isDefined)
      .toSet,

    envDockerBuild := envDockerList.value
      .map(docker => (docker, docker.file.get)) // we can safely do get here (envDockerList ensure it is defined!)
      .map {
        case (docker, file) =>
          val log = streams.value.log
          val dockerCommand = s"docker build . -t $docker"; log.info(dockerCommand)
          if (Process(s"docker build . -t $docker", cwd = file).! != 0) {
            log.error(s"docker build command failed: $docker")
          } else {
            log.info(s"docker build successful for image: $docker")
          }
          docker
      }
  )

  private def containsDockerDependency(jarFile: JarFile): Boolean = jarFile.entries.toSeq
    .filter(_.getName.startsWith("docker-dependencies"))
    .exists(_.getName.endsWith("env.conf")) // we just check now if it exists (ignoring the actual dependency, to see if it works)

  private def toEnvModule(module: ModuleID, jarFile: Option[JarFile] = None): EnvModule = EnvModule(module.name, module.organization, module.revision, jarFile)
}


case class EnvModule(name: String, organization: String, version: String, jarFile: Option[JarFile] = None) {
  override def toString = s"$organization:$name:$version[${jarFile.map(_.getName)}]"
  override def equals(obj: Any) = obj match {
    case other: EnvModule => name == other.name && organization == other.organization && version == other.version
    case _ => false
  }
  override def hashCode(): Int = name.hashCode + organization.hashCode + version.hashCode

  def toEnvDocker: Seq[EnvDocker] = jarFile.map(x => x.entries.toSeq
    .filter(_.getName.startsWith("docker-dependencies"))
    .filter(_.getName.endsWith("env.conf"))
    .map(file => {
      val image = file.getName.split('/').toSeq.init.tail.mkString("/") // remove head (docker-dependencies) and last (version)
      val envConfig = ConfigFactory.parseString(scala.io.Source.fromInputStream(x.getInputStream(file), "UTF-8").mkString)

      EnvDocker(image.split('/').head, image.split('/').last, envConfig)
    }
    )
  ).getOrElse(List.empty[EnvDocker])
}

case class EnvDocker(repo: String, name: String, envConfig: Config, file: Option[File] = None) { // file is defined if there is Dockerfile, otherwise it represents "pure" dependency
  private val ns = "environment"

  val tag     = envConfig.getString(s"$ns.tag")
  val options = Try[String] { envConfig.getString(s"$ns.options") }.getOrElse("")

  override def toString: String = repo match {
    case "library" => s"$name:$tag"
    case r         => s"$r/$name:$tag"
  }
}
