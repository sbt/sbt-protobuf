val commonSettings = Seq(
  scalaVersion := "2.11.11",
  version in protobufConfig := "3.3.1",
  protobufRunProtoc in protobufConfig := { args =>
    com.github.os72.protocjar.Protoc.runProtoc("-v330" +: args.toArray)
  }
)

val foo = project.settings(
  commonSettings
).enablePlugins(ProtobufPlugin)

val bar = project.settings(
  commonSettings,
  protobufIncludePaths in protobufConfig += (sourceDirectory in protobufConfig in foo).value
).dependsOn(foo).enablePlugins(ProtobufPlugin)
