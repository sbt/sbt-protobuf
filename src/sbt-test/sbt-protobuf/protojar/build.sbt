enablePlugins(ProtobufPlugin)

version := "0.1.0-SNAPSHOT"

name := "protojar"

scalaVersion := "2.10.7"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (ProtobufConfig / version).value % ProtobufConfig.name

ProtobufConfig / protobufRunProtoc := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v390" +: args.toArray)
}

addArtifact(ProtobufConfig / protobufPackage / artifact, ProtobufConfig / protobufPackage)

TaskKey[Unit]("checkJar") := {
  val jar = (ProtobufConfig / protobufPackage).value
  IO.withTemporaryDirectory{ dir =>
    val files = IO.unzip(jar, dir)
    val expect = Set(dir / "test1.proto", dir / "META-INF" / "MANIFEST.MF")
    assert(files == expect, s"$files $expect")
  }
}
