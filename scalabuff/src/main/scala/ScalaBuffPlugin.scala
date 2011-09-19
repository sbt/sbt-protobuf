package sbtprotobuf

import sbt._
import Keys._
import net.sandrogrzicic.scalabuff.compiler.ScalaBuff

object ScalaBuffPlugin {

  import ProtobufPlugin._

  lazy val scalaBuffSettings: Seq[Setting[_]] = protobufSettings ++ Seq(
    scalaSource in protobufConfig <<= (sourceManaged in Compile) { _ / "compiled_scalabuff" },
    generatorExecutions in protobufConfig := Seq(executeProtoc(_, _, _, _)),
    libraryDependencies <+= scalaVersion(sv => "scalabuffruntime" % "scalabuffruntime_%s".format(sv) % "0.9")
  )

  private def executeProtoc(srcDir: File, target: File, includePaths: Seq[File], log: Logger) = {
    val schemas = (srcDir ** "*.proto").get
    val scalaOut = "--scala_out=" + target.absolutePath
    val incPath = includePaths.map(_.absolutePath).mkString("-IPATH", " -IPATH", "")
    ScalaBuff.main(Array(scalaOut, incPath) ++ schemas.map(_.absolutePath))
    0
  }

}
