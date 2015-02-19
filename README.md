# sbt-protobuf
A plugin for sbt-0.(12|13).x that transforms *.proto files into gazillion-loc Java source files, and potentially to other languages too.

## Usage

### Adding the plugin dependency
In your project, create a file for plugin library dependencies `project/plugins.sbt` and add the following lines:

    addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.3.3")

The dependency to `"com.google.protobuf" % "protobuf-java"` is automatically added to the `Compile` scope.
The version for `protobuf-java` can be controlled by the setting `version in protobufConfig` (set to `2.5.0` by default).

### Importing sbt-protobuf settings
To actually "activate" the plugin, its settings need to be included in the build.

##### build.sbt

    import sbtprotobuf.{ProtobufPlugin=>PB}

    Seq(PB.protobufSettings: _*)

##### build.scala
    import sbt._

    import sbtprotobuf.{ProtobufPlugin=>PB}

    object MyBuild extends Build {
      lazy val myproject = MyProject(
        id = "myproject",
        base = file("."),
        settings = Defaults.defaultSettings ++ PB.protobufSettings ++ Seq(
            /* custom settings here */
        )
      )
    }


### Declaring dependencies
Assuming an artifact contains both `*.proto` files as well as the binaries of the generated `*.java` files, you can specify the dependency like so:

    libraryDependencies += "some.groupID" % "some.artifactID" % "1.0" % PB.protobufConfig.name // #1

    libraryDependencies += "some.groupID" % "some.artifactID" % "1.0" // #2

Line #1 tells `sbt-protobuf` that the specified artifact contains *.proto files which it needs to extract and add to the `--proto_path` for `protoc`.
Internally the setting `externalIncludePath` is used to track 3rd party proto files.

Line #2 adds the artifact to the regular compile classpath.

The `*.proto` files of dependencies are extracted and added to the `--proto_path` parameter for `protoc`, but are not compiled.

### Compiling external proto files
Sometimes it's desirable to compile external proto files (eg. because the library is compiled with an older version of `protoc`).
This can be achieved by adding the following setting:
 
    sourceDirectories in PB.protobufConfig <+= (externalIncludePath in PB.protobufConfig).identity
    

### Packaging proto files
`*.proto` files can be included in the jar file by adding the following setting to your build definition:

    unmanagedResourceDirectories in Compile <+= (sourceDirectory in PB.protobufConfig).identity,

### Changing the location of the generated java files
By default, the compiled proto files are created in `<project-dir>/target/<scala-version>/src_managed/main/compiled_protobuf`. Changing the location to `<project-dir>/src/generated` can be done by adding the following setting to your build definition:

    javaSource in PB.protobufConfig <<= (sourceDirectory in Compile)(_ / "generated")

**WARNING:** The content of this directory is **removed** by the `clean` task. Don't set it to a directory containing files you hold dear to your heart.

### Additional options to protoc
All options passed to `protoc` are configured via the `protobuf-protoc-options`. To add options, for example to run a custom plugin, add them to this setting key. For example:

    protocOptions in PB.protobufConfig :+= Seq("--custom-option")
    
### Additional target directories
The source directories where the files are generated, and the globs used to identify the generated files, are configured by `generatedTargets in PB.protobufConfig`.
In case only Java files are generated, this setting doesn't need to change, since it automatically inherits the value of `javaSource in PB.protobufConfig`, paired with the glob `*.java`.
In case other types of source files are generated, for example by using a custom plugin (see previous section), the corresponding target directories and source file globs must be configured by adding them to this setting. For example:

    generatedTargets in PB.protobufConfig <++= (sourceDirectory in Compile){ dir =>
        Seq((dir / "generated" / "scala", "*.scala"))
    }
    
This plugin uses the `generatedTargets` setting to:
- add the generated source directories to `cleanFiles` and `managedSourceDirectories`
- collect the generated files after running `protoc` and return them to SBT for the compilation phase

## Scope
All settings and tasks are in the `protobuf` task. If you want to execute the `protobuf-generate` task directly, just run `protobufTask::protobufGenerate`.

Now plugin support `Compile` and `Test` configurations. You can add only Test/Compile settings like this:

    settings = Defaults.defaultSettings ++ PB.protobufSettingsIn(Test) ++ Seq(
             /* custom settings here */
    )

To use the .proto files from `Compile` scope in `Test`:

    .settings( //allows sbt-protobuf to use .proto files from compile in test scope
        includePaths in (Test, protobufTask) <+= (sourceDirectory in (Compile, protobufTask)) map identity
    )


## Settings

<table>
<tr><th>name</th><th>name in shell</th><th>built-in key</th><th>default</th><th>description</th></tr>
<tr>
    <td>sourceDirectory</td>
    <td>source-directory</td>
    <td>x</td>
    <td><code>"src/main/protobuf"</code></td>
    <td>Path containing *.proto files.</td>
</tr>
<tr>
    <td>sourceDirectories</td>
    <td>source-directories</td>
    <td>x</td>
    <td><code>sourceDirectory</code></td>
    <td>This setting is used to collect all directories containing *.proto files to compile<td>
</tr>
<tr>
    <td>javaSource</td>
    <td>java-source</td>
    <td>x</td>
    <td><code>"$sourceManaged/compiled_protobuf"</code></td>
    <td>Path for the generated *.java files.</td>
</tr>
<tr>
    <td>version</td>
    <td>version</td>
    <td>x</td>
    <td><code>"2.5.0"</code></td>
    <td>Which version of the protobuf library should be used. A dependency to <code>"com.google.protobuf" % "protobuf-java" % "$version"</code> is automatically added to <code>libraryDependencies</td>
</tr>
<tr>
    <td>protoc</td>
    <td>protobuf-protoc</td>
    <td></td>
    <td><code>"protoc"</code></td><td>The path to the 'protoc' executable.</td>
</tr>
<tr>
    <td>includePaths</td>
    <td>protobuf-include-paths</td>
    <td></td>
    <td><code>Seq($generated-source, protobuf-external-include-path)</code></td><td>The path for additional *.proto files.</td>
</tr>
<tr>
    <td>externalIncludePath</td>
    <td>protobuf-external-include-path</td>
    <td></td>
    <td><code>target/protobuf_external</code></td><td>The path to which <code>protobuf:library-dependencies</code> are extracted and which is used as <code>protobuf:protobuf-include-path</code> for <code>protoc</code></td>
</tr>
<tr>
    <td>protocOptions</td>
    <td>protobuf-protoc-options</td>
    <td></td>
    <td><code>--java_out=</code>[java generated source directory from <code>generatedTargets</code>]</td>
    <td>the list of options passed to the <code>protoc</code> binary</td>
</tr>
<tr>
    <td>generatedTargets</td>
    <td>protobuf-generated-targets</td>
    <td></td>
    <td><code>(file(</code>java source directory based on <code>javaSource in PB.protobufConfig</code>), <code>"*.java")</code></td>
    <td>the list of target directories and source file globs for the generated files</td>
</tr>
</table>

## Tasks

<table>
<tr><th>name</th><th>shell-name</th><th>description</th></tr>
<tr><td>generate</td><td>protobuf-generate</td><td>Performs the hardcore compiling action and is automatically executed as a "source generator" in the <code>Compile</code> scope.</td></tr>
<tr><td>unpackDependencies</td><td>protobuf-unpack-dependencies</td><td>Extracts proto files from <code>library-dependencies</code> into <code>protobuf-external-inlude-patch</code></td></tr>
</table>

## Credits
`sbt-protobuf` is based on [softprops/coffeescripted-sbt](https://github.com/softprops/coffeescripted-sbt) for the sbt-0.10 specific parts and [codahale/protobuf-sbt](https://github.com/codahale/protobuf-sbt) for the protobuf specifics.
