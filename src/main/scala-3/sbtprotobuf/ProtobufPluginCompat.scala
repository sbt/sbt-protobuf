package sbtprotobuf

import sbt._
import sbt.Def.Classpath

private[sbtprotobuf] object ProtobufPluginCompat {
  type FileRef = HashedVirtualFileRef

  def managedJars(config: Configuration, jarTypes: Set[String], up: UpdateReport, converter: FileConverter): Classpath =
    Classpaths.managedJars(config, jarTypes, up, converter)

  def toFile(fileRef: HashedVirtualFileRef, converter: FileConverter): File =
    converter.toPath(fileRef).toFile

  def toFileRef(file: File, converter: FileConverter): HashedVirtualFileRef =
    converter.toVirtualFile(file.toPath)
}
