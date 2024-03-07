enablePlugins(ProtobufPlugin)
version := "0.1.0-SNAPSHOT"
name := "grpc"
scalaVersion := "2.13.13"
protobufGrpcEnabled := true

libraryDependencies ++= Seq(
  "javax.annotation" % "javax.annotation-api" % "1.3.2",
  "io.grpc" % "grpc-netty" % protobufGrpcVersion.value,
  "io.grpc" % "grpc-protobuf" % protobufGrpcVersion.value,
  "io.grpc" % "grpc-stub" % protobufGrpcVersion.value,
  "com.google.protobuf" % "protobuf-java" % (ProtobufConfig / version).value % ProtobufConfig,
)

ProtobufConfig / sourceDirectories += (ProtobufConfig / protobufExternalIncludePath).value
