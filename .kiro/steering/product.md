# Product Definition

## Project Goals

Build the official Java client library for rstmdb — a distributed state machine database.

### Primary Goals

1. **Full Protocol Compliance** — Implement complete RCP protocol with RCPX binary framing, CRC32C checksums, all 22 operations, and 16 error codes
2. **Production-Ready** — Thread-safe, performant, reliable client suitable for production workloads
3. **Developer Experience** — Clean, intuitive API with both async and sync patterns
4. **Zero Friction** — Minimal dependencies, fast startup, small footprint
5. **Ecosystem Integration** — Publish to Maven Central for easy adoption

### Non-Goals

- Connection pooling (single connection per client)
- Spring Boot auto-configuration (separate module later)
- gRPC transport (TCP + RCP only)
- Android support (JDK 17+ only)

## Target Users

### Primary Audience

**Java Backend Engineers** building distributed systems who need:
- State machine orchestration (workflows, orders, approvals)
- Event-driven architectures with state tracking
- Reliable state transitions with guards and validation
- Real-time state change notifications

### Use Cases

1. **E-commerce Order Management** — Track orders through created → paid → shipped → delivered states
2. **Workflow Orchestration** — Multi-step approval processes with conditional transitions
3. **IoT Device Management** — Track device lifecycle states (provisioned, active, maintenance, retired)
4. **Game State Management** — Player progression, match states, tournament brackets
5. **Financial Transactions** — Payment processing with state validation and rollback

### User Personas

**Senior Backend Engineer**
- Needs reliable state management without building custom solutions
- Values clean APIs and good documentation
- Expects async/await patterns and reactive streams
- Requires production-grade error handling

**Platform Engineer**
- Integrating rstmdb into microservices architecture
- Needs observability (tracing, metrics)
- Requires TLS/mTLS support
- Values thread safety and connection management

**Startup Developer**
- Rapid prototyping with minimal setup
- Needs quick start examples
- Values zero-config defaults
- Expects Maven Central availability

## Success Metrics

- **Adoption**: 100+ Maven Central downloads in first month
- **Performance**: < 5ms request latency (local), > 10K events/sec subscription throughput
- **Reliability**: Zero memory leaks, graceful connection handling
- **Developer Satisfaction**: Clear documentation, working examples, responsive issue handling
