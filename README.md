# rstmdb-java

Java client library for RSTMDB.

## Roadmap
* [X] init repo (initial README, .gitignore, LICENSE, etc)
* [X] skeleton of a multinodular gradle project
* [ ] module with java core client
* [ ] test containers
* [ ] maven central publication in the pipeline
* [ ] SAST security tools in the pipeline
* [ ] module with a spring boot autoconfig
* [ ] example hello world applications
* [ ] documentation (website or wiki)

## Build

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