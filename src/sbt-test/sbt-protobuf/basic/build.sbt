enablePlugins(ProtobufPlugin)

scalaVersion := "2.10.7"

crossScalaVersions += "2.11.12"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (version in ProtobufConfig).value % ProtobufConfig.name

protobufRunProtoc in ProtobufConfig := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v390" +: args.toArray)
}

excludeFilter in ProtobufConfig := "test1.proto"

unmanagedResourceDirectories in Compile += (sourceDirectory in ProtobufConfig).value

TaskKey[Unit]("checkJar") := {
  val jar = (packageBin in Compile).value
  IO.withTemporaryDirectory{ dir =>
    val files = IO.unzip(jar, dir, "*.proto")
    val expect = Set("test1.proto", "test2.proto").map(dir / _)
    assert(files == expect, s"$files $expect")
  }
}

// https://github.com/sbt/sbt-protobuf/issues/37
mainClass in compile := Some("whatever")
