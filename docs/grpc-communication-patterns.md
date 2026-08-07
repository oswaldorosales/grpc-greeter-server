# gRPC Communication Patterns

## The 4 RPC Types

---

### 1. Unary

One request → one response. The closest equivalent to a REST call.

```
Client                        Server
  │                              │
  │──── SayHello(name) ─────────►│
  │                              │  process
  │◄─── HelloReply(message) ─────│
  │                              │
```

**Proto:**
```protobuf
rpc SayHello (HelloRequest) returns (HelloReply);
```

**When to use:**
- Authentication / authorization checks
- Single record lookups (get user by ID)
- Any operation that maps naturally to request → response

**Real example:** a login endpoint — the client sends credentials, the server returns a token or an error. No reason to stream either side.

---

### 2. Server Streaming

One request → stream of responses. The server sends multiple messages and closes the stream when done.

```
Client                        Server
  │                              │
  │──── DownloadFile(name) ─────►│
  │                              │  open file
  │◄─── FileChunk(lines, 1) ─────│
  │◄─── FileChunk(lines, 2) ─────│
  │◄─── FileChunk(lines, 3) ─────│
  │         ...                  │
  │◄─── FileChunk(lines, 21) ────│
  │◄─── [stream closed] ─────────│
  │                              │
```

**Proto:**
```protobuf
rpc DownloadFile (FileRequest) returns (stream FileChunk);
```

**When to use:**
- File or dataset download (this repo's example)
- Real-time feed: stock prices, sports scores
- Tailing logs from a running job
- Sending progress updates for a long-running operation

**Real example:** a report generation service — the client requests a report, the server streams rows as it queries the database instead of waiting to load everything into memory.

---

### 3. Client Streaming

Stream of requests → one response. The client sends multiple messages and the server replies once when it has processed them all.

```
Client                        Server
  │                              │
  │──── UploadChunk(rows, 1) ───►│
  │──── UploadChunk(rows, 2) ───►│
  │──── UploadChunk(rows, 3) ───►│
  │         ...                  │  accumulate / process
  │──── [stream closed] ────────►│
  │                              │
  │◄─── UploadReply(count) ──────│
  │                              │
```

**Proto:**
```protobuf
rpc UploadFile (stream FileChunk) returns (UploadReply);
```

**When to use:**
- File upload (reverse of this repo's example)
- Bulk insert — send thousands of records, get back a summary
- Aggregation — stream sensor readings, server replies with the computed average

**Real example:** an IoT gateway streams temperature readings from thousands of sensors every second. The server accumulates them and replies with an alert if any threshold was exceeded.

---

### 4. Bidirectional Streaming

Stream of requests ↔ stream of responses. Both sides send messages independently over the same connection.

```
Client                        Server
  │                              │
  │──── Message("hello") ───────►│
  │◄─── Message("hi there") ─────│
  │──── Message("how are you?") ►│
  │◄─── Message("good, you?") ───│
  │──── Message("great!") ──────►│
  │◄─── Message("awesome") ──────│
  │         ...                  │
  │──── [stream closed] ────────►│
  │◄─── [stream closed] ─────────│
  │                              │
```

**Proto:**
```protobuf
rpc Chat (stream ChatMessage) returns (stream ChatMessage);
```

**When to use:**
- Real-time chat or collaborative editing
- Interactive command shells (send commands, stream output)
- Game state synchronization between client and server
- Trading systems — client streams orders, server streams confirmations and market events

**Real example:** a multiplayer game where the client streams player position and inputs, and the server streams back the positions of all other players in real time.

---

## Load Balancing with Multiple Instances

### The Problem

gRPC runs over **HTTP/2**, which uses long-lived, multiplexed connections — many RPCs travel over the same TCP connection. A traditional **L4 (TCP) load balancer** distributes connections, not individual requests.

```
                  L4 Load Balancer
                  ┌─────────────┐
Client ──────────►│             │──────────► Node A  ← all RPCs land here
                  │  round-robin│
                  │  on connect │──────────► Node B  ← idle
                  │             │
                  └─────────────┘──────────► Node C  ← idle
```

Once the client connects to Node A, every RPC goes there. Nodes B and C sit idle regardless of how busy Node A is.

With REST/HTTP/1.1 this is not a problem — requests open short-lived connections and the load balancer can distribute at the request level.

---

### Solution 1 — L7 Load Balancer

A proxy that understands HTTP/2 distributes at the **RPC level**, not the connection level.

```
                  L7 Load Balancer (Envoy / AWS ALB / Nginx)
                  ┌──────────────────────────────────────┐
                  │  inspects HTTP/2 frames               │
Client ──────────►│  routes each RPC independently        │──► Node A
                  │                                       │──► Node B
                  └──────────────────────────────────────┘──► Node C
```

- **Envoy** — the standard for gRPC, used by Istio and most service meshes
- **AWS ALB** — native gRPC support since 2020
- **Nginx** — with `grpc_pass` directive

---

### Solution 2 — Client-side Load Balancing

The client knows about all instances and distributes requests itself using gRPC's built-in name resolver and load balancing policy.

```
Client
┌──────────────────────────────────┐
│  name resolver → [A, B, C]       │
│  policy: round_robin             │──► Node A  (RPC 1, 4, 7...)
│                                  │──► Node B  (RPC 2, 5, 8...)
└──────────────────────────────────┘──► Node C  (RPC 3, 6, 9...)
```

```java
ManagedChannel channel = ManagedChannelBuilder
    .forTarget("dns:///my-service:9090")
    .defaultLoadBalancingPolicy("round_robin")
    .build();
```

Requires DNS to resolve all instance IPs (headless Service in Kubernetes).

---

### Solution 3 — Service Mesh (Istio / Linkerd)

A sidecar proxy is injected next to each service instance. It intercepts all traffic and handles load balancing transparently — no changes to the client or server code.

```
  Pod A                          Pod B
┌──────────────────┐           ┌──────────────────┐
│  App   │ Sidecar │◄─────────►│ Sidecar │  App   │
│ :9090  │ :15001  │           │ :15001  │ :9090  │
└──────────────────┘           └──────────────────┘
         ▲
         │ control plane manages routing rules
         ▼
     Istio / Linkerd
```

Best option when you already have a service mesh — zero code changes, full observability and mTLS included.

---

### Streaming and Load Balancing

A stream always lives on the **same node** for its entire duration — this is correct behavior. You do not want chunks from the same file jumping between servers mid-stream. Load balancing applies when a **new stream is opened**, not within an active stream.

```
Client                    LB                  Nodes
  │                        │                    │
  │── stream 1 open ──────►│──────────────────► Node A
  │   (21 chunks on A)     │                    │
  │                        │                    │
  │── stream 2 open ──────►│──────────────────► Node B
  │   (21 chunks on B)     │                    │
  │                        │                    │
  │── stream 3 open ──────►│──────────────────► Node C
```
