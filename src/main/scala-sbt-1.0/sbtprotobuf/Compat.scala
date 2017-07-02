package sbtprotobuf

import sbt._
import Keys._

private[sbtprotobuf] trait Compat { self: ScopedProtobufPlugin =>
  import self.Keys._

  protected[this] val Process = scala.sys.process.Process

  protected[this] val setProtoArtifact = artifact in (protobufConfig, protobufPackage) := {
    val previous: Artifact = (artifact in (protobufConfig, protobufPackage)).value
    previous
      .withConfigurations(Vector(protobufConfig))
      .withClassifier(Some(protoClassifier))
  }
}
