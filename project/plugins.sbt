addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.1")

libraryDependencies <+= sbtVersion(v => "org.scala-sbt" % "scripted-plugin" % v)
