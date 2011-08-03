# sbt-protobuf
A plugin for sbt-0.10.x that transforms *.proto files into gazillion-loc java files.

## Usage

### Adding the plugin dependency
In your project, create a file for plugin library dependencies `project/plugins/build.sbt` and add the following lines:

    resolvers += "gseitz@github" at "http://gseitz.github.com/maven/"

    libraryDependencies += "com.github.gseitz" %% "sbt-protobuf" % "0.1" // for sbt-0.10.1

### Importing sbt-protobuf settings
To actually "activate" the plugin, its settings need to be included in the build.

##### build.sbt

    import sbtprotobuf.{ProtobufPlugin=>PB}

    seq(PB.protobufSettings: _*)

##### build.scala

    import sbtprotobuf.{ProtobufPlugin=>PB}

    object MyBuild extends Build {
      lazy val MyProject(
        id = "myproject",
        base = file("."),
        settings = Defaults.defaultSettings ++ PB.protobufSettings ++ Seq( /* custom settings here */ )
      )
    }


### Declaring dependencies
Assuming an artifact contains both `*.proto` files as well as the binaries of the generated `*.java` files, you can specify the dependency like so:

    libraryDependencies += "some.groupID" % "some.artifactID" % "1.0" % ProtobufPlugin.protobufConfig.name // #1

    libraryDependencies += "some.groupID" % "some.artifactID" % "1.0" // #2

Line #1 tells `sbt-protobuf` that the specified artifact contains *.proto files which it needs to extract and add to the `includePath` for `protoc`.

Line #2 adds the artifact to the regular compile:libraryDependencies.

The `*.proto` files of dependencies are extracted and added to the `includePath` parameter for `protoc`, but are not compiled.

### Packaging proto files
`*.proto` files can be included in the jar file by adding the following setting to your build definition:

    unmanagedResourceDirectories in Compile <+= (PB.protoSource in PB.protobufConfig).identity,

### Changing the location of the generated java files
By default, the compiled proto files are created in `<project-dir>/target/<scala-version>/src_managed/main/compiled_protobuf`. Changing the location to `<project-dir>/src/generated` can be done by adding the following setting to your build definition:

    javaSource in PB.protobufConfig <<= (sourceDirectory in Compile)(_ / "generated")

## Scope
All settings and tasks are in the `protobuf` scope. If you want to execute the `generate` task directly, just run `protobuf:generate`.



## Settings

<table>
<tr><th>name</th><th>default</th><th>description</th></tr>
<tr><td>proto-source</td><td><code>"src/main/protobuf"</code></td><td>Path containing *.proto files.</td></tr>
<tr><td>java-source</td><td><code>"$sourceManaged/compiled_protobuf"</code></td><td>Path for the generated *.java files.</td></tr>
<tr><td>version</td><td><code>"2.4.1"</code></td><td>Which version of the protobuf library should be used. A dependency to <code>"com.google.protobuf" % "protobuf-java" % "$version"</code> is automatically added to <code>libraryDependencies</td></tr>
<tr><td>protoc</td><td><code>"protoc"</code></td><td>The path to the 'protoc' executable.</td></tr>
<tr><td>include-paths</td><td><code>Seq($generated-source, external-include-path)</code></td><td>The path for additional *.proto files.</td></tr>
<tr><td>external-include-path</td><td><code>target/protobuf_external</code></td><td>The path to which <code>protobuf:library-dependencies</code> are extracted and which is used as <code>protobuf:include-path</code> for <code>protoc</code></td></tr>
</table>

## Tasks

<table>
<tr><th>name</th><th>description</th></tr>
<tr><td>generate</td><td>Performs the hardcore compiling action and is automatically executed as a "source generator" in the <code>Compile</code> scope.</td></tr>
<tr><td>unpack-dependencies</td><td>Extracts proto files from <code>library-dependencies</code> into <code>external-inlude-patch</code></td></tr>
</table>

## Credits
`sbt-protobuf` is based on [softprops/coffeescripted-sbt](https://github.com/softprops/coffeescripted-sbt) for the sbt-0.10 specific parts and [codahale/protobuf-sbt](https://github.com/codahale/protobuf-sbt) for the protobuf specifics.
