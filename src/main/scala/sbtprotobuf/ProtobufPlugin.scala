package sbtprotobuf

import sbt._
import Keys._
import sbt.Defaults.packageTaskSettings
import java.io.File
import SbtProtobuf._

object ProtobufTestPlugin extends ScopedProtobufPlugin(Test, "-test")

object ProtobufPlugin extends ScopedProtobufPlugin(Compile) {
  val autoImport = Keys
}

class ScopedProtobufPlugin(configuration: Configuration, private[sbtprotobuf] val configurationPostfix: String = "") extends AutoPlugin with Compat { self =>
  lazy val ProtobufConfig = Configuration.of("ProtobufConfig", "protobuf" + configurationPostfix)
  lazy val ProtobufTool = config("protobuf-tool").hide

  override def requires = sbt.plugins.JvmPlugin

  val protoClassifier = "proto"

  object Keys {
    val ProtobufConfig = self.ProtobufConfig
    @deprecated("will be removed. use ProtobufConfig", "0.6.2")
    val protobufConfig = ProtobufConfig

    val protobufIncludePaths = taskKey[Seq[File]]("The paths that contain *.proto dependencies.")
    val protobufSources = taskKey[Seq[File]]("Protobuf files")
    val protobufIncludeFilters = settingKey[Seq[Glob]]("Include filters")
    val protobufExcludeFilters = settingKey[Seq[Glob]]("Exclude filters")
    val protobufUseSystemProtoc = settingKey[Boolean]("Use the protoc installed on the machine.")
    val protobufProtoc = settingKey[String]("The path+name of the protoc executable if protobufUseSystemProtoc is enabled.")
    val protobufRunProtoc = taskKey[Seq[String] => Int]("A function that executes the protobuf compiler with the given arguments, returning the exit code of the compilation run.")
    val protobufExternalIncludePath = settingKey[File]("The path to which protobuf:libraryDependencies are extracted and which is used as protobuf:includePath for protoc")
    val protobufGeneratedTargets = settingKey[Seq[(File,String)]]("Targets for protoc: target directory and glob for generated source files")
    val protobufGenerate = taskKey[Seq[File]]("Compile the protobuf sources.")
    val protobufUnpackDependencies = taskKey[UnpackedDependencies]("Unpack dependencies.")
    val protobufProtocOptions = taskKey[Seq[String]]("Additional options to be passed to protoc")
    val protobufPackage = taskKey[File]("Produces a proto artifact, such as a jar containing .proto files")
    val protobufGrpcEnabled = settingKey[Boolean]("Enable gRPC plugin")
    val protobufGrpcVersion = settingKey[String]("gRPC version")

    @deprecated("will be removed. use enablePlugins(ProtobufPlugin)", "0.6.0")
    def protobufSettings = self.projectSettings
  }

  import Keys._

  override def projectConfigurations: Seq[Configuration] = ProtobufConfig :: Nil

  // global scoping can be used to provide the default values
  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    protobufUseSystemProtoc := false,
    protobufGeneratedTargets := Nil,
    protobufProtocOptions := Nil,
    protobufIncludeFilters := Nil,
    protobufExcludeFilters := Nil,
    protobufGrpcEnabled := false,
    protobufGrpcVersion := SbtProtobufBuildInfo.defaultGrpcVersion,
  )

  override lazy val projectSettings: Seq[Setting[_]] = inConfig(ProtobufConfig)(Seq[Setting[_]](
    sourceDirectory := { (configuration / sourceDirectory).value / "protobuf" },
    sourceDirectories := (sourceDirectory.value :: Nil),
    protobufSources := {
      val dirs = sourceDirectories.value
      val includes = protobufIncludeFilters.value
      val excludes = protobufExcludeFilters.value
      dirs.flatMap { dir =>
        val allFiles = (dir ** "*").get.map(_.toPath())
        allFiles
          .filter(x => includes.exists(_.matches(x)))
          .filterNot(x => excludes.exists(_.matches(x)))
          .map(_.toFile())
      }
    },
    protobufIncludeFilters ++= {
      val dirs = sourceDirectories.value
      dirs.map(d => Glob(d.toPath()) / "**" / "*.proto")
    },
    protobufExcludeFilters ++= {
      val dirs = sourceDirectories.value
      val excludes = excludeFilter.value
      excludes match {
        case NothingFilter | HiddenFileFilter => Nil
        case f: ExactFilter => dirs.map(d => Glob(d.toPath()) / f.matchName)
        case filter => sys.error(s"unsupported excludeFilter $filter. migrate to ProtobufConfig / protobufExcludeFilters")
      }
    },
    protobufExcludeFilters ++= {
      val dirs = sourceDirectories.value
      dirs.map(d => Glob(d.toPath()) / "google" / "protobuf" / "*.proto")
    },
    javaSource := { (configuration / sourceManaged).value / "compiled_protobuf" },
    protobufExternalIncludePath := (target.value / "protobuf_external"),
    protobufProtoc := "protoc",
    protobufRunProtoc := {
      // keep this if-expression top-level for selective functor
      if (protobufUseSystemProtoc.value) {
        val s = streams.value
        args => Process(protobufProtoc.value, args).!(s.log)
      } else {
        val s = streams.value
        val ur = update.value
        val protoc = withFileCache(s.cacheDirectory.toPath()) { () =>
          extractFile(ur, protocArtifactName)
        }
        args => Process(protoc.toAbsolutePath.toString, args).!(s.log)
      }
    },
    version := SbtProtobufBuildInfo.defaultProtobufVersion,

    protobufGeneratedTargets += Tuple2((ProtobufConfig / javaSource).value, "*.java"), // add javaSource to the list of patterns

    protobufProtocOptions ++= { // if a java target is provided, add java generation option
      (ProtobufConfig / protobufGeneratedTargets).value.find(_._2.endsWith(".java")) match {
        case Some(targetForJava) => Seq("--java_out=%s".format(targetForJava._1.getCanonicalPath))
        case None => Nil
      }
    },
    protobufProtocOptions ++= {
      if (protobufGrpcEnabled.value) {
        val s = streams.value
        val ur = update.value
        val grpcCli = withFileCache(s.cacheDirectory.toPath()) { () =>
          extractFile(ur, protocGenGrpcJavaArtifactName)
        }
        ((ProtobufConfig / protobufGeneratedTargets).value.find(_._2.endsWith(".java")) match {
          case Some(targetForJava) => Seq("--java_rpc_out=%s".format(targetForJava._1.getCanonicalPath))
          case None => Nil
        }) ++ Seq(
          s"--plugin=protoc-gen-java_rpc=${grpcCli}"
        )
      } else Nil
    },
    managedClasspath := {
      Classpaths.managedJars(ProtobufConfig, classpathTypes.value, update.value)
    },

    protobufUnpackDependencies := unpackDependenciesTask.value,

    protobufIncludePaths := ((ProtobufConfig / sourceDirectory).value :: Nil),
    protobufIncludePaths += protobufExternalIncludePath.value,

    protobufGenerate := sourceGeneratorTask.dependsOn(protobufUnpackDependencies).value,
  )) ++ inConfig(ProtobufConfig)(
    packageTaskSettings(protobufPackage, packageProtoMappings)
  ) ++ Seq[Setting[_]](
    watchSourcesSetting,
    configuration / sourceGenerators += (ProtobufConfig / protobufGenerate).taskValue,
    cleanFiles ++= (ProtobufConfig / protobufGeneratedTargets).value.map{_._1},
    cleanFiles += (ProtobufConfig / protobufExternalIncludePath).value,
    configuration / managedSourceDirectories ++= (ProtobufConfig / protobufGeneratedTargets).value.map{_._1},
    libraryDependencies += ("com.google.protobuf" % "protobuf-java" % (ProtobufConfig / version).value),
    libraryDependencies += protocDependency((ProtobufConfig / version).value) % ProtobufTool,
    libraryDependencies ++= {
      if (protobufGrpcEnabled.value) {
        Seq(protocGenGrpcJavaDependency(protobufGrpcVersion.value) % ProtobufTool)
      } else Nil
    },
    ivyConfigurations ++= List(ProtobufConfig, ProtobufTool),
    setProtoArtifact
  )

  case class UnpackedDependencies(dir: File, files: Seq[File])

  private[this] def executeProtoc(protocCommand: Seq[String] => Int, schemas: Set[File], includePaths: Seq[File], protocOptions: Seq[String], log: Logger) : Int =
    try {
      val incPath = includePaths.map("-I" + _.getCanonicalPath)
      protocCommand(incPath ++ protocOptions ++ schemas.map(_.getCanonicalPath))
    } catch { case e: Exception =>
      throw new RuntimeException("error occurred while compiling protobuf files: %s" format(e.getMessage), e)
    }

  private[this] def compile(
      protocCommand: Seq[String] => Int,
      schemas: Set[File],
      includePaths: Seq[File],
      protocOptions: Seq[String],
      generatedTargets: Seq[(File, String)],
      log: Logger,
  ) = {
    val generatedTargetDirs = generatedTargets.map(_._1)
    generatedTargetDirs.foreach{ targetDir =>
      IO.delete(targetDir)
      targetDir.mkdirs()
    }

    if(!schemas.isEmpty){
      log.info("compiling %d protobuf files to %s".format(schemas.size, generatedTargetDirs.mkString(",")))
      log.debug("protoc options:")
      protocOptions.map("\t"+_).foreach(log.debug(_))
      schemas.foreach(schema => log.info("Compiling schema %s" format schema))

      val exitCode = executeProtoc(protocCommand, schemas, includePaths, protocOptions, log)
      if (exitCode != 0)
        sys.error("protoc returned exit code: %d" format exitCode)

      log.info("Compiling protobuf")
      generatedTargetDirs.foreach{ dir =>
        log.info("Protoc target directory: %s".format(dir.absolutePath))
      }

      (generatedTargets.flatMap{ot => (ot._1 ** ot._2).get}).toSet
    } else {
      Set[File]()
    }
  }

  private[this] def unpack(deps: Seq[File], extractTarget: File, log: Logger): Seq[File] = {
    IO.createDirectory(extractTarget)
    deps.flatMap { dep =>
      val seq = IO.unzip(dep, extractTarget, "*.proto").toSeq
      if (!seq.isEmpty) log.debug("Extracted " + seq.mkString("\n * ", "\n * ", ""))
      seq
    }
  }

  private[this] def sourceGeneratorTask =
    Def.task {
      val out     = streams.value
      val schemas = protobufSources.value.toSet[File].map(_.getAbsoluteFile)
      // Include Scala binary version like "_2.11" for cross building.
      val cacheFile = out.cacheDirectory / s"protobuf_${scalaBinaryVersion.value}"
      val runProtoc = protobufRunProtoc.value
      val includePaths = (ProtobufConfig / protobufIncludePaths).value
      val options = (ProtobufConfig / protobufProtocOptions).value
      val targets = (ProtobufConfig / protobufGeneratedTargets).value
      val cachedCompile = FileFunction.cached(cacheFile, inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
        compile(
          protocCommand = runProtoc,
          schemas = schemas,
          includePaths = includePaths,
          protocOptions = options,
          generatedTargets = targets,
          log = out.log)
      }
      cachedCompile(schemas).toSeq
    }

  private[this] def unpackDependenciesTask = Def.task {
    val extractTarget = (ProtobufConfig / protobufExternalIncludePath).value
    val extractedFiles = unpack((ProtobufConfig / managedClasspath).value.map(_.data), extractTarget, streams.value.log)
    UnpackedDependencies(extractTarget, extractedFiles)
  }

  private[this] def packageProtoMappings =
    Def.task {
      protobufSources.value.map {
        case x => (x, x.getName)
      }
    }
}
