package sbtprotobuf

import sbt._
import Keys._

import java.io.File


object ProtobufPlugin extends Plugin {
  val protobufConfig = config("protobuf")

  val includePaths = TaskKey[Seq[File]]("protobuf-include-paths", "The paths that contain *.proto dependencies.")
  val protoc = SettingKey[String]("protobuf-protoc", "The path+name of the protoc executable.")
  val externalIncludePath = SettingKey[File]("protobuf-external-include-path", "The path to which protobuf:library-dependencies are extracted and which is used as protobuf:include-path for protoc")
  val generatedTargets = SettingKey[Seq[(File,String)]]("protobuf-generated-targets", "Targets for protoc: target directory and glob for generated source files")
  val generate = TaskKey[Seq[File]]("protobuf-generate", "Compile the protobuf sources.")
  val unpackDependencies = TaskKey[UnpackedDependencies]("protobuf-unpack-dependencies", "Unpack dependencies.")
  val protocOptions = SettingKey[Seq[String]]("protobuf-protoc-options", "Additional options to be passed to protoc")

  lazy val protobufSettings: Seq[Setting[_]] = inConfig(protobufConfig)(Seq[Setting[_]](
    sourceDirectory <<= (sourceDirectory in Compile) { _ / "protobuf" },
    javaSource <<= (sourceManaged in Compile) { _ / "compiled_protobuf" },
    externalIncludePath <<= target(_ / "protobuf_external"),
    protoc := "protoc",
    version := "2.5.0",

    generatedTargets := Nil,
    generatedTargets <+= (javaSource in protobufConfig)((_, "*.java")), // add javaSource to the list of patterns

    protocOptions := Nil,
    protocOptions <++= (generatedTargets in protobufConfig){ generatedTargets => // if a java target is provided, add java generation option
      generatedTargets.find(_._2.endsWith(".java")) match {
        case Some(targetForJava) => Seq("--java_out=%s".format(targetForJava._1.absolutePath))
        case None => Nil
      }
    },

    managedClasspath <<= (classpathTypes, update) map { (ct, report) =>
      Classpaths.managedJars(protobufConfig, ct, report)
    },

    unpackDependencies <<= unpackDependenciesTask,

    includePaths <<= (sourceDirectory in protobufConfig) map (identity(_) :: Nil),
    includePaths <+= unpackDependencies map { _.dir },

    generate <<= sourceGeneratorTask

  )) ++ Seq[Setting[_]](
    sourceGenerators in Compile <+= generate in protobufConfig,
    cleanFiles <++= (generatedTargets in protobufConfig){_.map{_._1}},
    managedSourceDirectories in Compile <++= (generatedTargets in protobufConfig){_.map{_._1}},
    libraryDependencies <+= (version in protobufConfig)("com.google.protobuf" % "protobuf-java" % _),
    ivyConfigurations += protobufConfig
  )

  case class UnpackedDependencies(dir: File, files: Seq[File])

  private def executeProtoc(protocCommand: String, srcDir: File, includePaths: Seq[File], protocOptions: Seq[String], log: Logger) =
    try {
      val schemas = (srcDir ** "*.proto").get.map(_.absolutePath)
      val incPath = includePaths.map("-I" + _.absolutePath)
      val proc = Process(
        protocCommand,
        incPath ++ protocOptions ++ schemas
      )
      proc ! log
    } catch { case e: Exception =>
      throw new RuntimeException("error occured while compiling protobuf files: %s" format(e.getMessage), e)
    }


  private def compile(protocCommand: String, srcDir: File, includePaths: Seq[File], protocOptions: Seq[String], generatedTargets: Seq[(File, String)], log: Logger) = {
    val schemas = (srcDir ** "*.proto").get
    val generatedTargetDirs = generatedTargets.map(_._1)

    generatedTargetDirs.foreach(_.mkdirs())

    log.info("Compiling %d protobuf files to %s".format(schemas.size, generatedTargetDirs.mkString(",")))
    log.debug("protoc options:")
    protocOptions.map("\t"+_).foreach(log.debug(_))
    schemas.foreach(schema => log.info("Compiling schema %s" format schema))

    val exitCode = executeProtoc(protocCommand, srcDir, includePaths, protocOptions, log)
    if (exitCode != 0)
      sys.error("protoc returned exit code: %d" format exitCode)

    log.info("Compiling protobuf")
    generatedTargetDirs.foreach{ dir =>
      log.info("Protoc target directory: %s".format(dir.absolutePath))
    }

    (generatedTargets.flatMap{ot => (ot._1 ** ot._2).get}).toSet
  }

  private def unpack(deps: Seq[File], extractTarget: File, log: Logger): Seq[File] = {
    IO.createDirectory(extractTarget)
    deps.flatMap { dep =>
      val seq = IO.unzip(dep, extractTarget, "*.proto").toSeq
      if (!seq.isEmpty) log.debug("Extracted " + seq.mkString(","))
      seq
    }
  }

  private def sourceGeneratorTask =
    (streams, sourceDirectory in protobufConfig, includePaths in protobufConfig, protocOptions in protobufConfig, generatedTargets in protobufConfig, cacheDirectory, protoc) map {
    (out, srcDir, includePaths, protocOpts, otherTargets, cache, protocCommand) =>
      val cachedCompile = FileFunction.cached(cache / "protobuf", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
        compile(protocCommand, srcDir, includePaths, protocOpts, otherTargets, out.log)
      }
      cachedCompile((srcDir ** "*.proto").get.toSet).toSeq
  }

  private def unpackDependenciesTask = (streams, managedClasspath in protobufConfig, externalIncludePath in protobufConfig) map {
    (out, deps, extractTarget) =>
      val extractedFiles = unpack(deps.map(_.data), extractTarget, out.log)
      UnpackedDependencies(extractTarget, extractedFiles)
  }
}
