package sbtprotobuf

import sbt._
import Process._
import Keys._
import java.io.File

object ProtobufPlugin extends Plugin {
  val protobufConfig = config("protobuf")

  val includePaths = TaskKey[Seq[File]]("include-paths", "The paths that contain *.proto dependencies.")
  val protoc = SettingKey[String]("protoc", "The path+name of the protoc executable.")
  val externalIncludePath = SettingKey[File]("external-include-path", "The path to which protobuf:library-dependencies are extracted and which is used as protobuf:include-path for protoc")
  val generatedSource = SettingKey[File]("generated-source")


  type GeneratorExecution = (File, File, Seq[File], Logger) => Int

  val generatorExecution = TaskKey[GeneratorExecution]("generator-execution")

  val generate = TaskKey[Seq[File]]("generate", "Compile the protobuf sources.")
  val unpackDependencies = TaskKey[UnpackedDependencies]("unpack-dependencies", "Unpack dependencies.")

  lazy val protobufSettings: Seq[Setting[_]] = Internals.scopedSettings(protobufConfig) ++ Seq[Setting[_]](
    protoc in protobufConfig := "protoc",
    generatorExecution in protobufConfig <<= (protoc in protobufConfig)map(exec => {
        (srcDir: File, target: File, includePaths: Seq[File], log: Logger) =>
          executeProtoc(exec, srcDir, target, includePaths, log) } ),
    javaSource in protobufConfig <<= (generatedSource in protobufConfig).identity
  )

  object Internals {
    def scopedSettings(config: Configuration) = inConfig(config)(Seq[Setting[_]](
      sourceDirectory <<= (sourceDirectory in Compile) { _ / "protobuf" },
      generatedSource <<= (sourceManaged in Compile) { _ / "compiled_protobuf" },
      externalIncludePath <<= target(_ / "protobuf_external"),

      version := "2.4.1",
      managedClasspath <<= (classpathTypes, update) map { (ct, report) =>
        Classpaths.managedJars(config, ct, report)
      },

      unpackDependencies <<= unpackDependenciesTask(config),

      includePaths <<= (sourceDirectory in config) map (identity(_) :: Nil),
      includePaths <+= unpackDependencies map { _.dir },

      generate <<= sourceGeneratorTask(config)

    )) ++ Seq[Setting[_]](
    sourceGenerators in Compile <+= (generate in config).identity,
    cleanFiles <+= (generatedSource in config).identity,

    libraryDependencies <+= (version in config)("com.google.protobuf" % "protobuf-java" % _),
    ivyConfigurations += config
  )

  }

  case class UnpackedDependencies(dir: File, files: Seq[File])

  private def executeProtoc(protoc: String, srcDir: File, target: File, includePaths: Seq[File], log: Logger) =
    try {
      val schemas = (srcDir ** "*.proto").get
      val incPath = includePaths.map(_.absolutePath).mkString("-I", " -I", "")
      val exitCode = <x>{protoc} {incPath} --java_out={target.absolutePath} {schemas.map(_.absolutePath).mkString(" ")}</x> ! log
      exitCode
    } catch { case e: Exception =>
      throw new RuntimeException("error occured while compiling protobuf files: %s" format(e.getMessage), e)
    }


  private def compile(execution: GeneratorExecution, srcDir: File, target: File, includePaths: Seq[File], log: Logger) = {
    val schemas = (srcDir ** "*.proto").get
    target.mkdirs()
    log.info("Compiling %d protobuf files to %s".format(schemas.size, target))
    schemas.foreach { schema => log.info("Compiling schema %s" format schema) }

    val exitCode = execution(srcDir, target, includePaths, log)

    if (exitCode != 0) {
      error("protoc returned exit code: %s" format exitCode)
    }

    (target ** "*").filter(_.isFile).get.toSet
  }

  private def unpack(deps: Seq[File], extractTarget: File, log: Logger): Seq[File] = {
    IO.createDirectory(extractTarget)
    deps.flatMap { dep =>
      val seq = IO.unzip(dep, extractTarget, "*.proto").toSeq
      if (!seq.isEmpty) log.debug("Extracted " + seq.mkString(","))
      seq
    }
  }

  def sourceGeneratorTaskFactory(config: Configuration): (Keys.TaskStreams, GeneratorExecution, File, File, Seq[File], File) => Seq[File] = {
    (out, executions, srcDir, targetDir, includePaths, cache) =>
      val cachedCompile = FileFunction.cached(cache / ("protobuf_" + config.name), inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) {
        (in: Set[File]) =>
          compile(executions, srcDir, targetDir, includePaths, out.log)
      }
      cachedCompile((srcDir ** "*.proto").get.toSet).toSeq
  }

  private def sourceGeneratorTask(config: Configuration) = (streams, generatorExecution in config, sourceDirectory in config, generatedSource in config, includePaths in config, cacheDirectory) map {
    sourceGeneratorTaskFactory(config)
  }

  private def unpackDependenciesTask(config: Configuration) = (streams, managedClasspath in config, externalIncludePath in config) map {
    (out, deps, extractTarget) =>
      val extractedFiles = unpack(deps.map(_.data), extractTarget, out.log)
      UnpackedDependencies(extractTarget, extractedFiles)
  }

}
