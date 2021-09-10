# sbt-protobuf
A plugin for sbt that transforms `*.proto` files into gazillion-loc Java source files, and potentially to other languages too.


## Usage

### Adding the plugin dependency
In your project, create a file for plugin library dependencies `project/plugins.sbt` and add the following line:

```scala
addSbtPlugin("com.github.sbt" % "sbt-protobuf" % "0.7.0")
```

The dependency to `"com.google.protobuf" % "protobuf-java"` is automatically added to the `Compile` scope.
The version for `protobuf-java` can be controlled by the setting `version in ProtobufConfig` (set to `3.9.0` by default).

### Importing sbt-protobuf settings
To actually "activate" the plugin, its settings need to be included in the build.

##### build.sbt

```scala
enablePlugins(ProtobufPlugin)
```


### Declaring dependencies
#### Artifacts containing both `*.proto` files and java binaries
Assuming an artifact contains both `*.proto` files as well as the binaries of the generated `*.java` files, you can specify the dependency like so:

```scala
libraryDependencies += "some.groupID" % "some.artifactID" % "1.0" % ProtobufConfig.name // #1

libraryDependencies += "some.groupID" % "some.artifactID" % "1.0" // #2
```

Line #1 tells `sbt-protobuf` that the specified artifact contains *.proto files which it needs to extract and add to the `--proto_path` for `protoc`.
Internally the setting `protobufExternalIncludePath` is used to track 3rd party proto files.

Line #2 adds the artifact to the regular compile classpath.

The `*.proto` files of dependencies are extracted and added to the `--proto_path` parameter for `protoc`, but are not compiled.

#### Artifacts in the `protobuf` configuration containing only `*.proto` files
You can specify a dependency on an artifact that contains only `.proto` files in the `protobuf` configuration with a `proto` classifier like so:
```
libraryDependencies += ("some.groupID" % "some.artifactID" % "1.0" classifier protoClassifier) % s"${ProtobufConfig.name}->${ProtobufConfig.name}"
```

### Compiling external proto files
Sometimes it's desirable to compile external proto files (eg. because the library is compiled with an older version of `protoc`).
This can be achieved by adding the following setting:

```scala
ProtobufConfig / sourceDirectories += (ProtobufConfig / protobufExternalIncludePath).value
```

### Packaging proto files
`*.proto` files can be included in the jar file by adding the following setting to your build definition:

```scala
Compile / unmanagedResourceDirectories += (ProtobufConfig / sourceDirectory).value
```

Alternatively, `*.proto` files can be packaged in a separate jar file in the `protobuf` configuration with a `proto` classifier:

```scala
addArtifact(artifact in (ProtobufConfig, protobufPackage), ProtobufConfig / protobufPackage)
```

### Changing the location of the generated java files
By default, the compiled proto files are created in `<project-dir>/target/<scala-version>/src_managed/main/compiled_protobuf`. Changing the location to `<project-dir>/src/generated` can be done by adding the following setting to your build definition:

```scala
ProtobufConfig / javaSource := ((Compile / sourceDirectory).value / "generated")
```

**WARNING:** The content of this directory is **removed** by the `clean` task. Don't set it to a directory containing files you hold dear to your heart.

### Note

1. If you encounter compile errors, as ```[...] is already defined as object [...]``` you could change the compile order
as ```compileOrder := CompileOrder.JavaThenScala```,the default is ```mixed```.

2. The inner message's name could not be the ```.proto```'s file name.that will cause problem too, you could change the inner message's name or the ```.proto``` file name or add the ```option java_outer_classname = "NewNameNotAsTheFileName";``` to you ```.proto``` file.

### Additional options to protoc
All options passed to `protoc` are configured via the `protobufProtocOptions`. To add options, for example to run a custom plugin, add them to this setting key. For example:

```scala
ProtobufConfig / protobufProtocOptions ++= Seq("--custom-option")
```

### Additional target directories
The source directories where the files are generated, and the globs used to identify the generated files, are configured by `protobufGeneratedTargets in ProtobufConfig`.
In case only Java files are generated, this setting doesn't need to change, since it automatically inherits the value of `javaSource in ProtobufConfig`, paired with the glob `*.java`.
In case other types of source files are generated, for example by using a custom plugin (see previous section), the corresponding target directories and source file globs must be configured by adding them to this setting. For example:

```scala
ProtobufConfig / protobufGeneratedTargets ++= {
  Seq(((Compile / sourceDirectory).value / "generated" / "scala", "*.scala"))
}
```

This plugin uses the `protobufGeneratedTargets` setting to:
- add the generated source directories to `cleanFiles` and `managedSourceDirectories`
- collect the generated files after running `protoc` and return them to SBT for the compilation phase

## Scope
All settings and tasks are in the `protobuf` scope. If you want to execute the `protobufGenerate` task directly, just run `protobuf:protobufGenerate`.



## Settings

<table>
<tr><th>name</th><th>built-in key</th><th>default</th><th>description</th></tr>
<tr>
    <td>sourceDirectory</td>
    <td>x</td>
    <td><code>"src/main/protobuf"</code></td>
    <td>Path containing <code>*.proto</code> files.</td>
</tr>
<tr>
    <td>sourceDirectories</td>
    <td>x</td>
    <td><code>sourceDirectory</code></td>
    <td>This setting is used to collect all directories containing <code>*.proto</code> files to compile</td>
</tr>
<tr>
    <td>javaSource</td>
    <td>x</td>
    <td><code>"$sourceManaged/compiled_protobuf"</code></td>
    <td>Path for the generated <code>*.java</code> files.</td>
</tr>
<tr>
    <td>version</td>
    <td>x</td>
    <td><code>"3.9.0"</code></td>
    <td>Which version of the protobuf library should be used. A dependency to <code>"com.google.protobuf" % "protobuf-java" % "$version"</code> is automatically added to <code>libraryDependencies</td>
</tr>
<tr>
    <td>protobufProtoc</td>
    <td></td>
    <td><code>"protoc"</code></td>
    <td>The path to the 'protoc' executable.</td>
</tr>
<tr>
    <td>protobufIncludePaths</td>
    <td></td>
    <td><code>Seq($generated-source, protobufExternalIncludePath)</code></td>
    <td>The path for additional <code>*.proto</code> files.</td>
</tr>
<tr>
    <td>protobufExternalIncludePath</td>
    <td></td>
    <td><code>target/protobuf_external</code></td>
    <td>The path to which <code>protobuf:libraryDependencies</code> are extracted and which is used as <code>protobuf:protobufIncludePath</code> for <code>protoc</code></td>
</tr>
<tr>
    <td>protobufProtocOptions</td>
    <td></td>
    <td><code>--java_out=</code>[java generated source directory from <code>protobufGeneratedTargets</code>]</td>
    <td>the list of options passed to the <code>protoc</code> binary</td>
</tr>
<tr>
    <td>protobufGeneratedTargets</td>
    <td></td>
    <td><code>(file(</code>java source directory based on <code>ProtobufConfig / javaSource</code>), <code>"*.java")</code></td>
    <td>the list of target directories and source file globs for the generated files</td>
</tr>
</table>

## Tasks

<table>
<tr>
  <th>name</th>
  <th>description</th>
</tr>
<tr>
  <td>protobufGenerate</td>
  <td>Performs the hardcore compiling action and is automatically executed as a "source generator" in the <code>Compile</code> scope.</td>
</tr>
<tr>
  <td>protobufUnpackDependencies</td>
  <td>Extracts proto files from <code>libraryDependencies</code> into <code>protobufExternalInludePatch</code></td>
</tr>
<tr>
  <td>protobufRunProtoc</td>
  <td>A function that executes the protobuf compiler with the given arguments,
    returning the exit code. The default implementation runs the executable referenced by the <code>protoc</code> setting.</td>
</tr>
<tr>
  <td>protobufPackage</td>
  <td>Produces a jar artifact containing only <code>*.proto</code> files, with a <code>proto</code> classifier</td>
</tr>

</table>

## IntelliJ IDEA BSP bug
IntelliJ has a [bug](https://youtrack.jetbrains.com/issue/SCL-19517) where it only recognizes generated sources if there is at least one Scala class in the same package - otherwise you'll see red squiggles. As a workaround, you can configure your project to add a private empty class, e.g. like this:
```scala
Compile / sourceGenerators += Def.task {
  // adapt this for your build:
  val protoPackage = "org.example.proto.foo"

  val scalaFile = (Compile/sourceManaged).value / "_ONLY_FOR_INTELLIJ.scala"
  
  IO.write(scalaFile,
    s"""package $protoPackage
      |
      |private class _ONLY_FOR_INTELLIJ
      |""".stripMargin)

  Seq(scalaFile)
}.taskValue
```

## Credits
`sbt-protobuf` is based on [softprops/coffeescripted-sbt](https://github.com/softprops/coffeescripted-sbt) for the sbt-0.10 specific parts and [codahale/protobuf-sbt](https://github.com/codahale/protobuf-sbt) for the protobuf specifics.
