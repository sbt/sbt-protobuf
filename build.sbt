sbtPlugin := true

organization := "com.github.gseitz"

name := "sbt-protobuf"

version := "0.2.1"

scalacOptions := Seq("-deprecation", "-unchecked")

publishTo := Some(Resolver.file("gseitz@github", file(Path.userHome + "/dev/repo")))
