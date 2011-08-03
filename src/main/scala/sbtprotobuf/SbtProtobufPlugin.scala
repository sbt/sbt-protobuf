package sbtprotobuf

import sbt._
import Process._
import Keys._

import java.io.File

object SbtProtobufPlugin extends Plugin {
  val Protobuf = config("protobuf")

  val protoSource = SettingKey[File]("proto-source", "The path containing the *.proto files.")
  val managedSourceDirectory = SettingKey[File]("managed-source-directory", "The path for the generated protobuf java code.")
  val includePaths = SettingKey[Seq[File]]("include-paths", "The paths that contain *.proto dependencies.")
  val protoc = SettingKey[String]("protoc", "The path+name of the protoc executable.")
  val externalIncludePath = SettingKey[File]("external-include-path", "The path to which protobuf:library-dependencies are extracted and which is used as protobuf:include-path for protoc")

  val generate = TaskKey[Seq[File]]("generate", "Compile the protobuf sources.")
  val unpackDependencies = TaskKey[Seq[File]]("unpack-dependencies", "Unpack dependencies.")

  lazy val protobufSettings: Seq[Setting[_]] = inConfig(Protobuf)(Seq[Setting[_]](
    protoSource <<= (sourceDirectory in Compile) { _ / "protobuf" },
    managedSourceDirectory <<= (sourceManaged in Compile) { _ / "compiled_protobuf" },
    externalIncludePath <<= target(_ / "protobuf_external"),
    includePaths <<= (protoSource in Protobuf)(identity(_) :: Nil),
    includePaths <+= (externalIncludePath in Protobuf).identity,
    libraryDependencies := Nil,
    protoc := "protoc",
    version := "2.4.1",

    unpackDependencies <<= unpackDependenciesTask,

    generate <<= sourceGeneratorTask,
    generate <<= generate.dependsOn(unpackDependencies)
  )) ++ Seq[Setting[_]](
    sourceGenerators in Compile <+= (generate in Protobuf).identity,
    cleanFiles <+= (managedSourceDirectory in Protobuf).identity,
    libraryDependencies <+= (version in Protobuf)("com.google.protobuf" % "protobuf-java" % _),
    libraryDependencies <++= (libraryDependencies in Protobuf).identity,
    managedSourceDirectories in Compile <+= (managedSourceDirectory in Protobuf).identity
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
        log.debug("No protobuf files to compile")
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

  private def sourceGeneratorTask = (streams, protoSource in Protobuf, managedSourceDirectory in Protobuf, includePaths in Protobuf) map {
    (out, srcDir, targetDir, includePaths) =>
      compileChanged(srcDir, targetDir, includePaths, out.log)
  }

  private def unpackDependenciesTask = (streams, libraryDependencies in Protobuf, externalIncludePath in Protobuf) map {
    (out, deps, extractTarget) =>
      unpack(deps, extractTarget, out.log)
  }

}
