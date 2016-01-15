import sbtprotobuf.{ProtobufPlugin=>PB}

PB.protobufSettings

PB.runProtoc in PB.protobufConfig := { args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v261" +: args.toArray)
}
