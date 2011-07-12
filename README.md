sbt-protobuf
============
A plugin for sbt-0.10 and transforms *.proto files into gazillion-loc java files.

It is based on [softprops/coffeescripted-sbt](https://github.com/softprops/coffeescripted-sbt) for the sbt-0.10 specific parts and [codahale/protobuf-sbt](https://github.com/codahale/protobuf-sbt) for the protobuf specifics.

Usage
-----
In your project, define a file for plugin library dependencies `project/plugins/build.sbt`

And add the following lines

    resolvers += "gseitz@github" at "http://gseitz.github.com/maven/"

    libraryDependencies += "com.github.gseitz" %% "sbt-protobuf" % "0.0.1"


You can specify dependencies that contain `*.proto` files with the `protoLibraryDependencies`/`protobuf:library-dependencies` setting.
The `*.proto` files are extracted and added to the `include-path` parameter for `protoc`, but are not compiled. Additionally, `protoLibraryDependencies` are assumed to also contain the respective compiled java classes and are automatically added to `libraryDependencies`.

Scope
-----
All settings and tasks are in the `protobuf` scope. If you want to execute the `generate` task directly, just run `protobuf:generate`.



Settings
--------

<table>
<tr><th>name</th><th>default</th><th>description</th></tr>
<tr><td>source</td><td>"src/main/protobuf"</td><td>Path containing *.proto files.</td></tr>
<tr><td>generated-source</td><td>"$sourceManaged/compiled_protobuf"</td><td>Path for the generated *.java files.</td></tr>
<tr><td>version</td><td>"2.4.1"</td><td>Which version of the protobuf library should be used. A dependency to <code>"com.google.protobuf" % "protobuf-java" % "$version"</code> is automatically added to <code>libraryDependencies</td></tr>
<tr><td>protoc</td><td>"protoc"</td><td>The path to the 'protoc' executable.</td></tr>
<tr><td>include-path</td><td>"$generated-source"</td><td>The path for additional *.proto files.</td></tr>
<tr><td>library-dependencies</td><td>Nil</td><td>Libraries containing *.proto files.</td></tr>
<tr><td>external-include-path</td><td>Nil</td><td>The path to which protobuf:library-dependencies are extracted and which is used as protobuf:include-path for protoc</td></tr>
</table>

Tasks
-----

<table>
<tr><th>name</th><th>description</th></tr>
<tr><td>generate</td><td>Performs the hardcore compiling action and is automatically executed as a "source generator" in the `Compile` scope.</td></tr>
<tr><td>clean</td><td>Removes everything in $protobuf-target</td></tr>
</table>
