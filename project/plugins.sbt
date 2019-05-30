addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.5")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.11")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
