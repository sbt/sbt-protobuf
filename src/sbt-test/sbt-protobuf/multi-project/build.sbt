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
