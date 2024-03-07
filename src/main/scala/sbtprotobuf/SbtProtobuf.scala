package sbtprotobuf

import java.nio.file.Path
import sbt._
import sbt.librarymanagement.DependencyFilter

object SbtProtobuf {
  val protocArtifactName = "protoc"
  val protocGenGrpcJavaArtifactName = "protoc-gen-grpc-java"

  def protocDependency(v: String): ModuleID =
    ("com.google.protobuf" % protocArtifactName % v)
      .artifacts(Artifact(
        name = protocArtifactName,
        `type` = "exe",
        extension = "exe",
        classifier = detectClassifier,
      ))

  def protocGenGrpcJavaDependency(v: String): ModuleID =
    ("io.grpc" % protocGenGrpcJavaArtifactName % v)
      .artifacts(Artifact(
        name = protocGenGrpcJavaArtifactName,
        `type` = "exe",
        extension = "exe",
        classifier = detectClassifier,
      ))

  def detectClassifier: String = {
    val x86_64 = "x86_64"
    val arch = sys.props.get("os.arch") match {
      case Some("aarch64") => "aarch_64"
      case Some("amd64")   => x86_64
      case Some(arch)      => arch
      case _               => x86_64
    }
    if (scala.util.Properties.isMac){
      s"osx-$arch"
    } else if (scala.util.Properties.isWin){
      s"windows-$arch"
    } else {
      s"linux-$arch"
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
