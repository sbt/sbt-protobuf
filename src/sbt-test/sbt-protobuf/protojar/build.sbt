enablePlugins(ProtobufPlugin)

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.7"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (version in ProtobufConfig).value % ProtobufConfig.name

protobufRunProtoc in ProtobufConfig := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v390" +: args.toArray)
}

addArtifact(artifact in (ProtobufConfig, protobufPackage), protobufPackage in ProtobufConfig)

TaskKey[Unit]("checkJar") := {
  val jar = (protobufPackage in ProtobufConfig).value
  IO.withTemporaryDirectory{ dir =>
    val files = IO.unzip(jar, dir)
    val expect = Set(dir / "test1.proto", dir / "META-INF" / "MANIFEST.MF")
    assert(files == expect, s"$files $expect")
  }
}
