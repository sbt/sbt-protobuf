import sbt.*
import sbt.Keys.*

object TestCompat extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    val packageBinFile = taskKey[File]("")
  }

  import autoImport.*

  override lazy val projectSettings = Seq(Compile, Test).map { c =>
    (c / packageBinFile) := (c / packageBin).value
  }
}
