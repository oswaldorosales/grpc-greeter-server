# gRPC Greeter Server

A gRPC server built with Spring Boot 4 and Java 25, demonstrating unary and server-side streaming RPCs using the new `spring-boot-starter-grpc-server` starter.

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
  rpc SayHello    (HelloRequest) returns (HelloReply);
  rpc DownloadFile(FileRequest)  returns (stream FileChunk);
}
```

### SayHello — Unary

The client sends a name and receives a single greeting.

```protobuf
message HelloRequest { string name    = 1; }
message HelloReply   { string message = 1; }
```

### DownloadFile — Server Streaming

The client requests a file by name and receives it as a stream of chunks. Each chunk carries up to 100 lines of the file and its sequence number. The stream closes automatically when all lines have been sent.

```protobuf
message FileRequest { string filename         = 1; }
message FileChunk   { repeated string lines   = 1;
                      int32 chunk_number       = 2; }
```

The server reads the file lazily using Java 25 core APIs — `Files.newBufferedReader()` streams line by line and `Gatherers.windowFixed(100)` batches them into fixed-size windows without loading the entire file into memory.

The demo file is `src/main/resources/data/users.csv` (2,000 rows), which produces 21 chunks over the stream.

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
4. Run **SayHello** (unary) or **DownloadFile** (server streaming).

## Testing with grpcurl

Install [grpcurl](https://github.com/fullstorydev/grpcurl#installation) and make sure the server is running.

**List available services** (requires server reflection):

```bash
grpcurl -plaintext localhost:9090 list
```

**Call SayHello:**

```bash
grpcurl -plaintext -d '{"name": "World"}' localhost:9090 Greeter/SayHello
```

```json
{ "message": "Hello, World!" }
```

**Stream DownloadFile:**

```bash
grpcurl -plaintext -d '{"filename": "users.csv"}' localhost:9090 Greeter/DownloadFile
```

Each response message contains a chunk of lines and its sequence number:

```json
{ "lines": ["id,name,email,country,amount,date", "1,Bob 1,...", "..."], "chunkNumber": 1 }
{ "lines": ["101,Alice 101,...", "..."], "chunkNumber": 2 }
...
{ "lines": ["1991,Carlos 1991,...", "..."], "chunkNumber": 21 }
```

If the server does not have reflection enabled, pass the proto file directly:

```bash
grpcurl -plaintext -proto src/main/proto/greeter.proto \
  -d '{"filename": "users.csv"}' localhost:9090 Greeter/DownloadFile
```

## Further Reading

| Doc | Description |
|---|---|
| [gRPC Communication Patterns](docs/grpc-communication-patterns.md) | The 4 RPC types with diagrams, practical examples, and load balancing strategies |
| [REST vs gRPC — File Streaming](docs/rest-vs-grpc-streaming.md) | Trade-off analysis for the `DownloadFile` streaming use case |

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
