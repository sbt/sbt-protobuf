package sbtprotobuf

import sbt._
import Process._
import Keys._

import java.io.File

object ProtobufPlugin extends Plugin {
  val protobufConfig = config("protobuf")

  val includePaths = SettingKey[Seq[File]]("include-paths", "The paths that contain *.proto dependencies.")
  val protoc = SettingKey[String]("protoc", "The path+name of the protoc executable.")
  val externalIncludePath = SettingKey[File]("external-include-path", "The path to which protobuf:library-dependencies are extracted and which is used as protobuf:include-path for protoc")

  val generate = TaskKey[Seq[File]]("generate", "Compile the protobuf sources.")
  val unpackDependencies = TaskKey[Seq[File]]("unpack-dependencies", "Unpack dependencies.")

  lazy val protobufSettings: Seq[Setting[_]] = inConfig(protobufConfig)(Seq[Setting[_]](
    sourceDirectory <<= (sourceDirectory in Compile) { _ / "protobuf" },
    javaSource <<= (sourceManaged in Compile) { _ / "compiled_protobuf" },
    externalIncludePath <<= target(_ / "protobuf_external"),
    includePaths <<= (sourceDirectory in protobufConfig)(identity(_) :: Nil),
    includePaths <+= (externalIncludePath in protobufConfig).identity,
    protoc := "protoc",
    version := "2.4.1",

    managedClasspath <<= (classpathTypes in protobufConfig, update) map { (ct, report) =>
      Classpaths.managedJars(protobufConfig, ct, report)
    },

    unpackDependencies <<= unpackDependenciesTask,

    generate <<= sourceGeneratorTask,
    generate <<= generate.dependsOn(unpackDependencies)
  )) ++ Seq[Setting[_]](
    sourceGenerators in Compile <+= (generate in protobufConfig).identity,
    cleanFiles <+= (javaSource in protobufConfig).identity,
    libraryDependencies <+= (version in protobufConfig)("com.google.protobuf" % "protobuf-java" % _),
    managedSourceDirectories in Compile <+= (javaSource in protobufConfig).identity,
    ivyConfigurations += protobufConfig
  )

  private def executeProtoc(sources: File, target: File, includePaths: Seq[File], log: Logger) =
    try {
      val schemas = (PathFinder(sources) ** "*.proto").get
      val incPath = includePaths.map(_.absolutePath).mkString("-I", " -I", "")
      <x>protoc {incPath} --java_out={target.absolutePath} {schemas.map(_.absolutePath).mkString(" ")}</x> ! log
    } catch { case e: Exception =>
      throw new RuntimeException("error occured while compiling protobuf files: %s" format(e.getMessage), e)
    }


  private def compile(sources: File, target: File, includePaths: Seq[File], log: Logger) = {
    val schemas = (PathFinder(sources) ** "*.proto").get
    target.mkdirs()
    log.info("Compiling %d protobuf files to %s".format(schemas.size, target))
    schemas.foreach { schema => log.info("Compiling schema %s" format schema) }

    val exitCode = executeProtoc(sources, target, includePaths, log)
    if (exitCode != 0)
      error("protoc returned exit code: %d" format exitCode)

    (target ** "*.java").get.toSet
  }

  private val protoFilter = new SimpleFilter((name: String) => name.endsWith(".proto"))
  private def unpack(deps: Seq[File], extractTarget: File, log: Logger): Seq[File] = {
    IO.createDirectory(extractTarget)
    deps.flatMap { dep =>
      val seq = IO.unzip(dep, extractTarget, protoFilter).toSeq
      if (!seq.isEmpty) log.debug("Extracted " + seq.mkString(","))
      seq
    }
  }

  private def sourceGeneratorTask = (streams, sourceDirectory in protobufConfig, javaSource in protobufConfig, includePaths in protobufConfig, cacheDirectory) map {
    (out, srcDir, targetDir, includePaths, cache) =>
      val cachedCompile = FileFunction.cached(cache / "protobuf", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
        compile(srcDir, targetDir, includePaths, out.log)
      }
      cachedCompile((srcDir ** "*.proto").get.toSet).toSeq
  }

  private def unpackDependenciesTask = (streams, managedClasspath in protobufConfig, externalIncludePath in protobufConfig) map {
    (out, deps, extractTarget) =>
      unpack(deps.map(_.data), extractTarget, out.log)
  }

}
