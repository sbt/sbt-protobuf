package sbtprotobuf

import sbt._
import Keys._

private[sbtprotobuf] trait Compat { self: ScopedProtobufPlugin =>
  import self.Keys._

  protected[this] val Process = sbt.Process

  protected[this] val setProtoArtifact = artifact in (ProtobufConfig, protobufPackage) := {
    val previous: Artifact = (artifact in (ProtobufConfig, protobufPackage)).value
    previous
      .copy(configurations = List(ProtobufConfig))
      .copy(classifier = Some(protoClassifier))
    }

  protected[this] val watchSourcesSetting =
    watchSources ++= ((sourceDirectory in ProtobufConfig).value ** "*.proto").get

  protected[this] lazy val ProtobufConfig = config("protobuf" + configurationPostfix)
}
