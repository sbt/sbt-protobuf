ThisBuild / organization := "com.github.sbt"

lazy val protocJar = "com.github.os72" % "protoc-jar" % "3.11.4"
lazy val protobuf = "com.google.protobuf" % "protobuf-java" % "3.21.3" % "runtime" // for scala-steward

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(nocomma {
    name := "sbt-protobuf"
    libraryDependencies += protobuf
    Compile / sourceGenerators += task {
      val source = s"""package sbtprotobuf
        |
        |private[sbtprotobuf] object SbtProtobufBuildInfo {
        |  def defaultProtobufVersion: String = "${protobuf.revision}"
        |}
        |""".stripMargin
      val f = (Compile / sourceManaged).value / "sbtprotobuf" / "SbtProtobufBuildInfo.scala"
      IO.write(f, source)
      Seq(f)
    }
    pomPostProcess := { node =>
      import scala.xml.{NodeSeq, Node}
      val rule = new scala.xml.transform.RewriteRule {
        override def transform(n: Node) = {
          if (List(
            n.label == "dependency",
            (n \ "groupId").text == protobuf.organization,
            (n \ "artifactId").text == protobuf.name,
          ).forall(identity)) {
            NodeSeq.Empty
          } else {
            n
          }
        }
      }
      new scala.xml.transform.RuleTransformer(rule).transform(node)(0)
    }
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
    crossSbtVersions := Seq("1.2.8")
  })
