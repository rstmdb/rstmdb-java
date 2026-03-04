# rstmdb-java

Official Java client library for [rstmdb](https://github.com/rstmdb/rstmdb) — a distributed state machine database.

[![CI](https://github.com/rstmdb/rstmdb-java/actions/workflows/ci.yml/badge.svg)](https://github.com/rstmdb/rstmdb-java/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.rstmdb/rstmdb-client)](https://central.sonatype.com/artifact/com.rstmdb/rstmdb-client)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Roadmap
* [X] init repo (initial README, .gitignore, LICENSE, etc)
* [X] skeleton of a multinodular gradle project
* [ ] [WIP] module with java core client
* [ ] [WIP] test containers
* [ ] testing of different JDK versions
* [ ] maven central publication in the pipeline
* [ ] SAST security tools in the pipeline
* [ ] module with a spring boot autoconfig
* [ ] example hello world applications
* [ ] documentation (website or wiki)

## Requirements

- Java 11+
- rstmdb server

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.rstmdb:rstmdb-client:0.1.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.rstmdb:rstmdb-client:0.1.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.rstmdb</groupId>
    <artifactId>rstmdb-client</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Quick Start

```java
import com.rstmdb.client.*;
import com.rstmdb.client.model.*;
import java.util.Map;

try (var client = RstmdbClient.connect("localhost", 7401)) {
    // Register a state machine
    client.putMachineSync(new PutMachineRequest(
        "order", 1,
        new MachineDefinition(
            new String[]{"created", "paid", "shipped"},
            "created",
            new Transition[]{
                new Transition(new String[]{"created"}, "PAY", "paid", null),
                new Transition(new String[]{"paid"}, "SHIP", "shipped", null),
            },
            null
        ),
        null
    ));

    // Create an instance
    var instance = client.createInstanceSync(new CreateInstanceRequest(
        "order-001", "order", 1,
        Map.of("customer", "alice"),
        null
    ));

    // Apply an event
    var result = client.applyEventSync(new ApplyEventRequest(
        "order-001", "PAY", Map.of("amount", 99.99),
        null, null, null, null
    ));
    System.out.println(result.fromState() + " -> " + result.toState());
}
```

## API Overview

All operations are available as both async (`CompletableFuture<T>`) and sync (`*Sync`) methods:

| Category | Methods |
|----------|---------|
| System | `ping`, `getInfo` |
| Machines | `putMachine`, `getMachine`, `listMachines` |
| Instances | `createInstance`, `getInstance`, `listInstances`, `deleteInstance` |
| Events | `applyEvent`, `batch` |
| Watch | `watchInstance`, `watchAll` |
| WAL | `snapshotInstance`, `walRead`, `walStats`, `compact` |

### Connection Options

```java
var opts = RstmdbOptions.builder()
    .auth("bearer-token")
    .connectTimeout(Duration.ofSeconds(5))
    .requestTimeout(Duration.ofSeconds(15))
    .clientName("my-service")
    .build();

var client = RstmdbClient.connect("localhost", 7401, opts);
```

### TLS

```java
// CA certificate
var opts = RstmdbOptions.builder()
    .sslContext(RstmdbOptions.createTlsContext(Path.of("ca.pem")))
    .build();

// Insecure (development only)
var opts = RstmdbOptions.builder()
    .sslContext(RstmdbOptions.insecureTlsContext())
    .build();
```

### Async Usage

```java
client.ping()
    .thenCompose(v -> client.createInstance(request))
    .thenCompose(inst -> client.applyEvent(eventRequest))
    .thenAccept(result -> System.out.println(result.toState()))
    .exceptionally(ex -> {
        if (ex.getCause() instanceof RstmdbException re) {
            System.err.println("Error: " + re.getErrorCode());
        }
        return null;
    })
    .join();
```

### Watch / Subscriptions

```java
var sub = client.watchAllSync(new WatchAllOptions(
    true, null, new String[]{"order"}, null, null, null));

for (var event : sub.events()) {
    System.out.printf("%s: %s -> %s%n",
        event.instanceId(), event.fromState(), event.toState());
}
```

### Error Handling

```java
try {
    client.applyEventSync(request);
} catch (RstmdbException e) {
    if (RstmdbException.isInvalidTransition(e)) {
        // handle invalid transition
    } else if (e.isRetryable()) {
        // safe to retry
    }
}
```

## Build library

### Prerequisites
- JDK 25

### Build the project
```bash
./gradlew build
```

### Run tests
```bash
./gradlew test
```

### Clean build artifacts
```bash
./gradlew clean
```

### Build without tests
```bash
./gradlew build -x test
```

## License

MIT