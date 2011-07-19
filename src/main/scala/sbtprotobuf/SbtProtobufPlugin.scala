package sbtprotobuf

import sbt._
import Process._
import Keys._

import java.io.File


object SbtProtobufPlugin extends Plugin {
  val Protobuf = config("protobuf")

  val protoSourcePath = SettingKey[File]("source-path", "The path containing the *.proto files.")
  val protoGeneratedSourcePath = SettingKey[File]("generated-source-path", "The path for the generated protobuf java code.")
  val protoIncludePaths = SettingKey[Seq[File]]("include-path")
  val protoLibraryDependencies = SettingKey[Seq[sbt.ModuleID]]("library-dependencies", "Libraries containing *.proto files.")
  val protoc = SettingKey[String]("protoc", "The path+name of the protoc executable.")
  val protoVersion = SettingKey[String]("version", "The version of the protobuf library.")
  val protoExternalProtobufIncludePath = SettingKey[File]("external-include-path", "The path to which protobuf:library-dependencies are extracted and which is used as protobuf:include-path for protoc")

  val protoClean = TaskKey[Unit]("clean", "Clean just the files generated from protobuf sources.")
  val protoGenerate = TaskKey[Seq[File]]("generate", "Compile the protobuf sources.")
  val protoUnpackDependencies = TaskKey[Seq[File]]("unpack-dependencies", "Unpack dependencies.")

  lazy val protobufSettings: Seq[Setting[_]] = inConfig(Protobuf)(Seq[Setting[_]](
    protoSourcePath <<= (sourceDirectory in Compile) { _ / "protobuf" },
    protoGeneratedSourcePath <<= (sourceManaged in Compile) { _ / "compiled_protobuf" },
    protoExternalProtobufIncludePath <<= target(_ / "protobuf_external"),
    protoIncludePaths <<= (protoSourcePath in Protobuf)(identity(_) :: Nil),
    protoIncludePaths <+= (protoExternalProtobufIncludePath in Protobuf).identity,
    protoLibraryDependencies := Nil,
    protoc := "protoc",
    protoVersion := "2.4.1",

    protoClean <<= protoCleanTask,
    protoUnpackDependencies <<= protoUnpackDependenciesTask,

    protoGenerate <<= protoSourceGeneratorTask,
    protoGenerate <<= protoGenerate.dependsOn(protoUnpackDependencies)
  )) ++ Seq[Setting[_]](
    sourceGenerators in Compile <+= (protoGenerate in Protobuf).identity,
    cleanFiles <+= (protoGeneratedSourcePath in Protobuf).identity,
    libraryDependencies <+= (protoVersion in Protobuf)("com.google.protobuf" % "protobuf-java" % _),
    libraryDependencies <++= (protoLibraryDependencies in Protobuf).identity
  )

  private def compile(sources: File, target: File, includePaths: Seq[File], log: Logger) =
    try {
      val schemas = (PathFinder(sources) ** "*.proto").get
      val incPath = (sources +: includePaths).map(_.absolutePath).mkString("-I", " -I", "")
      <x>protoc {incPath} --java_out={target.absolutePath} {schemas.map(_.absolutePath).mkString(" ")}</x> ! log
    } catch { case e: Exception =>
      throw new RuntimeException("error occured while compiling protobuf files: %s" format(e.getMessage), e)
    }


  private def compileChanged(sources: File, target: File, includePaths: Seq[File], log: Logger) = {
    val schemas = (PathFinder(sources) ** "*.proto").get
    schemas.map(_.lastModified).toList.sortWith(_ > _).headOption.map { mostRecentSchemaTimestamp =>
      if (mostRecentSchemaTimestamp > target.lastModified) {
        target.mkdirs()
        log.info("Compiling %d protobuf files to %s".format(schemas.size, target))
        schemas.foreach { schema => log.info("Compiling schema %s" format schema) }

        val exitCode = compile(sources, target, includePaths, log)
        if (exitCode == 0)
          target.setLastModified(mostRecentSchemaTimestamp)
        else
          error("protoc returned exit code: %d" format exitCode)

        (target ** "*.java").get
      } else {
        log.info("No protobuf files to compile")
        (target ** "*.java").get
      }
    }.getOrElse(Seq())
  }

  private val protoFilter = new SimpleFilter((name: String) => name.endsWith(".proto"))
  private def unpack(deps: Seq[ModuleID], extractTarget: File, log: Logger): Seq[File] = {
    IO.createDirectory(extractTarget)
    deps.flatMap { dep =>
      IvyCache.withCachedJar(dep, None, log) { jar =>
        val seq = IO.unzip(jar, extractTarget, protoFilter).toSeq
        log.info("Extracted " + seq.mkString(","))
        seq
      }
    }
  }

  private def protoCleanTask = (streams, protoGeneratedSourcePath in Protobuf) map {
    (out, target) =>
      out.log.info("Cleaning generated java under " + target)
      IO.delete(target)
  }

  private def protoSourceGeneratorTask = (streams, protoSourcePath in Protobuf, protoGeneratedSourcePath in Protobuf, protoIncludePaths in Protobuf) map {
    (out, srcDir, targetDir, includePaths) =>
      compileChanged(srcDir, targetDir, includePaths, out.log)
  }

  private def protoUnpackDependenciesTask = (streams, protoLibraryDependencies in Protobuf, protoExternalProtobufIncludePath in Protobuf) map {
    (out, deps, extractTarget) =>
      unpack(deps, extractTarget, out.log)
  }

}
