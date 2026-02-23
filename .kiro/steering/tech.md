# Technology Stack

## Language & Runtime

- **Java 17+** (LTS) — Target JDK 17 as minimum, test on 17 and 21
- **Source/Target Compatibility**: Java 17
- **Language Features**: Records (JDK 16+), Pattern Matching, Text Blocks

## Build System

- **Gradle 8.x** with Kotlin DSL
- **Plugins**:
  - `java-library` — Library project configuration
  - `maven-publish` — Maven Central publishing
  - `signing` — JAR signing for Sonatype

## Dependencies

### Compile (Required)

| Dependency | Version | Purpose | Justification |
|-----------|---------|---------|---------------|
| `com.fasterxml.jackson.core:jackson-databind` | 2.17+ | JSON serialization | De facto standard, handles complex generics, zero transitive deps beyond jackson-core/annotations |

### Test

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `org.junit.jupiter:junit-jupiter` | 5.10+ | Unit & integration testing |
| `org.assertj:assertj-core` | 3.25+ | Fluent assertions |
| `org.awaitility:awaitility` | 4.2+ | Async test conditions |

### JDK Standard Library (Zero External Deps)

- `java.net.Socket` — TCP connections
- `java.nio` — Buffers and byte operations
- `java.util.zip.CRC32C` — Hardware-accelerated Castagnoli CRC (JDK 9+)
- `java.util.concurrent` — `CompletableFuture`, `ConcurrentHashMap`, `ReentrantLock`, `ArrayBlockingQueue`
- `java.util.concurrent.Flow` — Reactive streams (`Publisher`, `Subscriber`)
- `javax.net.ssl` — TLS/mTLS support

## Protocol Implementation

### RCPX Binary Framing

- **Magic**: `"RCPX"` (4 bytes, ASCII)
- **Header Size**: 18 bytes (Big-Endian)
- **Max Payload**: 16 MiB
- **CRC**: CRC32C Castagnoli (hardware-accelerated via `java.util.zip.CRC32C`)
- **Wire Format**: JSON payloads in RCPX frames

### Operations

22 RCP operations:
- System: `HELLO`, `AUTH`, `PING`, `BYE`, `INFO`
- Machines: `PUT_MACHINE`, `GET_MACHINE`, `LIST_MACHINES`
- Instances: `CREATE_INSTANCE`, `GET_INSTANCE`, `LIST_INSTANCES`, `DELETE_INSTANCE`
- Events: `APPLY_EVENT`, `BATCH`
- Watch: `WATCH_INSTANCE`, `WATCH_ALL`, `UNWATCH`
- WAL: `SNAPSHOT_INSTANCE`, `WAL_READ`, `WAL_STATS`, `COMPACT`

### Error Codes

16 protocol error codes:
- Protocol: `UNSUPPORTED_PROTOCOL`, `BAD_REQUEST`
- Auth: `UNAUTHORIZED`, `AUTH_FAILED`
- Resources: `NOT_FOUND`, `MACHINE_NOT_FOUND`, `INSTANCE_NOT_FOUND`, `INSTANCE_EXISTS`, `MACHINE_VERSION_EXISTS`, `MACHINE_VERSION_LIMIT_EXCEEDED`
- State: `INVALID_TRANSITION`, `GUARD_FAILED`
- Concurrency: `CONFLICT`
- System: `WAL_IO_ERROR`, `INTERNAL_ERROR`, `RATE_LIMITED`

## Concurrency Model

### Threading

- **Single TCP Connection** per client instance
- **Daemon Read Loop Thread** — Background thread for receiving responses/events
- **Request Multiplexing** — Concurrent requests on single connection via `ConcurrentHashMap<String, CompletableFuture>`
- **Write Serialization** — `ReentrantLock` ensures one frame written at a time

### Thread Safety

| Component | Mechanism |
|-----------|-----------|
| Request ID generation | `AtomicLong` |
| Frame writes | `ReentrantLock` |
| Pending requests | `ConcurrentHashMap` |
| Subscriptions | `ConcurrentHashMap` |
| Event queue | `ArrayBlockingQueue` (bounded 256) |
| Close flag | `volatile boolean` |

### Async Patterns

- **Primary**: `CompletableFuture<T>` for all operations
- **Sync Wrappers**: `*Sync()` methods using `.join()` with exception unwrapping
- **Subscriptions**: `Flow.Publisher` + blocking `Iterable` + callback API

## Constraints

### Technical Constraints

1. **Java 17 Minimum** — No support for Java 8/11
2. **Single Connection** — No connection pooling (matches Go/C# clients)
3. **Bounded Subscriptions** — 256 event queue capacity, drop-oldest backpressure
4. **Unchecked Exceptions** — `RstmdbException extends RuntimeException`
5. **Jackson Required** — Cannot be optional (complex model deserialization)

### Performance Targets

- **Connect + Handshake**: < 100ms
- **Request Latency**: < 5ms (local network)
- **Subscription Throughput**: > 10K events/sec
- **JAR Size**: < 100KB (excluding Jackson)
- **Memory**: < 10MB heap per client instance

### Platform Support

- **OS**: Linux, macOS, Windows
- **JVM**: HotSpot, OpenJ9, GraalVM
- **Architecture**: x64, ARM64

## Development Tools

- **IDE**: IntelliJ IDEA (recommended), Eclipse, VS Code
- **Code Style**: Google Java Style Guide (enforced via checkstyle)
- **CI/CD**: GitHub Actions
- **Registry**: Maven Central (Sonatype OSSRH)

## Reference Implementations

Align behavior with existing clients:

| Client | Language | Async Model | Subscription |
|--------|----------|-------------|--------------|
| Go | Go 1.21+ | `context.Context` | `chan Event` (cap 256) |
| C# | .NET 8+ | `async/await` | `System.Threading.Channels` (bounded 256) |
| Java | Java 17+ | `CompletableFuture` | `Flow.Publisher` + `BlockingQueue` (cap 256) |
