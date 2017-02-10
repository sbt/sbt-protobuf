import sbtprotobuf.ScopedProtobufPlugin
import sbtprotobuf.{ProtobufPlugin=>PB}
import sbtprotobuf.{ProtobufTestPlugin=>PBT}

PB.protobufSettings

PBT.protobufSettings

version in PB.protobufConfig := "3.1.0"

version in PBT.protobufConfig := (version in PB.protobufConfig).value

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (version in PB.protobufConfig).value % PB.protobufConfig.name

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (version in PBT.protobufConfig).value % PBT.protobufConfig.name

PB.runProtoc in PB.protobufConfig := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v310" +: args.toArray)
}

PBT.runProtoc in PBT.protobufConfig := (PB.runProtoc in PB.protobufConfig).value

excludeFilter in PB.protobufConfig := "test1.proto"

excludeFilter in PBT.protobufConfig := "test3.proto"

unmanagedResourceDirectories in Compile += (sourceDirectory in PB.protobufConfig).value

unmanagedResourceDirectories in Test += (sourceDirectory in PBT.protobufConfig).value

TaskKey[Unit]("checkJar") := IO.withTemporaryDirectory{ dir =>
  val files = IO.unzip((packageBin in Compile).value, dir, "*.proto")
  val expect = Set("test1.proto", "test2.proto").map(dir / _)
  val testfiles = IO.unzip((packageBin in Test).value, dir, "*.proto")
  val testexpect = Set("test3.proto", "test4.proto").map(dir / _)
  assert(files == expect, s"$files $expect")
  assert(testfiles == testexpect, s"$testfiles $testexpect")
}

// https://github.com/sbt/sbt-protobuf/issues/37
mainClass in compile := Some("whatever")
