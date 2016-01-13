organization := "com.github.gseitz"

name := "sbt-protobuf"

version := "0.5.0"

scalacOptions := Seq("-deprecation", "-unchecked")

sbtPlugin := true

publishMavenStyle := false

bintrayOrganization := Some("sbt")

bintrayRepository := "sbt-plugin-releases"

bintrayPackage := "sbt-protobuf"

bintrayReleaseOnPublish := false

