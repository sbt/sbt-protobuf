package sbtprotobuf

import sbt._
import Keys._
import net.sandrogrzicic.scalabuff.compiler.ScalaBuff
import java.io.File

object ScalaBuffPlugin {

  import ProtobufPlugin._

  val scalaBuffConfig = config("scalabuff")


  lazy val scalaBuffSettings: Seq[Setting[_]] = Internals.scopedSettings(scalaBuffConfig) ++ Seq(
    generatedSource in scalaBuffConfig <<= (sourceManaged in Compile) { _ / "compiled_scalabuff" },
    scalaSource in scalaBuffConfig <<= (generatedSource in scalaBuffConfig).identity,
    generatorExecution in scalaBuffConfig := ((srcDir: File, target: File, includePaths: Seq[File], log:Logger) => executeScalaBuff(srcDir, target, includePaths, log)),
    libraryDependencies <+= scalaVersion(sv => "scalabuffruntime" % "scalabuffruntime_%s".format(sv) % "0.10-SNAPSHOT")
  )

  private def executeScalaBuff(srcDir: File, target: File, includePaths: Seq[File], log: Logger) = {
    val schemas = (srcDir ** "*.proto").get
    val scalaOut = "--scala_out=" + target.absolutePath
    val incPath = includePaths.map(_.absolutePath).mkString("-IPATH", " -IPATH", "")
    ScalaBuff.main(Array(scalaOut, incPath) ++ schemas.map(_.absolutePath))
    0
  }

}
