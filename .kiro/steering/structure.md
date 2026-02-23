# Project Structure

## Directory Layout

```
rstmdb-java/
├── .github/
│   └── workflows/
│       ├── ci.yml                      # Build & test on push/PR
│       └── release.yml                 # Publish to Maven Central
├── .kiro/
│   └── steering/
│       ├── product.md                  # Project goals & users
│       ├── tech.md                     # Technology stack
│       └── structure.md                # This file
├── lib/
│   ├── build.gradle.kts                # Library build config
│   └── src/
│       ├── main/
│       │   ├── java/com/rstmdb/client/
│       │   │   ├── RstmdbClient.java           # Public API facade
│       │   │   ├── RstmdbOptions.java          # Connection options builder
│       │   │   ├── exception/
│       │   │   │   ├── ErrorCodes.java         # 16 error code constants
│       │   │   │   └── RstmdbException.java    # Typed exception
│       │   │   ├── protocol/
│       │   │   │   ├── FrameCodec.java         # RCPX encode/decode + CRC32C
│       │   │   │   ├── WireMessage.java        # Wire types (request/response/event)
│       │   │   │   └── Operations.java         # 22 operation constants
│       │   │   ├── model/
│       │   │   │   ├── MachineDefinition.java  # Machine, Transition
│       │   │   │   ├── MachineInfo.java        # PutMachineRequest/Result, MachineSummary
│       │   │   │   ├── Instance.java           # CreateInstanceRequest/Result, Instance, InstanceSummary, InstanceList
│       │   │   │   ├── EventResult.java        # ApplyEventRequest/Result
│       │   │   │   ├── BatchTypes.java         # BatchMode, BatchOperation, BatchResult
│       │   │   │   ├── WatchTypes.java         # WatchInstanceRequest, WatchAllOptions, SubscriptionEvent
│       │   │   │   ├── WalTypes.java           # SnapshotResult, WalReadResult, WalStatsResult, CompactResult
│       │   │   │   └── ServerInfo.java         # HelloResult, ServerInfo
│       │   │   └── transport/
│       │   │       ├── Connection.java         # TCP + multiplexing + read loop
│       │   │       └── Subscription.java       # Queue-based event stream
│       │   └── resources/
│       │       └── META-INF/
│       │           └── MANIFEST.MF             # JAR manifest
│       └── test/
│           └── java/com/rstmdb/client/
│               ├── protocol/
│               │   └── FrameCodecTest.java     # Frame encoding/decoding tests
│               ├── integration/
│               │   ├── ClientIntegrationTest.java  # Full client tests
│               │   └── MockServer.java         # TCP mock server
│               └── model/
│                   └── ModelSerializationTest.java  # Jackson serialization tests
├── examples/
│   ├── basic/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/BasicExample.java
│   ├── streaming/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/StreamingExample.java
│   ├── async/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/AsyncExample.java
│   └── batch/
│       ├── build.gradle.kts
│       └── src/main/java/BatchExample.java
├── config/
│   └── checkstyle/
│       └── checkstyle.xml                      # Google Java Style
├── build.gradle.kts                            # Root build config
├── settings.gradle.kts                         # Multi-project setup
├── gradle.properties                           # Version & properties
├── gradlew                                     # Gradle wrapper (Unix)
├── gradlew.bat                                 # Gradle wrapper (Windows)
├── .gitignore
├── README.md
├── LICENSE                                     # MIT
└── CONTRIBUTING.md
```

## Package Organization

### `com.rstmdb.client` (root)
Public API entry points:
- `RstmdbClient` — Main client facade
- `RstmdbOptions` — Configuration builder

### `com.rstmdb.client.exception`
Error handling:
- `ErrorCodes` — Static constants for 16 error codes
- `RstmdbException` — Unchecked exception with structured error info

### `com.rstmdb.client.protocol`
Wire protocol implementation (internal):
- `FrameCodec` — RCPX binary framing
- `WireMessage` — Internal wire types (not exposed in public API)
- `Operations` — Operation name constants

### `com.rstmdb.client.model`
Public data models (all records):
- Machine definitions and transitions
- Instance lifecycle types
- Event application types
- Batch operation types
- Watch/subscription types
- WAL operation types
- Server info types

### `com.rstmdb.client.transport`
Connection management (internal):
- `Connection` — TCP socket, multiplexing, read loop
- `Subscription` — Event streaming with multiple consumption patterns

## Naming Conventions

### Classes

| Type | Pattern | Example |
|------|---------|---------|
| Public API | `Rstmdb*` | `RstmdbClient`, `RstmdbOptions`, `RstmdbException` |
| Models (records) | Noun/NounPhrase | `Instance`, `MachineDefinition`, `ApplyEventRequest` |
| Request types | `*Request` | `CreateInstanceRequest`, `WatchInstanceRequest` |
| Result types | `*Result` | `ApplyEventResult`, `PutMachineResult` |
| Options types | `*Options` | `WatchAllOptions`, `ListInstancesOptions` |
| Internal classes | Descriptive noun | `Connection`, `Subscription`, `FrameCodec` |
| Constants | `UPPER_SNAKE_CASE` | `HELLO`, `APPLY_EVENT`, `INSTANCE_NOT_FOUND` |

### Methods

| Type | Pattern | Example |
|------|---------|---------|
| Async operations | `verbNoun()` | `ping()`, `createInstance()`, `applyEvent()` |
| Sync operations | `verbNounSync()` | `pingSync()`, `createInstanceSync()` |
| Getters (records) | `fieldName()` | `instanceId()`, `fromState()` |
| Builders | `fieldName(value)` | `auth(token)`, `connectTimeout(duration)` |
| Factory methods | `verb()` / `of()` | `connect()`, `builder()`, `of()` |
| Internal helpers | `verbNoun()` | `encodeFrame()`, `computeCrc32c()` |

### Files

| Type | Pattern | Example |
|------|---------|---------|
| Single class | `ClassName.java` | `RstmdbClient.java` |
| Related types | `DomainTypes.java` | `BatchTypes.java`, `WatchTypes.java`, `WalTypes.java` |
| Tests | `ClassNameTest.java` | `FrameCodecTest.java`, `ClientIntegrationTest.java` |
| Examples | `DescriptiveExample.java` | `BasicExample.java`, `StreamingExample.java` |

### Variables

| Type | Pattern | Example |
|------|---------|---------|
| Local variables | `camelCase` | `instanceId`, `fromState`, `walOffset` |
| Constants | `UPPER_SNAKE_CASE` | `HEADER_SIZE`, `MAX_PAYLOAD_SIZE` |
| Fields | `camelCase` | `connection`, `objectMapper`, `pending` |
| Parameters | `camelCase` | `host`, `port`, `options` |

## Code Organization Principles

### 1. Layered Architecture
```
Public API (RstmdbClient, RstmdbOptions)
    ↓
Models (records with Jackson annotations)
    ↓
Transport (Connection, Subscription)
    ↓
Protocol (FrameCodec, WireMessage)
```

### 2. Visibility Rules

- **Public**: Only `RstmdbClient`, `RstmdbOptions`, `RstmdbException`, all model records
- **Package-private**: `Connection`, `Subscription`, `FrameCodec`, `WireMessage`
- **Private**: Internal helpers, fields

### 3. Immutability

- All model types are **records** (immutable by default)
- `RstmdbOptions` built via builder, immutable after construction
- Mutable state only in `Connection` (thread-safe via concurrent collections)

### 4. Error Handling

- **Unchecked exceptions** — `RstmdbException extends RuntimeException`
- **Structured errors** — Error code, message, retryable flag, details map
- **Static helpers** — `RstmdbException.isInvalidTransition(ex)`

### 5. Testing Organization

```
test/java/com/rstmdb/client/
├── protocol/          # Unit tests for protocol layer
├── model/             # Serialization tests
└── integration/       # End-to-end tests with MockServer
```

## Build Artifacts

### JAR Structure

```
rstmdb-client-0.1.0.jar
├── META-INF/
│   ├── MANIFEST.MF
│   └── maven/com.rstmdb/rstmdb-client/
│       ├── pom.xml
│       └── pom.properties
└── com/rstmdb/client/
    ├── RstmdbClient.class
    ├── RstmdbOptions.class
    ├── exception/
    ├── protocol/
    ├── model/
    └── transport/
```

### Published Artifacts

- `rstmdb-client-0.1.0.jar` — Main library
- `rstmdb-client-0.1.0-sources.jar` — Source code
- `rstmdb-client-0.1.0-javadoc.jar` — API documentation
- `rstmdb-client-0.1.0.pom` — Maven POM

## Version Management

- **Semantic Versioning**: `MAJOR.MINOR.PATCH`
- **Current**: `0.1.0` (initial release)
- **Location**: `gradle.properties` → `version=0.1.0`
- **Git Tags**: `v0.1.0`, `v0.2.0`, etc.

## Documentation Structure

- **README.md** — Quick start, installation, basic examples
- **CONTRIBUTING.md** — Development setup, testing, PR guidelines
- **Javadoc** — API reference (all public classes/methods)
- **Examples** — Working code samples for common use cases
