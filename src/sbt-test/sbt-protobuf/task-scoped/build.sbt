import sbtprotobuf.ProtobufTestPlugin.{Keys => PBT}

scalaVersion := "2.10.7"

crossScalaVersions += "2.11.12"

enablePlugins(ProtobufPlugin, ProtobufTestPlugin)

version in PBT.ProtobufConfig := (version in ProtobufConfig).value

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (version in ProtobufConfig).value % ProtobufConfig.name

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (version in PBT.ProtobufConfig).value % PBT.ProtobufConfig.name

protobufRunProtoc in ProtobufConfig := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v390" +: args.toArray)
}

PBT.protobufRunProtoc in PBT.ProtobufConfig := (protobufRunProtoc in ProtobufConfig).value

excludeFilter in ProtobufConfig := "test1.proto"

excludeFilter in PBT.ProtobufConfig := "test3.proto"

unmanagedResourceDirectories in Compile += (sourceDirectory in ProtobufConfig).value

unmanagedResourceDirectories in Test += (sourceDirectory in PBT.ProtobufConfig).value

TaskKey[Unit]("checkJar") := {
  val compileJar = (packageBin in Compile).value
  val testJar = (packageBin in Test).value

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
mainClass in compile := Some("whatever")
