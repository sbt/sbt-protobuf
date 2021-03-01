package sbtprotobuf

import sbt._
import Keys._
import sbt.internal.io.Source

private[sbtprotobuf] trait Compat { self: ScopedProtobufPlugin =>
  import self.Keys._

  protected[this] val Process = scala.sys.process.Process

  protected[this] val setProtoArtifact = artifact in (ProtobufConfig, protobufPackage) := {
    val previous: Artifact = (artifact in (ProtobufConfig, protobufPackage)).value
    previous
      .withConfigurations(Vector(ProtobufConfig))
      .withClassifier(Some(protoClassifier))
  }

  protected[this] val watchSourcesSetting =
    watchSources += new Source((sourceDirectory in ProtobufConfig).value, "*.proto", AllPassFilter)

  protected[this] lazy val ProtobufConfig = Configuration.of("ProtobufConfig", "protobuf" + configurationPostfix)
}
