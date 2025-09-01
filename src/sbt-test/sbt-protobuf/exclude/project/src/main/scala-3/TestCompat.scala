import sbt.*
import sbt.Keys.*

object TestCompat extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    @transient
    val packageBinFile = taskKey[File]("")
  }

  import autoImport.*

  override lazy val projectSettings = Seq(Compile, Test).map { c =>
    (c / packageBinFile) := fileConverter.value.toPath((c / packageBin).value).toFile
  }
}
