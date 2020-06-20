val commonSettings = Seq(
  scalaVersion := "2.11.12"
)

val foo = project.settings(
  commonSettings
).enablePlugins(ProtobufPlugin)

val bar = project.settings(
  commonSettings,
  protobufIncludePaths in ProtobufConfig += (sourceDirectory in ProtobufConfig in foo).value
).dependsOn(foo).enablePlugins(ProtobufPlugin)
