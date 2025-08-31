package sbtprotobuf

import sbt._
import Keys._
import sbt.internal.io.Source
import sbtprotobuf.ProtobufPluginCompat._

private[sbtprotobuf] trait Compat { self: ScopedProtobufPlugin =>
  import self.Keys._

  protected[this] val Process = scala.sys.process.Process

  protected[this] val setProtoArtifact = (ProtobufConfig / protobufPackage / artifact) := {
    val previous: Artifact = (ProtobufConfig / protobufPackage / artifact).value
    previous
      .withConfigurations(Vector(ProtobufConfig))
      .withClassifier(Some(protoClassifier))
  }

  protected[this] val watchSourcesSetting =
    watchSources += Def.uncached(
      new Source((ProtobufConfig / sourceDirectory).value, "*.proto", AllPassFilter)
    )
}
