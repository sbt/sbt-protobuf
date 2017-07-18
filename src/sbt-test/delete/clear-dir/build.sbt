enablePlugins(ProtobufPlugin)

scalaVersion := "2.10.6"

protobufRunProtoc in ProtobufConfig := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v261" +: args.toArray)
}
