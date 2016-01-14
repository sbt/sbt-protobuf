organization := "com.github.gseitz"

name := "sbt-protobuf"

scalacOptions := Seq("-deprecation", "-unchecked", "-Xlint", "-Yno-adapted-args")

sbtPlugin := true

publishMavenStyle := false

bintrayOrganization := Some("sbt")

bintrayRepository := "sbt-plugin-releases"

bintrayPackage := "sbt-protobuf"

bintrayReleaseOnPublish := false

ScriptedPlugin.scriptedSettings

scriptedBufferLog := false

scriptedLaunchOpts <+= version( x => s"-Dplugin.version=${x}" )
