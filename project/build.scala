package sbtprotobuf

import sbt._
import Keys._

object build extends Build{

  lazy val standardSettings: Seq[Setting[_]] = Defaults.defaultSettings ++ Seq[Setting[_]](
    organization := "com.github.gseitz.sbt-protobuf",
    version := "0.3-SNAPSHOT"
  )

  lazy val buildSettings: Seq[Setting[_]] = standardSettings ++ Seq[Setting[_]](
    scalacOptions := Seq("-deprecation", "-unchecked"),
    sbtPlugin := true,
    publishTo := Some(Resolver.file("gseitz@github", file(Path.userHome + "/dev/repo")))
  )

  lazy val root = Project(
    id = "root",
    base = file("."),
    aggregate = Seq(protocPlugin, scalaBuffPlugin)
  )

  lazy val protocPlugin: Project = Project(
    id = "protoc",
    base = file("protoc"),
    settings = buildSettings
  )

  lazy val scalaBuffPlugin: Project = Project(
    id = "scalabuff",
    base = file("scalabuff"),
    settings = buildSettings ++ Seq[Setting[_]](
      resolvers += "gseitz @ github" at "http://gseitz.github.com/maven/",
      libraryDependencies ++= Seq(
        "scalabuff" %% "scalabuff" % "0.9"
      )
    ),
    dependencies = Seq(protocPlugin)
  )
}
