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

  val generate = TaskKey[Seq[File]]("generate", "Compile the protobuf sources.")
  val unpackDependencies = TaskKey[UnpackedDependencies]("unpack-dependencies", "Unpack dependencies.")

  lazy val protobufSettings: Seq[Setting[_]] = inConfig(protobufConfig)(Seq[Setting[_]](
    sourceDirectory <<= (sourceDirectory in Compile) { _ / "protobuf" },
    javaSource <<= (sourceManaged in Compile) { _ / "compiled_protobuf" },
    externalIncludePath <<= target(_ / "protobuf_external"),
    protoc := "protoc",
    version := "2.4.1",

    managedClasspath <<= (classpathTypes, update) map { (ct, report) =>
      Classpaths.managedJars(protobufConfig, ct, report)
    },

    unpackDependencies <<= unpackDependenciesTask,

    includePaths <<= (sourceDirectory in protobufConfig) map (identity(_) :: Nil),
    includePaths <+= unpackDependencies map { _.dir },

    generate <<= sourceGeneratorTask

  )) ++ Seq[Setting[_]](
    sourceGenerators in Compile <+= (generate in protobufConfig),
    managedSourceDirectories in Compile <+= (javaSource in protobufConfig),
    cleanFiles <+= (javaSource in protobufConfig),
    libraryDependencies <+= (version in protobufConfig)("com.google.protobuf" % "protobuf-java" % _),
    ivyConfigurations += protobufConfig
  )

  case class UnpackedDependencies(dir: File, files: Seq[File])

  private def executeProtoc(protocCommand: String, schemas: Seq[File], target: File, includePaths: Seq[File], log: Logger) =
    try {
      val incPath = includePaths.map(_.absolutePath).mkString("-I", " -I", "")
      <x>{protocCommand} {incPath} --java_out={target.absolutePath} {schemas.map(_.absolutePath).mkString(" ")}</x> ! log
    } catch { case e: Exception =>
      throw new RuntimeException("error occured while compiling protobuf files: %s" format(e.getMessage), e)
    }


  private def compile(protocCommand: String, srcDirs: Seq[File], target: File, includePaths: Seq[File], log: Logger) = {
    val schemas = srcDirs.flatMap { dir => (dir ** "*.proto").get }
    target.mkdirs()
    log.info("Compiling %d protobuf files to %s".format(schemas.size, target))
    schemas.foreach { schema => log.info("Compiling schema %s" format schema) }

    val exitCode = executeProtoc(protocCommand, schemas, target, includePaths, log)
    if (exitCode != 0)
      sys.error("protoc returned exit code: %d" format exitCode)

    (target ** "*.java").get.toSet
  }

  private def unpack(deps: Seq[File], extractTarget: File, log: Logger): Seq[File] = {
    IO.createDirectory(extractTarget)
    deps.flatMap { dep =>
      val seq = IO.unzip(dep, extractTarget, "*.proto").toSeq
      if (!seq.isEmpty) log.debug("Extracted " + seq.mkString(","))
      seq
    }
  }

  private def sourceGeneratorTask = (streams, sourceDirectory in protobufConfig, externalIncludePath in protobufConfig, javaSource in protobufConfig, includePaths in protobufConfig, cacheDirectory, protoc) map {
    (out, srcDir, externalDir, targetDir, includePaths, cache, protocCommand) =>
      val cachedCompile = FileFunction.cached(cache / "protobuf", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
        compile(protocCommand, Seq(srcDir, externalDir), targetDir, includePaths, out.log)
      }
      cachedCompile((srcDir ** "*.proto").get.toSet).toSeq
  }

  private def unpackDependenciesTask = (streams, managedClasspath in protobufConfig, externalIncludePath in protobufConfig) map {
    (out, deps, extractTarget) =>
      val extractedFiles = unpack(deps.map(_.data), extractTarget, out.log)
      UnpackedDependencies(extractTarget, extractedFiles)
  }

}
