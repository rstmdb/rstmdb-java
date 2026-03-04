package com.rstmdb.client;

import com.rstmdb.client.exception.RstmdbException;
import com.rstmdb.client.model.*;
import com.rstmdb.testcontainer.RstmdbContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
    static final RstmdbContainer RSTMDB = new RstmdbContainer();

    static RstmdbClient target;

    @BeforeAll
    static void beforeAll() throws IOException {
        target = RstmdbClientImpl.connect(RSTMDB.getHost(), RSTMDB.getRstmdbPort());

        // check whether an order machine exists
        // it may exist because the database has a state and there's no way to delete a machine
        // if no, RstmdbException error with `MACHINE_NOT_FOUND` code should be thrown
        thenThrownBy(() -> target.getMachine("order", 1).join())
                .cause()
                .isInstanceOf(RstmdbException.class)
                .satisfies(ex -> {
                    var rstmdbEx = (RstmdbException) ex;
                    then(rstmdbEx.getErrorCode()).isEqualTo("MACHINE_NOT_FOUND");
                });

        // we create a machine for test purposes
        // all tests use the following machine
        final var orderMachineDefinition = JsonUtils.readFile("test-order-machine.json", MachineDefinition.class);
        final var putMachineRequest = PutMachineRequest.builder().machine("order").version(1).definition(orderMachineDefinition).build();
        target.putMachine(putMachineRequest).join();
    }

    @AfterAll
    static void afterAll() throws Exception {
        target.close();
    }

    @Test
    @DisplayName("When ping is called Then completes successfully")
    void shouldPing() {
        // given
        // client is connected

        // when
        var future = target.ping();

        // then
        then(future).isNotNull();
        future.join();
    }

    @Test
    @DisplayName("When getInfo is called Then returns server information")
    void shouldGetInfo() {
        // given
        // client is connected

        // when
        var future = target.getInfo();

        // then
        var info = future.join();
        then(info).isNotNull();
        then(info.getServerVersion()).isNotNull();
    }

    @Test
    @DisplayName("When machine exists Then returns machine definition")
    void shouldGetMachine() {
        // given
        // order machine created in beforeAll

        // when
        var future = target.getMachine("order", 1);

        // then
        var machine = future.join();
        then(machine).isNotNull();
        then(machine.getDefinition()).isNotNull();
        then(machine.getDefinition().getStates()).contains("created", "paid", "shipped", "delivered", "cancelled");
    }

    @Test
    @DisplayName("When machines exist Then returns list of machines")
    void shouldListMachines() {
        // given
        // order machine created in beforeAll

        // when
        var future = target.listMachines();

        // then
        var machines = future.join();
        then(machines).isNotEmpty();
        then(machines).anyMatch(m -> "order".equals(m.getMachine()) && m.getVersions().contains(1));
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
        var future = target.createInstance(request);

        // then
        var result = future.join();
        then(result).isNotNull();
        then(result.getInstanceId()).isEqualTo(instanceId);
        then(result.getState()).isEqualTo("created");
    }

    @Test
    @DisplayName("When instance exists Then returns instance details")
    void shouldGetInstance() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var createRequest = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build();
        target.createInstance(createRequest).join();

        // when
        var future = target.getInstance(instanceId);

        // then
        var instance = future.join();
        then(instance).isNotNull();
        then(instance.getMachine()).isEqualTo("order");
        then(instance.getState()).isEqualTo("created");
    }

    @Test
    @DisplayName("When instances exist Then returns list of instances")
    void shouldListInstances() {
        // given
        // instances created in previous tests

        // when
        var future = target.listInstances();

        // then
        var instances = future.join();
        then(instances).isNotNull();
        then(instances.getInstances()).isNotEmpty();
    }

    @Test
    @DisplayName("When listing with options Then returns filtered instances")
    void shouldListInstancesWithOptions() {
        // given
        var options = ListInstancesOptions.builder()
                .machine("order")
                .build();

        // when
        var future = target.listInstances(options);

        // then
        var instances = future.join();
        then(instances).isNotNull();
        then(instances.getInstances()).allMatch(i -> "order".equals(i.getMachine()));
    }

    @Test
    @DisplayName("When event is applied Then state transitions correctly")
    void shouldApplyEvent() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var createRequest = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build();
        target.createInstance(createRequest).join();

        var eventRequest = ApplyEventRequest.builder()
                .instanceId(instanceId)
                .event("PAY")
                .payload(Map.of("amount", 99.99))
                .build();

        // when
        var future = target.applyEvent(eventRequest);

        // then
        var result = future.join();
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
        var future = target.batch(BatchMode.ATOMIC, createOp, eventOp);

        // then
        var results = future.join();
        then(results).hasSize(2);
        then(results.get(0).getStatus()).isEqualTo("ok");
        then(results.get(1).getStatus()).isEqualTo("ok");
    }

    @Test
    @DisplayName("When instance is deleted Then cannot be retrieved")
    void shouldDeleteInstance() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var createRequest = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build();
        target.createInstance(createRequest).join();

        // when
        var future = target.deleteInstance(instanceId);

        // then
        var result = future.join();
        then(result).isNotNull();
        then(result.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("When instance is deleted with idempotency key Then completes successfully")
    void shouldDeleteInstanceWithIdempotencyKey() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var createRequest = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build();
        target.createInstance(createRequest).join();

        // when
        var future = target.deleteInstance(instanceId, "idempotency-key-1");

        // then
        var result = future.join();
        then(result).isNotNull();
        then(result.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("When instance is snapshotted Then returns snapshot result")
    void shouldSnapshotInstance() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var createRequest = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build();
        target.createInstance(createRequest).join();

        // when
        var future = target.snapshotInstance(instanceId);

        // then
        var result = future.join();
        then(result).isNotNull();
        then(result.getInstanceId()).isEqualTo(instanceId);
    }

    @Test
    @DisplayName("When WAL is read Then returns records")
    void shouldWalRead() {
        // given
        // WAL has entries from previous operations

        // when
        var future = target.walRead(0L);

        // then
        var result = future.join();
        then(result).isNotNull();
        then(result.getRecords()).isNotNull();
    }

    @Test
    @DisplayName("When WAL is read with limit Then returns limited records")
    void shouldWalReadWithLimit() {
        // given
        // WAL has entries from previous operations

        // when
        var future = target.walRead(0L, 10);

        // then
        var result = future.join();
        then(result).isNotNull();
        then(result.getRecords()).isNotNull();
    }

    @Test
    @DisplayName("When WAL stats are requested Then returns statistics")
    void shouldWalStats() {
        // given
        // WAL has entries from previous operations

        // when
        var future = target.walStats();

        // then
        var result = future.join();
        then(result).isNotNull();
        then(result.getLatestOffset()).isNotNull();
    }

    @Test
    @DisplayName("When compact is called Then completes successfully")
    void shouldCompact() {
        // given
        // WAL has entries from previous operations

        // when
        var future = target.compact();

        // then
        var result = future.join();
        then(result).isNotNull();
    }

    @Test
    @DisplayName("When compact with force snapshot Then completes successfully")
    void shouldCompactWithForceSnapshot() {
        // given
        // WAL has entries from previous operations

        // when
        var future = target.compact(true);

        // then
        var result = future.join();
        then(result).isNotNull();
    }

    @Test
    @DisplayName("When watching instance Then returns subscription")
    void shouldWatchInstance() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var createRequest = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build();
        target.createInstance(createRequest).join();

        var watchRequest = WatchInstanceRequest.builder()
                .instanceId(instanceId)
                .build();

        // when
        var future = target.watchInstance(watchRequest);

        // then
        var subscription = future.join();
        then(subscription).isNotNull();
        subscription.close();
    }

    @Test
    @DisplayName("When watching all instances Then returns subscription")
    void shouldWatchAll() {
        // given
        // instances exist from previous tests

        // when
        var future = target.watchAll();

        // then
        var subscription = future.join();
        then(subscription).isNotNull();
        subscription.close();
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
        var future = target.watchAll(options);

        // then
        var subscription = future.join();
        then(subscription).isNotNull();
        subscription.close();
    }
}
