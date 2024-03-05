// So that publishLocal doesn't continuously create new versions
def versionFmt(out: sbtdynver.GitDescribeOutput): String = {
  val snapshotSuffix =
    if (out.isSnapshot()) "-SNAPSHOT"
    else ""
  out.ref.dropPrefix + snapshotSuffix
}
def fallbackVersion(d: java.util.Date): String = s"HEAD-${sbtdynver.DynVer timestamp d}"
ThisBuild / version := dynverGitDescribeOutput.value.mkVersion(versionFmt, fallbackVersion(dynverCurrentDate.value))
ThisBuild / dynver := {
  val d = new java.util.Date
  sbtdynver.DynVer.getGitDescribeOutput(d).mkVersion(versionFmt, fallbackVersion(d))
}
