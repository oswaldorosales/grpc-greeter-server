# gRPC Greeter Server

A minimal gRPC server built with Spring Boot 4 and Java 25, demonstrating how to define a Protobuf service and implement it with the new `spring-boot-starter-grpc-server` starter.

## Tech Stack

- **Java 25**
- **Spring Boot 4.1.0**
- **Spring gRPC** (`spring-boot-starter-grpc-server`)
- **Gradle**
- **Protocol Buffers 3**
- **Bruno** — API collection for manual testing

## Service Definition

```protobuf
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply);
}

message HelloRequest {
  string name = 1;
}

message HelloReply {
  string message = 1;
}
```

The server responds with `Hello, {name}!` for every `SayHello` call.

## Running the Server

```bash
./gradlew bootRun
```

The gRPC server starts on port **9090** by default.

## Testing with Bruno

The `bruno/` directory contains a ready-to-use collection.

1. Open [Bruno](https://www.usebruno.com/) and select **Open Collection**.
2. Point it to the `bruno/` folder in this repo.
3. Select the `local` environment.
4. Run the **SayHello** request.

## Testing with grpcurl

Install [grpcurl](https://github.com/fullstorydev/grpcurl#installation) and make sure the server is running.

**List available services** (requires server reflection):

```bash
grpcurl -plaintext localhost:9090 list
```

**Describe the SayHello method:**

```bash
grpcurl -plaintext localhost:9090 describe Greeter.SayHello
```

**Call SayHello:**

```bash
grpcurl -plaintext -d '{"name": "World"}' localhost:9090 Greeter/SayHello
```

Expected response:

```json
{
  "message": "Hello, World!"
}
```

If the server does not have reflection enabled, pass the proto file directly:

```bash
grpcurl -plaintext -proto src/main/proto/greeter.proto \
  -d '{"name": "World"}' localhost:9090 Greeter/SayHello
```

## Gradle Commands

| Command | Description |
|---|---|
| `./gradlew bootRun` | Start the application |
| `./gradlew build` | Compile, test, and package |
| `./gradlew clean build` | Full clean build |
| `./gradlew test` | Run unit tests |
| `./gradlew integrationTest` | Run integration tests |
| `./gradlew check` | Run all verifications (includes `test`) |
| `./gradlew bootJar` | Build the executable Spring Boot JAR |
| `./gradlew generateProto` | Generate Java classes from `.proto` files |
| `./gradlew clean` | Delete build outputs |

## Running Tests

**Unit tests** (excludes integration tests):

```bash
./gradlew test
```

**Integration tests** (starts an in-process gRPC server via `@AutoConfigureTestGrpcTransport`):

```bash
./gradlew integrationTest
```

Integration tests live under `src/test/java/.../integration/` and follow the `*IT` naming convention so they are excluded from the `test` task and only run when `integrationTest` is explicitly invoked.

## Project Structure

```
src/
├── main/
│   ├── java/dev/oswaldorosales/greeter/
│   │   ├── GrpcGreeterServerApplication.java
│   │   └── grpc/
│   │       └── GreeterGrpcService.java
│   ├── proto/
│   │   └── greeter.proto
│   └── resources/
│       └── application.yaml
└── test/
    └── java/dev/oswaldorosales/greeter/
        ├── grpc/
        │   └── GreeterGrpcServiceTest.java     ← unit tests
        └── integration/
            └── GreeterGrpcServiceIT.java        ← integration tests (./gradlew integrationTest)

bruno/
├── bruno.json
├── SayHello.bru
└── environments/
    └── local.bru
```
