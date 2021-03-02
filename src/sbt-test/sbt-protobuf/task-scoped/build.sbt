import sbtprotobuf.ProtobufTestPlugin.{Keys => PBT}

scalaVersion := "2.10.7"

crossScalaVersions += "2.11.12"

enablePlugins(ProtobufPlugin, ProtobufTestPlugin)

(PBT.ProtobufConfig / version) := (ProtobufConfig / version).value

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (ProtobufConfig / version).value % ProtobufConfig.name

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (PBT.ProtobufConfig / version).value % PBT.ProtobufConfig.name

(PBT.ProtobufConfig / PBT.protobufRunProtoc) := (ProtobufConfig / protobufRunProtoc).value

ProtobufConfig / excludeFilter := "test1.proto"

PBT.ProtobufConfig / excludeFilter := "test3.proto"

(Compile / unmanagedResourceDirectories) += (ProtobufConfig / sourceDirectory).value

(Test / unmanagedResourceDirectories) += (PBT.ProtobufConfig / sourceDirectory).value

TaskKey[Unit]("checkJar") := {
  val compileJar = (Compile / packageBin).value
  val testJar = (Test / packageBin).value

  IO.withTemporaryDirectory{ dir =>
    val files = IO.unzip(compileJar, dir, "*.proto")
    val expect = Set("test1.proto", "test2.proto").map(dir / _)
    val testfiles = IO.unzip(testJar, dir, "*.proto")
    val testexpect = Set("test3.proto", "test4.proto").map(dir / _)
    assert(files == expect, s"$files $expect")
    assert(testfiles == testexpect, s"$testfiles $testexpect")
  }
}

// https://github.com/sbt/sbt-protobuf/issues/37
compile / mainClass := Some("whatever")
