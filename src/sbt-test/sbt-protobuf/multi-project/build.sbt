val commonSettings = Seq(
  scalaVersion := "2.11.11",
  version in ProtobufConfig := "3.3.1",
  protobufRunProtoc in ProtobufConfig := { args =>
    com.github.os72.protocjar.Protoc.runProtoc("-v330" +: args.toArray)
  }
)

val foo = project.settings(
  commonSettings
).enablePlugins(ProtobufPlugin)

val bar = project.settings(
  commonSettings,
  protobufIncludePaths in ProtobufConfig += (sourceDirectory in ProtobufConfig in foo).value
).dependsOn(foo).enablePlugins(ProtobufPlugin)
