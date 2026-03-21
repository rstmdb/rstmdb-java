package com.rstmdb.client;

import com.rstmdb.client.exception.ErrorCodes;
import com.rstmdb.client.exception.RstmdbException;
import com.rstmdb.client.model.ApplyEventRequest;
import com.rstmdb.client.model.ApplyEventResult;
import com.rstmdb.client.model.BatchMode;
import com.rstmdb.client.model.BatchOperation;
import com.rstmdb.client.model.CompactResult;
import com.rstmdb.client.model.FlushAllResult;
import com.rstmdb.client.model.CreateInstanceRequest;
import com.rstmdb.client.model.CreateInstanceResult;
import com.rstmdb.client.model.DeleteInstanceResult;
import com.rstmdb.client.model.Instance;
import com.rstmdb.client.model.InstanceList;
import com.rstmdb.client.model.InstanceSummary;
import com.rstmdb.client.model.IoStats;
import com.rstmdb.client.model.ListInstancesOptions;
import com.rstmdb.client.model.MachineDefinition;
import com.rstmdb.client.model.MachineInfo;
import com.rstmdb.client.model.MachineSummary;
import com.rstmdb.client.model.PutMachineRequest;
import com.rstmdb.client.model.PutMachineResult;
import com.rstmdb.client.model.ServerInfo;
import com.rstmdb.client.model.SnapshotResult;
import com.rstmdb.client.model.Transition;
import com.rstmdb.client.model.WalReadResult;
import com.rstmdb.client.model.WalRecord;
import com.rstmdb.client.model.WalStatsResult;
import com.rstmdb.client.model.WatchAllOptions;
import com.rstmdb.client.model.WatchInstanceRequest;
import com.rstmdb.client.protocol.WireMessage.WireEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.BDDAssertions.*;
import static org.awaitility.Awaitility.await;

class RstmdbClientImplTest {

    private MockServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockServer();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.close();
    }

    private RstmdbClientImpl connectToMock() throws IOException {
        return connectToMock(null);
    }

    private RstmdbClientImpl connectToMock(RstmdbOptions opts) throws IOException {
        return RstmdbClientImpl.connect("localhost", server.getPort(), opts);
    }

    @Test
    @DisplayName("When client connects Then server info is returned")
    void connect() throws IOException {
        // given
        // default mock server

        // when
        try (var client = connectToMock()) {
            var info = client.getServerInfo();

            // then
            then(info.getServerName()).isEqualTo("mock-rstmdb");
            then(info.getProtocolVersion()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("When client connects with auth token Then connection succeeds")
    void connectWithAuth() throws IOException {
        // given
        var opts = RstmdbOptions.builder().auth("test-token").build();

        // when
        try (var client = connectToMock(opts)) {
            var info = client.getServerInfo();

            // then
            then(info).isNotNull();
        }
    }

    @Test
    @DisplayName("When ping is called Then no exception is thrown")
    void ping() throws IOException {
        // given
        server.setRequestHandler(req -> Map.of("pong", true));

        // when / then
        try (var client = connectToMock()) {
            thenCode(client::pingSync).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("When getInfo is called Then server info details are returned")
    void getInfo() throws IOException {
        // given
        server.setRequestHandler(req -> new ServerInfo(
                "rstmdb", "0.2.0", 1, List.of("watch"), 16777216, 100));

        // when
        try (var client = connectToMock()) {
            var info = client.getInfoSync();

            // then
            then(info.getServerName()).isEqualTo("rstmdb");
            then(info.getMaxBatchOps()).isEqualTo(100);
        }
    }

    @Test
    @DisplayName("When putMachine is called Then machine is created")
    void putMachine() throws IOException {
        // given
        server.setRequestHandler(req -> new PutMachineResult("order", 1, "abc123", true));

        // when
        try (var client = connectToMock()) {
            var result = client.putMachineSync(new PutMachineRequest(
                    "order", 1,
                    new MachineDefinition(
                            List.of("created", "paid"),
                            "created",
                            List.of(
                                    new Transition(List.of("created"), "PAY", "paid", null)
                            ),
                            null
                    ),
                    null
            ));

            // then
            then(result.isCreated()).isTrue();
            then(result.getMachine()).isEqualTo("order");
        }
    }

    @Test
    @DisplayName("When getMachine is called Then machine info with checksum is returned")
    void getMachine() throws IOException {
        // given
        server.setRequestHandler(req -> new MachineInfo(
                new MachineDefinition(List.of("a", "b"), "a", null, null),
                "sha256hex"
        ));

        // when
        try (var client = connectToMock()) {
            var info = client.getMachineSync("order", 1);

            // then
            then(info.getChecksum()).isEqualTo("sha256hex");
        }
    }

    @Test
    @DisplayName("When listMachines is called Then all machines are returned")
    void listMachines() throws IOException {
        // given
        server.setRequestHandler(req -> Map.of("items", List.of(
                new MachineSummary("order", List.of(1, 2)),
                new MachineSummary("user", List.of(1))
        )));

        // when
        try (var client = connectToMock()) {
            var machines = client.listMachinesSync();

            // then
            then(machines).hasSize(2);
            then(machines.get(0).getMachine()).isEqualTo("order");
        }
    }

    @Test
    @DisplayName("When createInstance is called Then instance is created with initial state")
    void createInstance() throws IOException {
        // given
        server.setRequestHandler(req -> new CreateInstanceResult("inst-1", "created", 1));

        // when
        try (var client = connectToMock()) {
            var result = client.createInstanceSync(new CreateInstanceRequest(
                    null, "order", 1, Map.of("customer", "alice"), null));

            // then
            then(result.getInstanceId()).isEqualTo("inst-1");
            then(result.getState()).isEqualTo("created");
        }
    }

    @Test
    @DisplayName("When getInstance is called Then instance state is returned")
    void getInstance() throws IOException {
        // given
        server.setRequestHandler(req -> new Instance(
                "order", 1, "paid", Map.of("amount", 99), null, 0));

        // when
        try (var client = connectToMock()) {
            var inst = client.getInstanceSync("inst-1");

            // then
            then(inst.getState()).isEqualTo("paid");
        }
    }

    @Test
    @DisplayName("When listInstances is called with options Then filtered list is returned")
    void listInstances() throws IOException {
        // given
        server.setRequestHandler(req -> new InstanceList(
                List.of(
                        new InstanceSummary("i1", "order", 1, "created", 0, 0, 0),
                        new InstanceSummary("i2", "order", 1, "paid", 0, 0, 0)
                ),
                2, false
        ));

        // when
        try (var client = connectToMock()) {
            var list = client.listInstancesSync(
                    ListInstancesOptions.builder().machine("order").limit(10).build());

            // then
            then(list.getTotal()).isEqualTo(2);
            then(list.getInstances()).hasSize(2);
        }
    }

    @Test
    @DisplayName("When deleteInstance is called Then instance is deleted")
    void deleteInstance() throws IOException {
        // given
        server.setRequestHandler(req -> new DeleteInstanceResult("inst-1", true, 5));

        // when
        try (var client = connectToMock()) {
            var result = client.deleteInstanceSync("inst-1");

            // then
            then(result.isDeleted()).isTrue();
        }
    }

    @Test
    @DisplayName("When applyEvent is called Then state transitions successfully")
    void applyEvent() throws IOException {
        // given
        server.setRequestHandler(req -> new ApplyEventResult(
                "created", "paid", null, 2, true, "evt-1"));

        // when
        try (var client = connectToMock()) {
            var result = client.applyEventSync(new ApplyEventRequest(
                    "inst-1", "PAY", Map.of("amount", 99.99),
                    null, null, null, null));

            // then
            then(result.isApplied()).isTrue();
            then(result.getToState()).isEqualTo("paid");
        }
    }

    @Test
    @DisplayName("When batch is called with atomic mode Then all operations succeed")
    void batch() throws IOException {
        // given
        server.setRequestHandler(req -> Map.of("results", new Object[]{
                Map.of("status", "ok", "result", Map.of("instance_id", "i1")),
                Map.of("status", "ok", "result", Map.of("applied", true))
        }));

        // when
        try (var client = connectToMock()) {
            var results = client.batchSync(BatchMode.ATOMIC,
                    BatchOperation.createInstance(new CreateInstanceRequest(null, "order", 1, null, null)),
                    BatchOperation.applyEvent(new ApplyEventRequest("i1", "PAY", null, null, null, null, null)));

            // then
            then(results).hasSize(2);
            then(results.get(0).getStatus()).isEqualTo("ok");
        }
    }

    @Test
    @DisplayName("When watchInstance is called Then subscription receives events")
    void watchInstance() throws Exception {
        // given
        String subId = "sub-watch-1";
        server.setRequestHandler(req -> {
            if ("WATCH_INSTANCE".equals(req.getOp())) {
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        server.sendEvent(new WireEvent(
                                "event", subId, "inst-1", "order", 1, 0,
                                "created", "paid", "PAY", null, null));
                    } catch (Exception ignored) {}
                }).start();
                return Map.of("subscription_id", subId);
            }
            return Map.of();
        });

        // when
        try (var client = connectToMock()) {
            var sub = client.watchInstanceSync(new WatchInstanceRequest("inst-1", true, 0));
            var event = sub.poll(Duration.ofSeconds(5));

            // then
            then(sub.getId()).isEqualTo(subId);
            then(event).isNotNull();
            then(event.getToState()).isEqualTo("paid");
            then(event.getInstanceId()).isEqualTo("inst-1");

            sub.close();
        }
    }

    @Test
    @DisplayName("When watchAll is called with options Then subscription is created")
    void watchAll() throws Exception {
        // given
        String subId = "sub-all-1";
        server.setRequestHandler(req -> {
            if ("WATCH_ALL".equals(req.getOp())) {
                return Map.of("subscription_id", subId);
            }
            return Map.of();
        });

        // when
        try (var client = connectToMock()) {
            var sub = client.watchAllSync(new WatchAllOptions(
                    true, null, List.of("order"), null, null, List.of("shipped")));

            // then
            then(sub.getId()).isEqualTo(subId);
            sub.close();
        }
    }

    @Test
    @DisplayName("When snapshotInstance is called Then snapshot is created")
    void snapshotInstance() throws IOException {
        // given
        server.setRequestHandler(req -> new SnapshotResult("inst-1", "snap-1", 10, 4096L, null));

        // when
        try (var client = connectToMock()) {
            var result = client.snapshotInstanceSync("inst-1");

            // then
            then(result.getSnapshotId()).isEqualTo("snap-1");
        }
    }

    @Test
    @DisplayName("When walRead is called Then WAL records are returned")
    void walRead() throws IOException {
        // given
        server.setRequestHandler(req -> new WalReadResult(
                List.of(
                        new WalRecord(1, 0, Map.of("type", "create")),
                        new WalRecord(2, 1, Map.of("type", "event"))
                ),
                2
        ));

        // when
        try (var client = connectToMock()) {
            var result = client.walReadSync(0);

            // then
            then(result.getRecords()).hasSize(2);
            then(result.getNextOffset()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("When walStats is called Then WAL statistics are returned")
    void walStats() throws IOException {
        // given
        server.setRequestHandler(req -> new WalStatsResult(
                100, 3, 1024000, 99,
                new IoStats(500000, 200000, 100, 50, 100)
        ));

        // when
        try (var client = connectToMock()) {
            var stats = client.walStatsSync();

            // then
            then(stats.getEntryCount()).isEqualTo(100);
            then(stats.getIoStats().getFsyncs()).isEqualTo(100);
        }
    }

    @Test
    @DisplayName("When compact is called Then compaction result is returned")
    void compact() throws IOException {
        // given
        server.setRequestHandler(req -> new CompactResult(2, 5, 1048576, 10, 3));

        // when
        try (var client = connectToMock()) {
            var result = client.compactSync();

            // then
            then(result.getSnapshotsCreated()).isEqualTo(2);
            then(result.getBytesReclaimed()).isEqualTo(1048576);
        }
    }

    @Test
    @DisplayName("When flushAll is called synchronously Then flushed counts are returned")
    void flushAll() throws IOException {
        // given
        server.setRequestHandler(req -> new FlushAllResult(true, 42, 3));

        // when
        try (var client = connectToMock()) {
            var result = client.flushAllSync();

            // then
            then(result.isFlushed()).isTrue();
            then(result.getInstancesRemoved()).isEqualTo(42);
            then(result.getMachinesRemoved()).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("When flushAll is called asynchronously Then future completes with flushed counts")
    void flushAllAsync() throws Exception {
        // given
        server.setRequestHandler(req -> new FlushAllResult(true, 10, 2));

        // when
        try (var client = connectToMock()) {
            var result = client.flushAll().get();

            // then
            then(result.isFlushed()).isTrue();
            then(result.getInstancesRemoved()).isEqualTo(10);
            then(result.getMachinesRemoved()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("When flushAll is disabled on server Then BAD_REQUEST exception is thrown")
    void flushAllDisabled() throws IOException {
        // given
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.BAD_REQUEST, "FLUSH_ALL is disabled in server configuration", false));

        // when / then
        try (var client = connectToMock()) {
            thenThrownBy(client::flushAllSync)
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> {
                        var re = (RstmdbException) ex;
                        then(re.getErrorCode()).isEqualTo(ErrorCodes.BAD_REQUEST);
                    });
        }
    }

    @Test
    @DisplayName("When flushAll is called on empty database Then zero removals are returned")
    void flushAllEmptyDatabase() throws IOException {
        // given
        server.setRequestHandler(req -> new FlushAllResult(true, 0, 0));

        // when
        try (var client = connectToMock()) {
            var result = client.flushAllSync();

            // then
            then(result.isFlushed()).isTrue();
            then(result.getInstancesRemoved()).isZero();
            then(result.getMachinesRemoved()).isZero();
        }
    }

    @Test
    @DisplayName("When server returns NOT_FOUND Then RstmdbException with NOT_FOUND code is thrown")
    void errorCodeMapping() throws IOException {
        // given
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.NOT_FOUND, "not found", false));

        // when / then
        try (var client = connectToMock()) {
            thenThrownBy(() -> client.getInstanceSync("no-such-id"))
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> then(RstmdbException.isNotFound(ex)).isTrue());
        }
    }

    @Test
    @DisplayName("When server returns INSTANCE_NOT_FOUND Then exception is classified correctly")
    void errorInstanceNotFound() throws IOException {
        // given
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.INSTANCE_NOT_FOUND, "instance not found", false));

        // when / then
        try (var client = connectToMock()) {
            thenThrownBy(() -> client.getInstanceSync("no-such-id"))
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> then(RstmdbException.isInstanceNotFound(ex)).isTrue());
        }
    }

    @Test
    @DisplayName("When server returns MACHINE_NOT_FOUND Then exception is classified correctly")
    void errorMachineNotFound() throws IOException {
        // given
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.MACHINE_NOT_FOUND, "machine not found", false));

        // when / then
        try (var client = connectToMock()) {
            thenThrownBy(() -> client.getMachineSync("no-such", 1))
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> then(RstmdbException.isMachineNotFound(ex)).isTrue());
        }
    }

    @Test
    @DisplayName("When server returns INVALID_TRANSITION Then exception is classified correctly")
    void errorInvalidTransition() throws IOException {
        // given
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.INVALID_TRANSITION, "invalid transition", false));

        // when / then
        try (var client = connectToMock()) {
            thenThrownBy(() -> client.applyEventSync(
                    new ApplyEventRequest("i1", "BAD", null, null, null, null, null)))
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> then(RstmdbException.isInvalidTransition(ex)).isTrue());
        }
    }

    @Test
    @DisplayName("When server returns GUARD_FAILED Then exception is classified correctly")
    void errorGuardFailed() throws IOException {
        // given
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.GUARD_FAILED, "guard failed", false));

        // when / then
        try (var client = connectToMock()) {
            thenThrownBy(() -> client.applyEventSync(
                    new ApplyEventRequest("i1", "E", null, null, null, null, null)))
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> then(RstmdbException.isGuardFailed(ex)).isTrue());
        }
    }

    @Test
    @DisplayName("When server returns CONFLICT Then exception is classified correctly")
    void errorConflict() throws IOException {
        // given
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.CONFLICT, "conflict", false));

        // when / then
        try (var client = connectToMock()) {
            thenThrownBy(() -> client.applyEventSync(
                    new ApplyEventRequest("i1", "E", null, null, null, null, null)))
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> then(RstmdbException.isConflict(ex)).isTrue());
        }
    }

    @Test
    @DisplayName("When server returns RATE_LIMITED Then exception is retryable")
    void retryableErrors() throws IOException {
        // given
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.RATE_LIMITED, "slow down", true));

        // when / then
        try (var client = connectToMock()) {
            thenThrownBy(client::pingSync)
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> {
                        var re = (RstmdbException) ex;
                        then(re.isRetryable()).isTrue();
                        then(re.getErrorCode()).isEqualTo(ErrorCodes.RATE_LIMITED);
                    });
        }
    }

    @Test
    @DisplayName("When request exceeds timeout Then exception is thrown")
    void requestTimeout() throws IOException {
        // given
        server.setRequestHandler(req -> {
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ignored) {
                // expected
            }
            return null;
        });

        // when / then
        try (var client = connectToMock(RstmdbOptions.builder()
                .requestTimeout(Duration.ofMillis(200)).build())) {
            thenThrownBy(client::pingSync)
                    .isInstanceOf(Exception.class);
        }
    }

    @Test
    @DisplayName("When connection is closed Then subsequent calls throw exception")
    void connectionClose() throws IOException {
        // given
        try (var client = connectToMock()) {
            // when
            client.close();

            // then
            thenThrownBy(client::pingSync)
                    .isInstanceOf(RstmdbException.class);
        }
    }

    @Test
    @DisplayName("When server drops connection Then client detects the failure")
    void connectionDrop() throws Exception {
        // given
        try (var client = connectToMock()) {
            // when
            server.close();

            // then
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                    thenThrownBy(client::pingSync)
                            .isInstanceOf(Exception.class));
        }
    }

    @Test
    @DisplayName("When all error codes are checked Then 16 distinct codes exist")
    void allErrorCodesExist() {
        // given
        String[] codes = {
                ErrorCodes.UNSUPPORTED_PROTOCOL, ErrorCodes.BAD_REQUEST,
                ErrorCodes.UNAUTHORIZED, ErrorCodes.AUTH_FAILED,
                ErrorCodes.NOT_FOUND, ErrorCodes.MACHINE_NOT_FOUND,
                ErrorCodes.MACHINE_VERSION_EXISTS, ErrorCodes.MACHINE_VERSION_LIMIT_EXCEEDED,
                ErrorCodes.INSTANCE_NOT_FOUND, ErrorCodes.INSTANCE_EXISTS,
                ErrorCodes.INVALID_TRANSITION, ErrorCodes.GUARD_FAILED,
                ErrorCodes.CONFLICT, ErrorCodes.WAL_IO_ERROR,
                ErrorCodes.INTERNAL_ERROR, ErrorCodes.RATE_LIMITED
        };

        // when / then
        then(codes).hasSize(16);
        then(codes).doesNotContainNull();
        then(codes).doesNotHaveDuplicates();
    }
}
