
addSbtPlugin("net.virtual-void" % "sbt-cross-building" % "0.8.1")

libraryDependencies <+= sbtVersion(v => "org.scala-sbt" % "scripted-plugin" % v)
