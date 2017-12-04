import sbtrelease.ReleaseStateTransformations._

organization := "com.github.gseitz"

name := "sbt-protobuf"

scalacOptions := Seq("-deprecation", "-unchecked", "-Xlint", "-Yno-adapted-args")

scalacOptions in (Compile, doc) ++= {
  val tagOrBranch = if(isSnapshot.value) {
    sys.process.Process("git rev-parse HEAD").lines_!.head
  } else {
    "v" + version.value
  }
  Seq(
    "-sourcepath", baseDirectory.value.getAbsolutePath,
    "-doc-source-url", "https://github.com/sbt/sbt-protobuf/blob/" + tagOrBranch + "€{FILE_PATH}.scala"
  )
}

sbtPlugin := true

publishMavenStyle := false

bintrayOrganization := Some("sbt")

bintrayRepository := "sbt-plugin-releases"

bintrayPackage := "sbt-protobuf"

bintrayReleaseOnPublish := false

ScriptedPlugin.scriptedSettings

scriptedBufferLog := false

scriptedLaunchOpts += s"-Dplugin.version=${version.value}"
scriptedLaunchOpts += s"-Dprotoc-jar.version=3.5.0"

crossSbtVersions := Seq("0.13.16", "1.0.4")

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
