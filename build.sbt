sbtPlugin := true

organization := "com.github.gseitz"

name := "sbt-protobuf"

version := "0.3.2"

scalacOptions := Seq("-deprecation", "-unchecked")

publishTo <<= (version) { version: String =>
   val scalasbt = "http://scalasbt.artifactoryonline.com/scalasbt/"
   val (name, url) = if (version.contains("-SNAPSHOT"))
                       ("sbt-plugin-snapshots-publish", scalasbt+"sbt-plugin-snapshots")
                     else
                       ("sbt-plugin-releases-publish", scalasbt+"sbt-plugin-releases")
   Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

publishMavenStyle := false

crossBuildingSettings

CrossBuilding.crossSbtVersions := Seq("0.11.3", "0.12", "0.13")

ScriptedPlugin.scriptedSettings

//Remember to comment this out
scriptedBufferLog := false

scriptedLaunchOpts <+= version( x => s"-Dplugin.version=${x}" )
