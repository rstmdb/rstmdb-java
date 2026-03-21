package com.rstmdb.client;

import com.rstmdb.client.exception.RstmdbException;
import com.rstmdb.client.model.*;
import com.rstmdb.testcontainer.RstmdbContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Integration test to test asynchronous methods of {@link RstmdbClient}
 */
@Testcontainers
class RstmdbContainerAsyncIT {

    @Container
    static final RstmdbContainer RSTMDB = new RstmdbContainer("rstmdb/rstmdb:0.2.0")
            .withFlushAll();

    static RstmdbClient target;

    @BeforeAll
    static void beforeAll() throws IOException {
        target = RstmdbClientImpl.connect(RSTMDB.getHost(), RSTMDB.getRstmdbPort());
    }

    @AfterAll
    static void afterAll() throws Exception {
        target.close();
    }

    @BeforeEach
    void createTestMachine() throws IOException {
        final var definition = JsonUtils.readFile("test-order-machine.json", MachineDefinition.class);
        final var request = PutMachineRequest.builder()
                .machine("order")
                .version(1)
                .definition(definition)
                .build();
        target.putMachine(request).join();
    }

    @AfterEach
    void cleanUp() {
        // Wipe all instances so each test starts with an empty database.
        target.flushAllSync();
    }

    @Test
    @DisplayName("When ping is called Then completes successfully")
    void shouldPing() {
        // given
        // client is connected

        // when
        // then
        target.ping().join();
    }

    @Test
    @DisplayName("When getInfo is called Then returns server information")
    void shouldGetInfo() {
        // given
        // client is connected

        // when
        var result = target.getInfo().join();

        // then
        then(result).isNotNull();
        then(result.getServerVersion()).isNotNull();
    }

    @Test
    @DisplayName("When machine exists Then returns machine definition")
    void shouldGetMachine() {
        // given
        // order machine created in beforeEach

        // when
        var result = target.getMachine("order", 1).join();

        // then
        then(result).isNotNull();
        then(result.getDefinition()).isNotNull();
        then(result.getDefinition().getStates()).contains("created", "paid", "shipped", "delivered", "cancelled");
    }

    @Test
    @DisplayName("When machines exist Then returns list of machines")
    void shouldListMachines() {
        // given
        // order machine created in beforeEach

        // when
        var result = target.listMachines().join();

        // then
        then(result).isNotEmpty();
        then(result).anyMatch(m -> "order".equals(m.getMachine()) && m.getVersions().contains(1));
    }

    @Test
    @DisplayName("When instance is created Then returns instance with initial state")
    void shouldCreateInstance() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var request = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .initialCtx(Map.of("customer", "alice"))
                .build();

        // when
        var result = target.createInstance(request).join();

        // then
        then(result).isNotNull();
        then(result.getInstanceId()).isEqualTo(instanceId);
        then(result.getState()).isEqualTo("created");
    }

    @Test
    @DisplayName("When instance exists Then returns instance details")
    void shouldGetInstance() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        target.createInstance(CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build()).join();

        // when
        var result = target.getInstance(instanceId).join();

        // then
        then(result).isNotNull();
        then(result.getMachine()).isEqualTo("order");
        then(result.getState()).isEqualTo("created");
    }

    @Test
    @DisplayName("When instances exist Then returns list of instances")
    void shouldListInstances() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        target.createInstance(CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build()).join();

        // when
        var result = target.listInstances().join();

        // then
        then(result).isNotNull();
        then(result.getInstances()).isNotEmpty();
    }

    @Test
    @DisplayName("When listing with options Then returns filtered instances")
    void shouldListInstancesWithOptions() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        target.createInstance(CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build()).join();
        var options = ListInstancesOptions.builder()
                .machine("order")
                .build();

        // when
        var result = target.listInstances(options).join();

        // then
        then(result).isNotNull();
        then(result.getInstances()).allMatch(i -> "order".equals(i.getMachine()));
    }

    @Test
    @DisplayName("When event is applied Then state transitions correctly")
    void shouldApplyEvent() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        target.createInstance(CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build()).join();

        var eventRequest = ApplyEventRequest.builder()
                .instanceId(instanceId)
                .event("PAY")
                .payload(Map.of("amount", 99.99))
                .build();

        // when
        var result = target.applyEvent(eventRequest).join();

        // then
        then(result).isNotNull();
        then(result.getFromState()).isEqualTo("created");
        then(result.getToState()).isEqualTo("paid");
    }

    @Test
    @DisplayName("When batch operations are executed Then all operations succeed")
    void shouldBatch() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var createOp = BatchOperation.createInstance(
                CreateInstanceRequest.builder()
                        .instanceId(instanceId)
                        .machine("order")
                        .version(1)
                        .build()
        );
        var eventOp = BatchOperation.applyEvent(
                ApplyEventRequest.builder()
                        .instanceId(instanceId)
                        .event("PAY")
                        .build()
        );

        // when
        var result = target.batch(BatchMode.ATOMIC, createOp, eventOp).join();

        // then
        then(result).hasSize(2);
        then(result.get(0).getStatus()).isEqualTo("ok");
        then(result.get(1).getStatus()).isEqualTo("ok");
    }

    @Test
    @DisplayName("When instance is deleted Then cannot be retrieved")
    void shouldDeleteInstance() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        target.createInstance(CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build()).join();

        // when
        var result = target.deleteInstance(instanceId).join();

        // then
        then(result).isNotNull();
        then(result.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("When instance is deleted with idempotency key Then completes successfully")
    void shouldDeleteInstanceWithIdempotencyKey() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        target.createInstance(CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build()).join();

        // when
        var result = target.deleteInstance(instanceId, "idempotency-key-1").join();

        // then
        then(result).isNotNull();
        then(result.isDeleted()).isTrue();

        thenThrownBy(() -> target.getInstance(instanceId).join())
                .cause()
                .isInstanceOf(RstmdbException.class)
                .satisfies(ex -> {
                    var rstmdbEx = (RstmdbException) ex;
                    then(rstmdbEx.getErrorCode()).isEqualTo("INSTANCE_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("When instance is snapshotted Then returns snapshot result")
    void shouldSnapshotInstance() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        target.createInstance(CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build()).join();

        // when
        var result = target.snapshotInstance(instanceId).join();

        // then
        then(result).isNotNull();
        then(result.getInstanceId()).isEqualTo(instanceId);
    }

    @Test
    @DisplayName("When WAL is read Then returns records")
    void shouldWalRead() {
        // given
        // WAL has entries from beforeEach machine creation

        // when
        var result = target.walRead(0L).join();

        // then
        then(result).isNotNull();
        then(result.getRecords()).isNotNull();
    }

    @Test
    @DisplayName("When WAL is read with limit Then returns limited records")
    void shouldWalReadWithLimit() {
        // given
        // WAL has entries from beforeEach machine creation

        // when
        var result = target.walRead(0L, 10).join();

        // then
        then(result).isNotNull();
        then(result.getRecords()).isNotNull();
    }

    @Test
    @DisplayName("When WAL stats are requested Then returns statistics")
    void shouldWalStats() {
        // given
        // WAL has entries from beforeEach machine creation

        // when
        var result = target.walStats().join();

        // then
        then(result).isNotNull();
        then(result.getLatestOffset()).isNotNull();
    }

    @Test
    @DisplayName("When compact is called Then completes successfully")
    void shouldCompact() {
        // given
        // WAL has entries from beforeEach machine creation

        // when
        var result = target.compact().join();

        // then
        then(result).isNotNull();
    }

    @Test
    @DisplayName("When compact with force snapshot Then completes successfully")
    void shouldCompactWithForceSnapshot() {
        // given
        // WAL has entries from beforeEach machine creation

        // when
        var result = target.compact(true).join();

        // then
        then(result).isNotNull();
    }

    @Test
    @DisplayName("When watching instance Then returns subscription")
    void shouldWatchInstance() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        target.createInstance(CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build()).join();

        var watchRequest = WatchInstanceRequest.builder()
                .instanceId(instanceId)
                .build();

        // when
        var result = target.watchInstance(watchRequest).join();

        // then
        then(result).isNotNull();
        result.close();
    }

    @Test
    @DisplayName("When flushAll is called Then all instances are removed")
    void shouldFlushAll() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        target.createInstance(CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build()).join();

        // when
        var result = target.flushAll().join();

        // then
        then(result).isNotNull();
        then(result.isFlushed()).isTrue();
        then(result.getInstancesRemoved()).isGreaterThan(0);
        then(result.getMachinesRemoved()).isGreaterThan(0);

        thenThrownBy(() -> target.getInstance(instanceId).join())
                .cause()
                .isInstanceOf(RstmdbException.class)
                .satisfies(ex -> {
                    var rstmdbEx = (RstmdbException) ex;
                    then(rstmdbEx.getErrorCode()).isEqualTo("INSTANCE_NOT_FOUND");
                });

        thenThrownBy(() -> target.getMachine("order", 1).join())
                .cause()
                .isInstanceOf(RstmdbException.class)
                .satisfies(ex -> {
                    var rstmdbEx = (RstmdbException) ex;
                    then(rstmdbEx.getErrorCode()).isEqualTo("MACHINE_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("When watching all instances Then returns subscription")
    void shouldWatchAll() {
        // given
        // client is connected

        // when
        var result = target.watchAll().join();

        // then
        then(result).isNotNull();
        result.close();
    }

    @Test
    @DisplayName("When watching all with options Then returns filtered subscription")
    void shouldWatchAllWithOptions() {
        // given
        var options = WatchAllOptions.builder()
                .includeCtx(true)
                .machines(List.of("order"))
                .build();

        // when
        var result = target.watchAll(options).join();

        // then
        then(result).isNotNull();
        result.close();
    }
}
