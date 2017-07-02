package sbtprotobuf

import sbt._
import Keys._

private[sbtprotobuf] trait Compat { self: ScopedProtobufPlugin =>
  import self.Keys._

  protected[this] val Process = sbt.Process

  protected[this] val setProtoArtifact = artifact in (protobufConfig, protobufPackage) := {
    val previous: Artifact = (artifact in (protobufConfig, protobufPackage)).value
    previous
      .copy(configurations = List(protobufConfig))
      .copy(classifier = Some(protoClassifier))
    }
}
