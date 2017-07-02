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

unmanagedSourceDirectories in Compile ++= {
  if((sbtBinaryVersion in pluginCrossBuild).value.startsWith("1.0.")) {
    ((scalaSource in Compile).value.getParentFile / "scala-sbt-1.0") :: Nil
  } else {
    Nil
  }
}

crossSbtVersions := Seq("0.13.15", "1.0.0-M6")

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

// https://github.com/sbt/sbt/issues/3245
ScriptedPlugin.scripted := {
  val args = ScriptedPlugin.asInstanceOf[{
    def scriptedParser(f: File): complete.Parser[Seq[String]]
  }].scriptedParser(sbtTestDirectory.value).parsed
  val prereq: Unit = scriptedDependencies.value
  try {
    if((sbtVersion in pluginCrossBuild).value == "1.0.0-M6") {
      ScriptedPlugin.scriptedTests.value.asInstanceOf[{
        def run(x1: File, x2: Boolean, x3: Array[String], x4: File, x5: Array[String], x6: java.util.List[File]): Unit
      }].run(
        sbtTestDirectory.value,
        scriptedBufferLog.value,
        args.toArray,
        sbtLauncher.value,
        scriptedLaunchOpts.value.toArray,
        new java.util.ArrayList()
      )
    } else {
      ScriptedPlugin.scriptedTests.value.asInstanceOf[{
        def run(x1: File, x2: Boolean, x3: Array[String], x4: File, x5: Array[String]): Unit
      }].run(
        sbtTestDirectory.value,
        scriptedBufferLog.value,
        args.toArray,
        sbtLauncher.value,
        scriptedLaunchOpts.value.toArray
      )
    }
  } catch { case e: java.lang.reflect.InvocationTargetException => throw e.getCause }
}
