enablePlugins(ProtobufPlugin)

scalaVersion := "2.10.7"

crossScalaVersions += "2.11.12"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (ProtobufConfig / version).value % ProtobufConfig.name

ProtobufConfig / excludeFilter := "test1.proto"

(Compile / unmanagedResourceDirectories) += (ProtobufConfig / sourceDirectory).value

TaskKey[Unit]("checkJar") := {
  val jar = (Compile / packageBin).value
  IO.withTemporaryDirectory{ dir =>
    val files = IO.unzip(jar, dir, "*.proto")
    val expect = Set("test1.proto", "test2.proto").map(dir / _)
    assert(files == expect, s"$files $expect")
  }
}

// https://github.com/sbt/sbt-protobuf/issues/37
compile / mainClass := Some("whatever")
