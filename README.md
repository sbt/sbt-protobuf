sbt-protobuf
============
A plugin for sbt-0.10 and transforms *.proto files into gazillion-loc java files.

It is based on [softprops/coffeescripted-sbt](https://github.com/softprops/coffeescripted-sbt) for the sbt-0.10 specific parts and [codahale/protobuf-sbt](https://github.com/codahale/protobuf-sbt) for the protobuf specifics.

Scope
-----
All settings and tasks are scoped with protobuf. If you want to execute the `generate` task directly, just run `protobuf:generate`.



Settings
--------

<table>
<tr><th>name</th><th>default</th><th>description</th></tr>
<tr><td>source</td><td>"src/main/protobuf"</td><td>Path containing *.proto files.</td></tr>
<tr><td>generated-source</td><td>"$sourceManaged/compiled_protobuf"</td><td>Path for the generated *.java files.</td></tr>
<tr><td>version</td><td>"2.4.1"</td><td>Which version of the protobuf library should be used. A dependency to <code>"com.google.protobuf" % "protobuf-java" % "$version"</code> is automatically added to <code>libraryDependencies</td></tr>
<tr><td>protoc</td><td>"protoc"</td><td>The path to the 'protoc' executable.</td></tr>
<tr><td>include-path</td><td>"$generated-source"</td><td>The path for additional *.proto files.</td></tr>
</table>

Tasks
-----

<table>
<tr><th>name</th><th>description</th></tr>
<tr><td>generate</td><td>Performs the hardcore compiling action and is automatically executed as a "source generator" in the `Compile` scope.</td></tr>
<tr><td>clean</td><td>Removes everything in $protobuf-target</td></tr>
</table>
