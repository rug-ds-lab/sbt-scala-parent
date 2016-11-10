package rugds.sbt

import sbt._
import sbt.Keys._
import java.util.jar._
import scala.collection.JavaConversions._
import net.virtualvoid.sbt.graph.DependencyGraphKeys._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging.autoImport.projectDependencyArtifacts


trait DockerDevEnvironment {
  lazy val envProjectDependencyTree = taskKey[Seq[(EnvModule, Set[EnvModule])]]("tree of all project dependencies starting from root")
  lazy val envDockerDependencyTree  = taskKey[Seq[(EnvDocker, Set[EnvDocker])]]("tree of all docker dependencies starting from root")
  lazy val envPullDockerImages      = taskKey[Set[String]]   ("pulls all docker images from the central repository")
  lazy val envStartDev              = taskKey[Set[EnvDocker]]("start development environment") // single vm (localhost by default?) single instance for each image, links between containers for dependencies (we should probably be more careful for jars, and build a tree of docker files, not just list as of now)

  lazy val envDockerList            = taskKey[Set[EnvDocker]]("list of project docker images (with Dockerfile defined)")
  lazy val envDockerBuild           = taskKey[Set[EnvDocker]]("builds all project docker images (excluding sub-projects)")


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

    envStartDev := {
      val log = streams.value.log
      val pdi = envPullDockerImages.value
//      val networkCmd = s"docker -P" // TODO: auto-create network if not yet created (or fail if exists?)
      val networkId = "rugds-dev"
      envDockerDependencyTree.value.map {
        case (docker, dependencies) =>
          val dockerPsCmd = Seq("docker", "ps", "-aq", "--format", "{{ .Names}}"); log.info(dockerPsCmd.mkString(" "))
          if (!dockerPsCmd.lines.contains(docker.name)) { // if it does not exist => run it
            val dockerInspectCmd = Seq("docker", "inspect", "-f", "{{range $key, $item := .Config.ExposedPorts}}{{$key}} {{end}}", docker.toString); log.info(dockerInspectCmd.mkString(" "))
            val dockerExposedPorts = dockerInspectCmd.!!
            val exposedPorts = dockerExposedPorts.trim.split(' ').map(port => port.split('/')).map(list => (list.head, list.last)).map {
              case (port, _) => s"-p $port:$port"
            }.mkString(" ")
            val dockerRunCmd = s"docker run -d --network=$networkId $exposedPorts --name ${docker.name} ${docker.toString}"; log.info(dockerRunCmd)
            dockerRunCmd.!!
          }
          docker
      }.toSet
    },

    envDockerList := (resources in Compile).value
      .filter(_.isDirectory)
      .filter(_.name == "docker-dependencies")
      .flatMap(_.listFiles)
      .flatMap(repo => {
        repo.listFiles.map(image => {
          val imageVersion = scala.io.Source.fromFile(image.getAbsolutePath + "/version").mkString
          val dockerBuild  = if (new File(image.getAbsolutePath + "/Dockerfile").exists()) Some(image) else None
          EnvDocker(repo.name, image.name, imageVersion, dockerBuild)
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
    .exists(_.getName.endsWith("version")) // we just check now if it exists (ignoring the actual dependency, to see if it works)

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
    .filter(_.getName.endsWith("version"))
    .map(file => {
      val image = file.getName.split('/').toSeq.init.tail.mkString("/") // remove head (docker-dependencies) and last (version)
      val dockerVersion = scala.io.Source.fromInputStream(x.getInputStream(file), "UTF-8").mkString.trim
      EnvDocker(image.split('/').head, image.split('/').last, dockerVersion)
    }
    )
  ).getOrElse(List.empty[EnvDocker])
}

case class EnvDocker(repo: String, name: String, tag: String, file: Option[File] = None) { // file is defined if there is Dockerfile, otherwise it represents "pure" dependency
  override def toString: String = s"$repo/$name:$tag"
}
