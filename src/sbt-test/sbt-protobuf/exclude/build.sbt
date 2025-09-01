enablePlugins(ProtobufPlugin)
version := "0.1.0-SNAPSHOT"
name := "exclude-test"
scalaVersion := "2.13.13"

libraryDependencies ++= Seq(
  "com.google.protobuf" % "protobuf-java" % (ProtobufConfig / version).value % ProtobufConfig,
)

ProtobufConfig / sourceDirectories += (ProtobufConfig / protobufExternalIncludePath).value

TaskKey[Unit]("checkJar") := {
  val jar = (Compile / packageBinFile).value
  IO.withTemporaryDirectory{ dir =>
    val files = IO.unzip(jar, dir)
    val expect = Set(dir / "META-INF" / "MANIFEST.MF")
    assert(files == expect, s"""actual = $files,
expected = $expect""")
  }
}
