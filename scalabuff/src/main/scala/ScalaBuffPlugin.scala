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
    version in scalaBuffConfig := "0.10-SNAPSHOT",
    generatorExecution in scalaBuffConfig := ((srcDir: File, target: File, includePaths: Seq[File], log:Logger) => executeScalaBuff(srcDir, target, includePaths, log)),
    libraryDependencies <+= (scalaVersion, version in scalaBuffConfig)((sv, sbv) => "scalabuffruntime" % "scalabuffruntime_%s".format(sv) % sbv)
  )

  private def executeScalaBuff(srcDir: File, target: File, includePaths: Seq[File], log: Logger) = {
    val schemas = (srcDir ** "*.proto").get
    val scalaOut = "--scala_out=" + target.absolutePath
    val incPath = includePaths.map(_.absolutePath).mkString("-IPATH", " -IPATH", "")
    ScalaBuff.main(Array(scalaOut, incPath) ++ schemas.map(_.absolutePath))
    0
  }

  def addProtocCompatibility = libraryDependencies <++= (version in scalaBuffConfig)(sbv => Seq(
    "scalabuffruntime" % "scalabuffruntime_2.9.1" % sbv % protobufConfig.name,
    "scalabuffruntime" % "scalabuffruntime_2.9.1" % sbv)
  )

}
