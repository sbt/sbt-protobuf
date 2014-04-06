externalResolvers := Seq(Resolver.defaultLocal)

libraryDependencies += "com.google.protobuf" % "protobuf-java" % "2.5.0"

{
  val pluginVersion = System.getProperty("plugin.version")
  if(pluginVersion == null)
    throw new RuntimeException("""|The system property 'plugin.version' is not defined.
                                  |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  else addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % pluginVersion)
}
