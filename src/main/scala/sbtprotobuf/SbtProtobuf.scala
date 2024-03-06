package sbtprotobuf

import java.nio.file.Path
import sbt._
import sbt.librarymanagement.DependencyFilter

object SbtProtobuf {
  val protocArtifactName = "protoc"

  def protocDependency(v: String): ModuleID =
    ("com.google.protobuf" % "protoc" % v)
      .artifacts(Artifact(
        name = protocArtifactName,
        `type` = "exe",
        extension = "exe",
        classifier = detectClassifier,
      ))

  def detectClassifier: String = {
    if (scala.util.Properties.isMac){
      "osx-x86_64"
    } else if (scala.util.Properties.isWin){
      "windows-x86_64"
    } else {
      "linux-x86_64"
    }
  }

  /**
   * Set the executable flag to true, and cache it.
   */
  def withFileCache(cacheDirectory: Path)(f: () => Path): Path = {
    val g = FileFunction.cached(
      cacheDirectory.toFile() / "cache",
      inStyle = FilesInfo.hash,
      outStyle = FilesInfo.exists,
    ) { (in: Set[File]) =>
      val out = cacheDirectory.toFile() / in.head.getName
      IO.copyFile(in.head, out)
      out.setExecutable(true)
      Set(out)
    }
    g(Set(f().toFile())).head.toPath()
  }

  /**
   * Extract a file with given name from the given UpdateReport.
   */
  def extractFile(ur: UpdateReport, artifactName: String): Path = {
    val df: DependencyFilter =
      configurationFilter(name = "protobuf-tool") &&
      artifactFilter(name = artifactName)
    ur.matching(df) match {
      case Vector(x) => x.toPath()
      case xs        => sys.error(s"$artifactName was not found: $xs")
    }
  }
}
