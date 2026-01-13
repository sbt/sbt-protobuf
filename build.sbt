ThisBuild / organization := "com.github.sbt"

lazy val protobuf = "com.google.protobuf" % "protobuf-java" % "3.25.8" % Runtime // for scala-steward
lazy val grpc = "io.grpc" % "protoc-gen-grpc-java" % "1.62.2" % Runtime // for scala-steward

def scala212 = "2.12.21"

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(nocomma {
    name := "sbt-protobuf"
    libraryDependencies += protobuf
    crossScalaVersions := Seq(scala212, "3.8.0")
    scriptedSbt := {
      scalaBinaryVersion.value match {
        case "2.12" =>
          sbtVersion.value
        case _ =>
          (pluginCrossBuild / sbtVersion).value
      }
    }
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" =>
          "1.5.8"
        case _ =>
          "2.0.0-RC8"
      }
    }
    Compile / sourceGenerators += task {
      val source = s"""package sbtprotobuf
        |
        |private[sbtprotobuf] object SbtProtobufBuildInfo {
        |  def defaultProtobufVersion: String = "${protobuf.revision}"
        |  def defaultGrpcVersion: String = "${grpc.revision}"
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
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xlint")
    scalacOptions ++= {
      scalaBinaryVersion.value match {
        case "2.12" =>
          Seq("-Yno-adapted-args")
        case _ =>
          Nil
      }
    }
    (Compile / doc / scalacOptions) ++= {
      scalaBinaryVersion.value match {
        case "2.12" =>
          val hash = sys.process.Process("git rev-parse HEAD").lineStream_!.head
          Seq(
            "-sourcepath", baseDirectory.value.getAbsolutePath,
            "-doc-source-url", "https://github.com/sbt/sbt-protobuf/blob/" + hash + "€{FILE_PATH}.scala"
          )
        case _ =>
          Nil
      }
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
    TaskKey[Unit]("scriptedTestSbt2") := Def.taskDyn {
      val values = sbtTestDirectory.value
        .listFiles(_.isDirectory)
        .flatMap { dir1 =>
          dir1.listFiles(_.isDirectory).map { dir2 =>
            dir1.getName -> dir2.getName
          }
        }
        .toList
      val log = streams.value.log
      // TODO enable all tests
      val exclude: Set[(String, String)] = Set(
        "basic", "task-scoped"
      ).map("sbt-protobuf" -> _)
      val args = values.filterNot(exclude).map { case (x1, x2) => s"${x1}/${x2}" }
      val arg = args.mkString(" ", " ", "")
      log.info(s"scripted $arg")
      scripted.toTask(arg)
    }.value
    scriptedBufferLog := false
    scriptedLaunchOpts += s"-Dplugin.version=${version.value}"
  })
