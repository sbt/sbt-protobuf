package sbtprotobuf

import sbt._
import sbt.Def.Classpath
import xsbti.FileConverter
import java.io.File

private[sbtprotobuf] object ProtobufPluginCompat {
  type FileRef = File

  implicit class DefOps(private val self: sbt.Def.type) extends AnyVal {
    def uncached[A](a: A): A = a
  }

  def managedJars(config: Configuration, jarTypes: Set[String], up: UpdateReport, converter: FileConverter): Classpath =
    Classpaths.managedJars(config, jarTypes, up)

  def toFileRef(file: File, converter: FileConverter): File = file

  def toFile(file: File, converter: FileConverter): File = file
}
