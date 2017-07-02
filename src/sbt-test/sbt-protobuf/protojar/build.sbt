import sbt.addArtifact
import sbtprotobuf.{ProtobufPlugin=>PB}

PB.protobufSettings

version in PB.protobufConfig := "3.2.0"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (version in PB.protobufConfig).value % PB.protobufConfig.name

PB.runProtoc in PB.protobufConfig := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v310" +: args.toArray)
}

addArtifact(artifact in (PB.protobufConfig, PB.packageProto), PB.packageProto in PB.protobufConfig)

TaskKey[Unit]("checkJar") := IO.withTemporaryDirectory{ dir =>
  val files = IO.unzip((PB.packageProto in PB.protobufConfig).value, dir)
  val expect = Set(dir / "test1.proto", dir / "META-INF" / "MANIFEST.MF")
  assert(files == expect, s"$files $expect")
}

// https://github.com/sbt/sbt-protobuf/issues/37
mainClass in compile := Some("whatever")
