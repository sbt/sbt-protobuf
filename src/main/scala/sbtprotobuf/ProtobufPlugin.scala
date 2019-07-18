package sbtprotobuf

import sbt._
import Keys._
import sbt.Defaults.{collectFiles, packageTaskSettings}
import java.io.File

object ProtobufTestPlugin extends ScopedProtobufPlugin(Test, "-test")

object ProtobufPlugin extends ScopedProtobufPlugin(Compile) {
  val autoImport = Keys
}

class ScopedProtobufPlugin(configuration: Configuration, private[sbtprotobuf] val configurationPostfix: String = "") extends AutoPlugin with Compat { self =>

  override def requires = sbt.plugins.JvmPlugin

  val protoClassifier = "proto"

  object Keys {
    val ProtobufConfig = self.ProtobufConfig
    @deprecated("will be removed. use ProtobufConfig", "0.6.2")
    val protobufConfig = ProtobufConfig

    val protobufIncludePaths = TaskKey[Seq[File]]("protobuf-include-paths", "The paths that contain *.proto dependencies.")
    val protobufProtoc = SettingKey[String]("protobuf-protoc", "The path+name of the protoc executable.")
    val protobufRunProtoc = TaskKey[Seq[String] => Int]("protobuf-run-protoc", "A function that executes the protobuf compiler with the given arguments, returning the exit code of the compilation run.")
    val protobufExternalIncludePath = SettingKey[File]("protobuf-external-include-path", "The path to which protobuf:libraryDependencies are extracted and which is used as protobuf:includePath for protoc")
    val protobufGeneratedTargets = SettingKey[Seq[(File,String)]]("protobuf-generated-targets", "Targets for protoc: target directory and glob for generated source files")
    val protobufGenerate = TaskKey[Seq[File]]("protobuf-generate", "Compile the protobuf sources.")
    val protobufUnpackDependencies = TaskKey[UnpackedDependencies]("protobuf-unpack-dependencies", "Unpack dependencies.")
    val protobufProtocOptions = SettingKey[Seq[String]]("protobuf-protoc-options", "Additional options to be passed to protoc")
    val protobufPackage = TaskKey[File]("protobufPackage", "Produces a proto artifact, such as a jar containing .proto files")

    @deprecated("will be removed. use enablePlugins(ProtobufPlugin)", "0.6.0")
    def protobufSettings = self.projectSettings
  }

  import Keys._

  override def projectConfigurations: Seq[Configuration] = ProtobufConfig :: Nil

  override lazy val projectSettings: Seq[Setting[_]] = inConfig(ProtobufConfig)(Seq[Setting[_]](
    sourceDirectory := { (sourceDirectory in configuration).value / "protobuf" },
    sourceDirectories := (sourceDirectory.value :: Nil),
    includeFilter := "*.proto",
    javaSource := { (sourceManaged in configuration).value / "compiled_protobuf" },
    protobufExternalIncludePath := (target.value / "protobuf_external"),
    protobufProtoc := "protoc",
    protobufRunProtoc := {
      val s = streams.value
      val protoc = protobufProtoc.value
      args => Process(protoc, args) ! s.log
    },
    version := "3.9.0",

    protobufGeneratedTargets := Nil,
    protobufGeneratedTargets += Tuple2((javaSource in ProtobufConfig).value, "*.java"), // add javaSource to the list of patterns

    protobufProtocOptions := Nil,
    protobufProtocOptions ++= { // if a java target is provided, add java generation option
      (protobufGeneratedTargets in ProtobufConfig).value.find(_._2.endsWith(".java")) match {
        case Some(targetForJava) => Seq("--java_out=%s".format(targetForJava._1.getCanonicalPath))
        case None => Nil
      }
    },

    managedClasspath := {
      Classpaths.managedJars(ProtobufConfig, classpathTypes.value, update.value)
    },

    protobufUnpackDependencies := unpackDependenciesTask.value,

    protobufIncludePaths := ((sourceDirectory in ProtobufConfig).value :: Nil),
    protobufIncludePaths += protobufExternalIncludePath.value,

    protobufGenerate := sourceGeneratorTask.dependsOn(protobufUnpackDependencies).value

  )) ++ inConfig(ProtobufConfig)(
    packageTaskSettings(protobufPackage, packageProtoMappings)
  ) ++ Seq[Setting[_]](
    watchSourcesSetting,
    sourceGenerators in configuration += (protobufGenerate in ProtobufConfig).taskValue,
    cleanFiles ++= (protobufGeneratedTargets in ProtobufConfig).value.map{_._1},
    cleanFiles += (protobufExternalIncludePath in ProtobufConfig).value,
    managedSourceDirectories in configuration ++= (protobufGeneratedTargets in ProtobufConfig).value.map{_._1},
    libraryDependencies += ("com.google.protobuf" % "protobuf-java" % (version in ProtobufConfig).value),
    ivyConfigurations += ProtobufConfig,
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

  private[this] def compile(protocCommand: Seq[String] => Int, schemas: Set[File], includePaths: Seq[File], protocOptions: Seq[String], generatedTargets: Seq[(File, String)], log: Logger) = {
    val generatedTargetDirs = generatedTargets.map(_._1)
    generatedTargetDirs.foreach{ targetDir =>
      IO.delete(targetDir)
      targetDir.mkdirs()
    }

    if(!schemas.isEmpty){
      log.info("Compiling %d protobuf files to %s".format(schemas.size, generatedTargetDirs.mkString(",")))
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
      val schemas = collectFiles(sourceDirectories in ProtobufConfig, includeFilter in ProtobufConfig, excludeFilter in ProtobufConfig)
        .value.toSet[File].map(_.getAbsoluteFile)
      // Include Scala binary version like "_2.11" for cross building.
      val cacheFile = out.cacheDirectory / s"protobuf_${scalaBinaryVersion.value}"
      val runProtoc = protobufRunProtoc.value
      val includePaths = (protobufIncludePaths in ProtobufConfig).value
      val options = (protobufProtocOptions in ProtobufConfig).value
      val targets = (protobufGeneratedTargets in ProtobufConfig).value
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
    val extractTarget = (protobufExternalIncludePath in ProtobufConfig).value
    val extractedFiles = unpack((managedClasspath in ProtobufConfig).value.map(_.data), extractTarget, streams.value.log)
    UnpackedDependencies(extractTarget, extractedFiles)
  }

  private[this] def packageProtoMappings = Def.task {
    collectFiles(sourceDirectories in ProtobufConfig, includeFilter in ProtobufConfig, excludeFilter in ProtobufConfig)
      .value.map(f => (f, f.getName))
  }

}
