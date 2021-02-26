import sbtrelease.ReleaseStateTransformations._

ThisBuild / organization := "com.github.gseitz"

lazy val protocJar = "com.github.os72" % "protoc-jar" % "3.11.4"

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(nocomma {
    name := "sbt-protobuf"

    libraryDependencies += protocJar
    scalacOptions := Seq("-deprecation", "-unchecked", "-Xlint", "-Yno-adapted-args")
    (Compile / doc / scalacOptions) ++= {
      val tagOrBranch = if(isSnapshot.value) {
        sys.process.Process("git rev-parse HEAD").lineStream_!.head
      } else {
        "v" + version.value
      }
      Seq(
        "-sourcepath", baseDirectory.value.getAbsolutePath,
        "-doc-source-url", "https://github.com/sbt/sbt-protobuf/blob/" + tagOrBranch + "€{FILE_PATH}.scala"
      )
    }
    publishMavenStyle := false
    bintrayOrganization := Some("sbt")
    bintrayRepository := "sbt-plugin-releases"
    bintrayPackage := "sbt-protobuf"
    bintrayReleaseOnPublish := false
    scriptedBufferLog := false
    scriptedLaunchOpts += s"-Dplugin.version=${version.value}"

    // Don't update to 1.3.0 https://github.com/sbt/sbt/issues/5049
    crossSbtVersions := Seq("0.13.18", "1.2.8")
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      releaseStepCommandAndRemaining("^ test"),
      releaseStepCommandAndRemaining("^ scripted"),
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("^ publish"),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  })
