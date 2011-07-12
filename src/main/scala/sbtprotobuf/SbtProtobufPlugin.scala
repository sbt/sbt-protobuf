package sbtprotobuf

import sbt._
import Process._
import Keys._

import java.io.File


object SbtProtobufPlugin extends Plugin {
  val Protobuf = config("protobuf")

  val protoSource = SettingKey[File]("source", "The path containing the *.proto files.")
  val protoTarget = SettingKey[File]("generated-source", "The path for the generated protobuf java code.")
  val protoIncludePaths = SettingKey[Seq[File]]("include-path")
  val protoLibraryDependencies = SettingKey[Seq[sbt.ModuleID]]("library-dependencies", "Libraries containing *.proto files.")
  val protoc = SettingKey[String]("protoc", "The path+name of the protoc executable.")
  val protobufVersion = SettingKey[String]("version", "The version of the protobuf library.")
  val protoDependencyIncludePath = SettingKey[File]("dependency-include")

  val protoClean = TaskKey[Unit]("clean", "Clean just the files generated from protobuf sources.")
  val protobufCompile = TaskKey[Seq[File]]("generate", "Compile the protobuf sources.")
  val protoUnpackDependencies = TaskKey[Seq[File]]("unpack-dependencies", "Unpack dependencies.")

  override lazy val settings = inConfig(Protobuf)(Seq(
    protoSource <<= (sourceDirectory in Compile) { _ / "protobuf" },
    protoTarget <<= (sourceManaged in Compile) { _ / "compiled_protobuf" },
    protoDependencyIncludePath <<= target(_ / "protobuf_external"),
    protoIncludePaths <<= (protoSource in Protobuf)(_=>/*identity(_) ::*/ Nil),
    protoIncludePaths <+= (protoDependencyIncludePath in Protobuf).identity,
    protoLibraryDependencies := Nil,
    protoc := "protoc",
    protobufVersion := "2.4.1",

    protoClean <<= protoCleanTask,
    protoUnpackDependencies <<= protoUnpackDependenciesTask,

    protobufCompile <<= protoSourceGeneratorTask,
    protobufCompile <<= protobufCompile.dependsOn(protoUnpackDependencies)
  )) ++ Seq(
    sourceGenerators in Compile <+= (protobufCompile in Protobuf).identity,
    cleanFiles <+= (protoTarget in Protobuf).identity,
    libraryDependencies <+= (protobufVersion in Protobuf)("com.google.protobuf" % "protobuf-java" % _),
    libraryDependencies <++= protoLibraryDependencies(identity(_))
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
    deps.flatMap { dep =>
      IvyCache.withCachedJar(dep, None, log) { jar =>
        val seq = IO.unzip(jar, extractTarget, protoFilter).toSeq
        log.info("Extracted " + seq.mkString(","))
        seq
      }
    }
  }

  private def protoCleanTask = (streams, protoTarget in Protobuf) map {
    (out, target) =>
      out.log.info("Cleaning generated java under " + target)
      IO.delete(target)
  }

  private def protoSourceGeneratorTask = (streams, protoSource in Protobuf, protoTarget in Protobuf, protoIncludePaths in Protobuf) map {
    (out, srcDir, targetDir, includePaths) =>
      compileChanged(srcDir, targetDir, includePaths, out.log)
  }

  private def protoUnpackDependenciesTask = (streams, protoLibraryDependencies in Protobuf, protoDependencyIncludePath in Protobuf) map {
    (out, deps, extractTarget) =>
      unpack(deps, extractTarget, out.log)
  }

}
