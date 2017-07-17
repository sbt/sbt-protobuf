enablePlugins(ProtobufPlugin)

scalaVersion := "2.10.6"

version in ProtobufConfig := "3.3.1"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (version in ProtobufConfig).value % ProtobufConfig.name

protobufRunProtoc in ProtobufConfig := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v330" +: args.toArray)
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
