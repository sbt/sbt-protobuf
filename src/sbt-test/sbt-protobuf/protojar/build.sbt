enablePlugins(ProtobufPlugin)

scalaVersion := "2.10.6"

version in protobufConfig := "3.3.1"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (version in protobufConfig).value % protobufConfig.name

protobufRunProtoc in protobufConfig := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v330" +: args.toArray)
}

addArtifact(artifact in (protobufConfig, protobufPackage), protobufPackage in protobufConfig)

TaskKey[Unit]("checkJar") := IO.withTemporaryDirectory{ dir =>
  val files = IO.unzip((protobufPackage in protobufConfig).value, dir)
  val expect = Set(dir / "test1.proto", dir / "META-INF" / "MANIFEST.MF")
  assert(files == expect, s"$files $expect")
}
