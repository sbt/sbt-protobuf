ThisBuild / organization := "com.github.sbt"

lazy val protocJar = "com.github.os72" % "protoc-jar" % "3.11.4"

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(nocomma {
    name := "sbt-protobuf"

    libraryDependencies += protocJar
    scalacOptions := Seq("-deprecation", "-unchecked", "-Xlint", "-Yno-adapted-args")
    (Compile / doc / scalacOptions) ++= {
      val hash = sys.process.Process("git rev-parse HEAD").lineStream_!.head
      Seq(
        "-sourcepath", baseDirectory.value.getAbsolutePath,
        "-doc-source-url", "https://github.com/sbt/sbt-protobuf/blob/" + hash + "€{FILE_PATH}.scala"
      )
    }
    licenses += (("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")))
    homepage := Some(url("https://github.com/sbt/sbt-protobuf"))
    pomExtra := {
      <developers>{
        Seq(
          ("xuwei-k", "Kenji Yoshida"),
          ("eed3si9n", "Eugene Yokota"),
        ).map { case (id, name) =>
          <developer>
            <id>{id}</id>
            <name>{name}</name>
            <url>https://github.com/{id}</url>
          </developer>
        }
      }</developers>
    }
    scriptedBufferLog := false
    scriptedLaunchOpts += s"-Dplugin.version=${version.value}"

    // Don't update to 1.3.0 https://github.com/sbt/sbt/issues/5049
    crossSbtVersions := Seq("0.13.18", "1.2.8")
  })
