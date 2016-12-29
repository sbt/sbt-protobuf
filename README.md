# sbt-protobuf
A plugin for sbt that transforms *.proto files into gazillion-loc Java source files, and potentially to other languages too.

[![Build Status](https://travis-ci.org/sbt/sbt-protobuf.svg?branch=master)](https://travis-ci.org/sbt/sbt-protobuf)
[ ![Download](https://api.bintray.com/packages/sbt/sbt-plugin-releases/sbt-protobuf/images/download.svg) ](https://bintray.com/sbt/sbt-plugin-releases/sbt-protobuf/_latestVersion)

## Usage

### Adding the plugin dependency
In your project, create a file for plugin library dependencies `project/plugins.sbt` and add the following lines:

```scala
addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.5.4")
```

The dependency to `"com.google.protobuf" % "protobuf-java"` is automatically added to the `Compile` scope.
The version for `protobuf-java` can be controlled by the setting `version in protobufConfig` (set to `3.1.0` by default).

### Importing sbt-protobuf settings
To actually "activate" the plugin, its settings need to be included in the build.

##### build.sbt

```scala
import sbtprotobuf.{ProtobufPlugin=>PB}

PB.protobufSettings
```

##### build.scala

```scala
import sbt._

import sbtprotobuf.{ProtobufPlugin=>PB}

object MyBuild extends Build {
  lazy val myproject = Project(
    id = "myproject",
    base = file(".")
  ).settings(
    PB.protobufSettings : _*
  ).settings(
    /* custom settings here */
  )
}
```


### Declaring dependencies
Assuming an artifact contains both `*.proto` files as well as the binaries of the generated `*.java` files, you can specify the dependency like so:

```scala
libraryDependencies += "some.groupID" % "some.artifactID" % "1.0" % PB.protobufConfig.name // #1

libraryDependencies += "some.groupID" % "some.artifactID" % "1.0" // #2
```

Line #1 tells `sbt-protobuf` that the specified artifact contains *.proto files which it needs to extract and add to the `--proto_path` for `protoc`.
Internally the setting `externalIncludePath` is used to track 3rd party proto files.

Line #2 adds the artifact to the regular compile classpath.

The `*.proto` files of dependencies are extracted and added to the `--proto_path` parameter for `protoc`, but are not compiled.

### Compiling external proto files
Sometimes it's desirable to compile external proto files (eg. because the library is compiled with an older version of `protoc`).
This can be achieved by adding the following setting:

```scala
sourceDirectories in PB.protobufConfig += (externalIncludePath in PB.protobufConfig).value
```

### Packaging proto files
`*.proto` files can be included in the jar file by adding the following setting to your build definition:

```scala
unmanagedResourceDirectories in Compile += (sourceDirectory in PB.protobufConfig).value
```

### Changing the location of the generated java files
By default, the compiled proto files are created in `<project-dir>/target/<scala-version>/src_managed/main/compiled_protobuf`. Changing the location to `<project-dir>/src/generated` can be done by adding the following setting to your build definition:

```scala
javaSource in PB.protobufConfig := ((sourceDirectory in Compile).value / "generated")
```

**WARNING:** The content of this directory is **removed** by the `clean` task. Don't set it to a directory containing files you hold dear to your heart.

###Note

1,If you occurred compile error,as ```[...] is already defined as object [...]``` you could change the compile order
as ```compileOrder := CompileOrder.JavaThenScala```,the default is ```mixed```.

2,The inner message's name could not be the ```.proto```'s file name.that will cause problem too,you could change the inner message's name or the ```.proto``` file name or add the ```option java_outer_classname = "NewNameNotAsTheFileName";``` to you ```.proto``` file.

### Additional options to protoc
All options passed to `protoc` are configured via the `protobufProtocOptions`. To add options, for example to run a custom plugin, add them to this setting key. For example:

```scala
protocOptions in PB.protobufConfig ++= Seq("--custom-option")
```

### Additional target directories
The source directories where the files are generated, and the globs used to identify the generated files, are configured by `generatedTargets in PB.protobufConfig`.
In case only Java files are generated, this setting doesn't need to change, since it automatically inherits the value of `javaSource in PB.protobufConfig`, paired with the glob `*.java`.
In case other types of source files are generated, for example by using a custom plugin (see previous section), the corresponding target directories and source file globs must be configured by adding them to this setting. For example:

```scala
generatedTargets in PB.protobufConfig ++= {
  Seq((sourceDirectory in Compile).value / "generated" / "scala", "*.scala")
}
```

This plugin uses the `generatedTargets` setting to:
- add the generated source directories to `cleanFiles` and `managedSourceDirectories`
- collect the generated files after running `protoc` and return them to SBT for the compilation phase

## Scope
All settings and tasks are in the `protobuf` scope. If you want to execute the `protobufGenerate` task directly, just run `protobuf:protobufGenerate`.



## Settings

<table>
<tr><th>name</th><th>name in shell</th><th>built-in key</th><th>default</th><th>description</th></tr>
<tr>
    <td>sourceDirectory</td>
    <td>sourceDirectory</td>
    <td>x</td>
    <td><code>"src/main/protobuf"</code></td>
    <td>Path containing *.proto files.</td>
</tr>
<tr>
    <td>sourceDirectories</td>
    <td>sourceDirectories</td>
    <td>x</td>
    <td><code>sourceDirectory</code></td>
    <td>This setting is used to collect all directories containing *.proto files to compile</td>
</tr>
<tr>
    <td>javaSource</td>
    <td>javaSource</td>
    <td>x</td>
    <td><code>"$sourceManaged/compiled_protobuf"</code></td>
    <td>Path for the generated *.java files.</td>
</tr>
<tr>
    <td>version</td>
    <td>version</td>
    <td>x</td>
    <td><code>"3.1.0"</code></td>
    <td>Which version of the protobuf library should be used. A dependency to <code>"com.google.protobuf" % "protobuf-java" % "$version"</code> is automatically added to <code>libraryDependencies</td>
</tr>
<tr>
    <td>protoc</td>
    <td>protobufProtoc</td>
    <td></td>
    <td><code>"protoc"</code></td><td>The path to the 'protoc' executable.</td>
</tr>
<tr>
    <td>includePaths</td>
    <td>protobufIncludePaths</td>
    <td></td>
    <td><code>Seq($generated-source, protobufExternalIncludePath)</code></td><td>The path for additional *.proto files.</td>
</tr>
<tr>
    <td>externalIncludePath</td>
    <td>protobufExternalIncludePath</td>
    <td></td>
    <td><code>target/protobuf_external</code></td><td>The path to which <code>protobuf:libraryDependencies</code> are extracted and which is used as <code>protobuf:protobufIncludePath</code> for <code>protoc</code></td>
</tr>
<tr>
    <td>protocOptions</td>
    <td>protobufProtocOptions</td>
    <td></td>
    <td><code>--java_out=</code>[java generated source directory from <code>generatedTargets</code>]</td>
    <td>the list of options passed to the <code>protoc</code> binary</td>
</tr>
<tr>
    <td>generatedTargets</td>
    <td>protobufGeneratedTargets</td>
    <td></td>
    <td><code>(file(</code>java source directory based on <code>javaSource in PB.protobufConfig</code>), <code>"*.java")</code></td>
    <td>the list of target directories and source file globs for the generated files</td>
</tr>
</table>

## Tasks

<table>
<tr><th>name</th><th>shell-name</th><th>description</th></tr>
<tr><td>generate</td><td>protobufGenerate</td><td>Performs the hardcore compiling action and is automatically executed as a "source generator" in the <code>Compile</code> scope.</td></tr>
<tr><td>unpackDependencies</td><td>protobufUnpackDependencies</td><td>Extracts proto files from <code>libraryDependencies</code> into <code>protobufExternalInludePatch</code></td></tr>
<tr>
    <td>runProtoc</td>
    <td>protobufRunProtoc</td>
    <td>A function that executes the protobuf compiler with the given arguments,
    returning the exit code. The default implementation runs the executable referenced by the `protoc` setting.</td>
</tr>

</table>

## Credits
`sbt-protobuf` is based on [softprops/coffeescripted-sbt](https://github.com/softprops/coffeescripted-sbt) for the sbt-0.10 specific parts and [codahale/protobuf-sbt](https://github.com/codahale/protobuf-sbt) for the protobuf specifics.
