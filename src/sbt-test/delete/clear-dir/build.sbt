enablePlugins(ProtobufPlugin)

protobufRunProtoc in protobufConfig := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v261" +: args.toArray)
}
