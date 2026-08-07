# REST vs gRPC — File Streaming

## REST

```
GET /files/users.csv
```

The server has two options:

**Option A — Full response:**
The file is loaded entirely into memory and sent as a single response. Fine for 2,000 rows, but a server will run out of memory at 200,000.

**Option B — Chunked Transfer Encoding:**
HTTP/1.1 supports `Transfer-Encoding: chunked`. The server writes bytes to the socket as it reads the file, and the client can start processing before the transfer finishes. This works, but the transport is raw bytes — the client is responsible for parsing the CSV itself.

---

## gRPC

Each `FileChunk` is a typed Protobuf message with `repeated string lines` and `chunk_number`. The client receives already-structured rows, not raw bytes.

```protobuf
message FileChunk {
  repeated string lines = 1;
  int32 chunk_number    = 2;
}
```

The server reads the file lazily and batches lines using Java 25 core APIs:

```java
reader.lines()
      .gather(Gatherers.windowFixed(100))
      .forEach(lines -> responseObserver.onNext(
          FileChunk.newBuilder()
              .addAllLines(lines)
              .setChunkNumber(chunkNumber.incrementAndGet())
              .build()
      ));
```

---

## Comparison

| | REST (chunked) | gRPC streaming |
|---|---|---|
| Protocol | HTTP/1.1 or HTTP/2 | HTTP/2 always |
| Contract | None — client assumes the format | Proto defines the structure |
| Payload | Raw bytes | Typed messages |
| Progress | Client has no idea how many chunks are coming | `chunk_number` on every message |
| Mid-stream error | Client receives a truncated response | `onError()` closes the stream with a typed `Status` |
| Bidirectional | No | Yes, supported natively |
| Browser support | Native | Requires grpc-web or a proxy |

---

## When to choose each

**REST** when:
- The file is small and will always be small
- The consumer is a browser with no intermediary
- The team is unfamiliar with gRPC and a strict contract is not critical

**gRPC streaming** when:
- File size is variable or potentially large
- The client needs to process data as it arrives (pipeline processing)
- You want a strong contract — switching from CSV to Parquet only requires a proto change
- Both sides are microservices (no browsers involved)

---

## Why gRPC is the right call here

In this repo the gRPC approach wins on every front: the server controls chunk boundaries, the client receives immediately processable rows, and if the file fails mid-read the client gets a typed error — not a silently truncated body with no explanation.
