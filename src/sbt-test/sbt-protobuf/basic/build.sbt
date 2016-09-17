import sbtprotobuf.{ProtobufPlugin=>PB}

PB.protobufSettings

version in PB.protobufConfig := "3.0.0"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (version in PB.protobufConfig).value % PB.protobufConfig.name

PB.runProtoc in PB.protobufConfig := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v300" +: args.toArray)
}

excludeFilter in PB.protobufConfig := "test1.proto"

unmanagedResourceDirectories in Compile += (sourceDirectory in PB.protobufConfig).value

TaskKey[Unit]("checkJar") := IO.withTemporaryDirectory{ dir =>
  val files = IO.unzip((packageBin in Compile).value, dir, "*.proto")
  val expect = Set("test1.proto", "test2.proto").map(dir / _)
  assert(files == expect, s"$files $expect")
}

// https://github.com/sbt/sbt-protobuf/issues/37
mainClass in compile := Some("whatever")
