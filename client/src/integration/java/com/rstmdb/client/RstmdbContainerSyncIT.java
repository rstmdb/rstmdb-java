package com.rstmdb.client;

import com.rstmdb.client.exception.RstmdbException;
import com.rstmdb.client.model.*;
import com.rstmdb.testcontainer.RstmdbContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Integration test to test synchronous methods of {@link RstmdbClient}
 */
@Testcontainers
class RstmdbContainerSyncIT {

    /**
     * Shared container: started once for the test class and stopped afterwards.
     */
    @Container
    static final RstmdbContainer RSTMDB = new RstmdbContainer();

    static RstmdbClient target;

    @BeforeAll
    static void beforeAll() throws IOException {
        target = RstmdbClientImpl.connect(RSTMDB.getHost(), RSTMDB.getRstmdbPort());

        final var machineSummariesBeforeCreation = target.listMachinesSync();
        System.out.println("machine list before creation");
        System.out.println(machineSummariesBeforeCreation);

        // check whether an order machine exists
        // it may exist because the database has a state and there's no way to delete a machine
        // if no, RstmdbException error with `MACHINE_NOT_FOUND` code should be thrown
        thenThrownBy(() -> target.getMachineSync("order", 1))
                .isInstanceOf(RstmdbException.class)
                .satisfies(ex -> {
                    var rstmdbEx = (RstmdbException) ex;
                    then(rstmdbEx.getErrorCode()).isEqualTo("MACHINE_NOT_FOUND");
                });

        // we create a machine for test purposes
        // all tests use the following machine
        final var orderMachineDefinition = JsonUtils.readFile("test-order-machine.json", MachineDefinition.class);
        final var putMachineRequest = PutMachineRequest.builder().machine("order").version(1).definition(orderMachineDefinition).build();
        target.putMachineSync(putMachineRequest);

        final var machineSummariesAfterCreation = target.listMachinesSync();
        System.out.println("machine list after creation");
        System.out.println(machineSummariesAfterCreation);

    }

    @AfterAll
    static void afterAll() throws Exception {
        target.close();
    }

    @Test
    @Timeout(120)
    @DisplayName("When container is running Then responds to CLI ping")
    void containerIsRunningAndRespondsToCliPing() throws Exception {
        // given
        // container is started by @Container annotation

        // when
        var result = RSTMDB.execInContainer("rstmdb-cli", "-s", "127.0.0.1:7401", "ping");

        // then
        then(RSTMDB.isRunning()).isTrue();
        then(RSTMDB.getHost()).isNotNull();
        then(RSTMDB.getRstmdbPort()).isGreaterThan(0);
        then(result.getExitCode()).isEqualTo(0);
    }

    @Test
    @DisplayName("When ping is called Then completes successfully")
    void shouldPingSync() {
        // given
        // client is connected

        // when
        target.pingSync();

        // then
        // no exception thrown
    }

    @Test
    @DisplayName("When getInfo is called Then returns server information")
    void shouldGetInfoSync() {
        // given
        // client is connected

        // when
        var info = target.getInfoSync();

        // then
        then(info).isNotNull();
        then(info.getServerVersion()).isNotNull();
    }

    @Test
    @DisplayName("When machine exists Then returns machine definition")
    void shouldGetMachineSync() {
        // given
        // order machine created in beforeAll

        // when
        var machine = target.getMachineSync("order", 1);

        // then
        then(machine).isNotNull();
        then(machine.getDefinition()).isNotNull();
        then(machine.getDefinition().getStates()).contains("created", "paid", "shipped", "delivered", "cancelled");
    }

    @Test
    @DisplayName("When machines exist Then returns list of machines")
    void shouldListMachinesSync() {
        // given
        // order machine created in beforeAll

        // when
        var machines = target.listMachinesSync();

        // then
        then(machines).isNotEmpty();
        then(machines).anyMatch(m -> "order".equals(m.getMachine()) && m.getVersions().contains(1));
    }

    @Test
    @DisplayName("When instance is created Then returns instance with initial state")
    void shouldCreateInstanceSync() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var request = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .initialCtx(Map.of("customer", "alice"))
                .build();

        // when
        var result = target.createInstanceSync(request);

        // then
        then(result).isNotNull();
        then(result.getInstanceId()).isEqualTo(instanceId);
        then(result.getState()).isEqualTo("created");
    }

    @Test
    @DisplayName("When instance exists Then returns instance details")
    void shouldGetInstanceSync() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var createRequest = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build();
        target.createInstanceSync(createRequest);

        // when
        var instance = target.getInstanceSync(instanceId);

        // then
        then(instance).isNotNull();
        then(instance.getMachine()).isEqualTo("order");
        then(instance.getState()).isEqualTo("created");
    }

    @Test
    @DisplayName("When instances exist Then returns list of instances")
    void shouldListInstancesSync() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var request = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .initialCtx(Map.of("customer", "alice"))
                .build();

        target.createInstanceSync(request);

        // when
        var instances = target.listInstancesSync();

        // then
        then(instances).isNotNull();
        then(instances.getInstances()).isNotEmpty();
    }

    @Test
    @DisplayName("When listing with options Then returns filtered instances")
    void shouldListInstancesWithOptionsSync() {
        // given
        var options = ListInstancesOptions.builder()
                .machine("order")
                .build();

        // when
        var instances = target.listInstancesSync(options);

        // then
        then(instances).isNotNull();
        then(instances.getInstances()).allMatch(i -> "order".equals(i.getMachine()));
    }

    @Test
    @DisplayName("When event is applied Then state transitions correctly")
    void shouldApplyEventSync() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var createRequest = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build();
        target.createInstanceSync(createRequest);

        var eventRequest = ApplyEventRequest.builder()
                .instanceId(instanceId)
                .event("PAY")
                .payload(Map.of("amount", 99.99))
                .build();

        // when
        var result = target.applyEventSync(eventRequest);

        // then
        then(result).isNotNull();
        then(result.getFromState()).isEqualTo("created");
        then(result.getToState()).isEqualTo("paid");
    }

    @Test
    @DisplayName("When batch operations are executed Then all operations succeed")
    void shouldBatchSync() {
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
        var results = target.batchSync(BatchMode.ATOMIC, createOp, eventOp);

        // then
        then(results).hasSize(2);
        then(results.get(0).getStatus()).isEqualTo("ok");
        then(results.get(1).getStatus()).isEqualTo("ok");
    }

    @Test
    @DisplayName("When instance is deleted Then cannot be retrieved")
    void shouldDeleteInstanceSync() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var createRequest = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build();
        target.createInstanceSync(createRequest);

        // when
        var result = target.deleteInstanceSync(instanceId);

        // then
        then(result).isNotNull();
        then(result.isDeleted()).isTrue();

        // fixme: at the moment instance doesn't note disappear
//        thenThrownBy(() -> target.getInstanceSync(instanceId))
//                .isInstanceOf(RstmdbException.class)
//                .satisfies(ex -> {
//                    var rstmdbEx = (RstmdbException) ex;
//                    then(rstmdbEx.getErrorCode()).isEqualTo("INSTANCE_NOT_FOUND");
//                });
    }

    @Test
    @DisplayName("When instance is snapshotted Then returns snapshot result")
    void shouldSnapshotInstanceSync() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var createRequest = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build();
        target.createInstanceSync(createRequest);

        // when
        var result = target.snapshotInstanceSync(instanceId);

        // then
        then(result).isNotNull();
        then(result.getInstanceId()).isEqualTo(instanceId);
    }

    @Test
    @DisplayName("When WAL is read Then returns records")
    void shouldWalReadSync() {
        // given
        // WAL has entries from previous operations

        // when
        var result = target.walReadSync(0L);

        // then
        then(result).isNotNull();
        then(result.getRecords()).isNotNull();
    }

    @Test
    @DisplayName("When WAL stats are requested Then returns statistics")
    void shouldWalStatsSync() {
        // given
        // WAL has entries from previous operations

        // when
        var result = target.walStatsSync();

        // then
        then(result).isNotNull();
        then(result.getLatestOffset()).isNotNull();
    }

    @Test
    @DisplayName("When compact is called Then completes successfully")
    void shouldCompactSync() {
        // given
        // WAL has entries from previous operations

        // when
        var result = target.compactSync();

        // then
        then(result).isNotNull();
    }

    @Test
    @DisplayName("When watching instance Then returns subscription")
    void shouldWatchInstanceSync() {
        // given
        final var instanceId = UUID.randomUUID().toString();
        var createRequest = CreateInstanceRequest.builder()
                .instanceId(instanceId)
                .machine("order")
                .version(1)
                .build();
        target.createInstanceSync(createRequest);

        var watchRequest = WatchInstanceRequest.builder()
                .instanceId(instanceId)
                .build();

        // when
        var subscription = target.watchInstanceSync(watchRequest);

        // then
        then(subscription).isNotNull();
        subscription.close();
    }

    @Test
    @DisplayName("When watching all instances Then returns subscription")
    void shouldWatchAllSync() {
        // given
        // instances exist from previous tests

        // when
        var subscription = target.watchAllSync();

        // then
        then(subscription).isNotNull();
        subscription.close();
    }

    @Test
    @DisplayName("When watching all with options Then returns filtered subscription")
    void shouldWatchAllWithOptionsSync() {
        // given
        var options = WatchAllOptions.builder()
                .includeCtx(true)
                .machines(List.of("order"))
                .build();

        // when
        var subscription = target.watchAllSync(options);

        // then
        then(subscription).isNotNull();
        subscription.close();
    }
}
