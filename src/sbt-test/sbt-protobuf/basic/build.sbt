import sbtprotobuf.{ProtobufPlugin=>PB}

PB.protobufSettings

version in PB.protobufConfig := "3.0.0-beta-2"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % (version in PB.protobufConfig).value % PB.protobufConfig.name

PB.runProtoc in PB.protobufConfig := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v300" +: args.toArray)
}

excludeFilter in PB.protobufConfig := "test1.proto"
