package sbtprotobuf

import sbt._
import Process._
import Keys._
import Project.Initialize

import java.io.File


object SbtProtobufPlugin extends Plugin {
  val Protobuf = config("protobuf")

  val protoSource = SettingKey[File]("source", "The path containing the *.proto files.")
  val protoTarget = SettingKey[File]("target", "The path for the generated protobuf java code.")
  val protoClean = TaskKey[Unit]("clean", "Clean just the files generated from protobuf sources.")
  val protobufCompile = TaskKey[Seq[File]]("generate", "Compile the protobuf sources.")
  val protoIncludePaths = SettingKey[Seq[File]]("include-path")
  val protoc = SettingKey[String]("protoc", "The path+name of the protoc executable.")
  val protobufVersion = SettingKey[String]("version", "The version of the protobuf library.")

  override lazy val settings = inConfig(Protobuf)(Seq(
    protoSource <<= (sourceDirectory in Compile) { _ / "protobuf" },
    protoTarget <<= (sourceManaged in Compile) { _ / "compiled_protobuf" },
    protoIncludePaths <<= (protoSource in Protobuf)(identity(_) :: Nil),
    protoClean <<= protoCleanTask,
    protobufCompile <<= protoSourceGeneratorTask,
    protoc := "protoc",
    protobufVersion := "2.4.1"
// wait until the geniuses of the protobuf team publish the jars to maven central.
// libraryDependencies <+= protobufVersion("com.google.protobuf" % "protobuf-java" % _)
  )) ++ Seq(
    sourceGenerators in Compile <+= (protobufCompile in Protobuf).identity,
    cleanFiles <+= (protoTarget in Protobuf).identity
    //compile in Compile <+= (protobufCompile in Protobuf).identity
  )
  
  private def outdated(protobuf: File, java: File) = 
    !java.exists || protobuf.lastModified > java.lastModified

  private def compile(sources: File, target: File, log: Logger) =
    try {
      //<x>protoc {incPath} --java_out={protobufOutputPath.absolutePath} {protobufSchemas.getPaths.mkString(" ")}</x> ! log
      val schemas = (PathFinder(sources) ** "*.proto").get
      val incPath = List(sources).map(_.absolutePath).mkString("-I", " -I", "")
      <x>protoc {incPath} --java_out={target.absolutePath} {schemas.map(_.absolutePath).mkString(" ")}</x> ! log
     
    } catch { case e: Exception =>
      throw new RuntimeException("error occured while compiling protobuf files: %s" format(e.getMessage), e)
    }




  private def compileChanged(sources: File, target: File, log: Logger) = {
    val schemas = (PathFinder(sources) ** "*.proto").get
    schemas.map(_.lastModified).toList.sortWith(_ > _).headOption.map { mostRecentSchemaTimestamp =>
      if (mostRecentSchemaTimestamp > target.lastModified) {
        target.mkdirs()
        log.info("Compiling %d protobuf files to %s".format(schemas.size, target))
        schemas.foreach { schema => log.info("Compiling schema %s".format(schema)) }
        val exitCode = compile(sources, target, log)
        if (exitCode == 0) target.setLastModified(mostRecentSchemaTimestamp)
        else log.error("protoc returned exit code: %d" format exitCode)
        (target ** "*.java").get
      } else {
        log.info("No protobuf files to compile")
        (target ** "*.java").get
      }
    }.getOrElse(Seq())
  }
         
  private def protoCleanTask = (streams, protoTarget in Protobuf) map {
    (out, target) =>
      out.log.info("Cleaning generated java under " + target)
      IO.delete(target)
  }

  private def protoSourceGeneratorTask = (streams, protoSource in Protobuf, protoTarget in Protobuf) map {
    (out, srcDir, targetDir) =>
      compileChanged(srcDir, targetDir, out.log)
  }

}
