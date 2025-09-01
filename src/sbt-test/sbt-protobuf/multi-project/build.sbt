val commonSettings = Seq(
  scalaVersion := "2.11.12"
)

val foo = project.settings(
  commonSettings
).enablePlugins(ProtobufPlugin)

val bar = project.settings(
  commonSettings,
  (ProtobufConfig / protobufIncludePaths) += (foo / ProtobufConfig / sourceDirectory).value
).dependsOn(foo).enablePlugins(ProtobufPlugin)

val root = project.in(file("."))
  .settings(
    commonSettings,
    InputKey[Unit]("check") := {
      val f = file(
        if (sbtVersion.value.startsWith("1")) {
          s"bar/target/scala-${scalaBinaryVersion.value}/src_managed/main/compiled_protobuf/test/Test2OuterClass.java"
        } else {
          s"target/out/jvm/scala-${scalaVersion.value}/bar/src_managed/main/compiled_protobuf/test/Test2OuterClass.java"
        }
      )
      assert(f.isFile(), s"not found ${f}")
    }
  )
  .aggregate(foo, bar)
