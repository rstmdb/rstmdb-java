package com.rstmdb.client;

import com.rstmdb.client.exception.ErrorCodes;
import com.rstmdb.client.exception.RstmdbException;
import com.rstmdb.client.model.ApplyEventRequest;
import com.rstmdb.client.model.ApplyEventResult;
import com.rstmdb.client.model.BatchMode;
import com.rstmdb.client.model.BatchOperation;
import com.rstmdb.client.model.CompactResult;
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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

class ClientIntegrationTest {

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
        return RstmdbClientImpl.connect("127.0.0.1", server.getPort(), opts);
    }

    @Test
    void connect() throws IOException {
        try (var client = connectToMock()) {
            var info = client.getServerInfo();
            assertThat(info.getServerName()).isEqualTo("mock-rstmdb");
            assertThat(info.getProtocolVersion()).isEqualTo(1);
        }
    }

    @Test
    void connectWithAuth() throws IOException {
        try (var client = connectToMock(RstmdbOptions.builder().auth("test-token").build())) {
            assertThat(client.getServerInfo()).isNotNull();
        }
    }

    @Test
    void ping() throws IOException {
        server.setRequestHandler(req -> Map.of("pong", true));
        try (var client = connectToMock()) {
            assertThatCode(client::pingSync).doesNotThrowAnyException();
        }
    }

    @Test
    void getInfo() throws IOException {
        server.setRequestHandler(req -> new ServerInfo(
                "rstmdb", "0.2.0", 1, List.of("watch"), 16777216, 100));
        try (var client = connectToMock()) {
            var info = client.getInfoSync();
            assertThat(info.getServerName()).isEqualTo("rstmdb");
            assertThat(info.getMaxBatchOps()).isEqualTo(100);
        }
    }

    @Test
    void putMachine() throws IOException {
        server.setRequestHandler(req -> new PutMachineResult("order", 1, "abc123", true));
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
            assertThat(result.isCreated()).isTrue();
            assertThat(result.getMachine()).isEqualTo("order");
        }
    }

    @Test
    void getMachine() throws IOException {
        server.setRequestHandler(req -> new MachineInfo(
                new MachineDefinition(List.of("a", "b"), "a", null, null),
                "sha256hex"
        ));
        try (var client = connectToMock()) {
            var info = client.getMachineSync("order", 1);
            assertThat(info.getChecksum()).isEqualTo("sha256hex");
        }
    }

    @Test
    void listMachines() throws IOException {
        server.setRequestHandler(req -> Map.of("items", List.of(
                new MachineSummary("order", List.of(1, 2)),
                new MachineSummary("user", List.of(1))
        )));
        try (var client = connectToMock()) {
            var machines = client.listMachinesSync();
            assertThat(machines).hasSize(2);
            assertThat(machines.get(0).getMachine()).isEqualTo("order");
        }
    }

    @Test
    void createInstance() throws IOException {
        server.setRequestHandler(req -> new CreateInstanceResult("inst-1", "created", 1));
        try (var client = connectToMock()) {
            var result = client.createInstanceSync(new CreateInstanceRequest(
                    null, "order", 1, Map.of("customer", "alice"), null));
            assertThat(result.getInstanceId()).isEqualTo("inst-1");
            assertThat(result.getState()).isEqualTo("created");
        }
    }

    @Test
    void getInstance() throws IOException {
        server.setRequestHandler(req -> new Instance(
                "order", 1, "paid", Map.of("amount", 99), null, 0));
        try (var client = connectToMock()) {
            var inst = client.getInstanceSync("inst-1");
            assertThat(inst.getState()).isEqualTo("paid");
        }
    }

    @Test
    void listInstances() throws IOException {
        server.setRequestHandler(req -> new InstanceList(
                List.of(
                        new InstanceSummary("i1", "order", 1, "created", 0, 0, 0),
                        new InstanceSummary("i2", "order", 1, "paid", 0, 0, 0)
                ),
                2, false
        ));
        try (var client = connectToMock()) {
            var list = client.listInstancesSync(
                    ListInstancesOptions.builder().machine("order").limit(10).build());
            assertThat(list.getTotal()).isEqualTo(2);
            assertThat(list.getInstances()).hasSize(2);
        }
    }

    @Test
    void deleteInstance() throws IOException {
        server.setRequestHandler(req -> new DeleteInstanceResult("inst-1", true, 5));
        try (var client = connectToMock()) {
            var result = client.deleteInstanceSync("inst-1");
            assertThat(result.isDeleted()).isTrue();
        }
    }

    @Test
    void applyEvent() throws IOException {
        server.setRequestHandler(req -> new ApplyEventResult(
                "created", "paid", null, 2, true, "evt-1"));
        try (var client = connectToMock()) {
            var result = client.applyEventSync(new ApplyEventRequest(
                    "inst-1", "PAY", Map.of("amount", 99.99),
                    null, null, null, null));
            assertThat(result.isApplied()).isTrue();
            assertThat(result.getToState()).isEqualTo("paid");
        }
    }

    @Test
    void batch() throws IOException {
        server.setRequestHandler(req -> Map.of("results", new Object[]{
                Map.of("status", "ok", "result", Map.of("instance_id", "i1")),
                Map.of("status", "ok", "result", Map.of("applied", true))
        }));
        try (var client = connectToMock()) {
            var results = client.batchSync(BatchMode.ATOMIC,
                    BatchOperation.createInstance(new CreateInstanceRequest(null, "order", 1, null, null)),
                    BatchOperation.applyEvent(new ApplyEventRequest("i1", "PAY", null, null, null, null, null)));
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getStatus()).isEqualTo("ok");
        }
    }

    @Test
    void watchInstance() throws Exception {
        String subId = "sub-watch-1";
        server.setRequestHandler(req -> {
            if ("WATCH_INSTANCE".equals(req.getOp())) {
                // Send event after a brief delay
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

        try (var client = connectToMock()) {
            var sub = client.watchInstanceSync(new WatchInstanceRequest("inst-1", true, 0));
            assertThat(sub.getId()).isEqualTo(subId);

            var event = sub.poll(Duration.ofSeconds(5));
            assertThat(event).isNotNull();
            assertThat(event.getToState()).isEqualTo("paid");
            assertThat(event.getInstanceId()).isEqualTo("inst-1");

            sub.close();
        }
    }

    @Test
    void watchAll() throws Exception {
        String subId = "sub-all-1";
        server.setRequestHandler(req -> {
            if ("WATCH_ALL".equals(req.getOp())) {
                return Map.of("subscription_id", subId);
            }
            return Map.of();
        });

        try (var client = connectToMock()) {
            var sub = client.watchAllSync(new WatchAllOptions(
                    true, null, List.of("order"), null, null, List.of("shipped")));
            assertThat(sub.getId()).isEqualTo(subId);
            sub.close();
        }
    }

    @Test
    void snapshotInstance() throws IOException {
        server.setRequestHandler(req -> new SnapshotResult("inst-1", "snap-1", 10, 4096L, null));
        try (var client = connectToMock()) {
            var result = client.snapshotInstanceSync("inst-1");
            assertThat(result.getSnapshotId()).isEqualTo("snap-1");
        }
    }

    @Test
    void walRead() throws IOException {
        server.setRequestHandler(req -> new WalReadResult(
                List.of(
                        new WalRecord(1, 0, Map.of("type", "create")),
                        new WalRecord(2, 1, Map.of("type", "event"))
                ),
                2
        ));
        try (var client = connectToMock()) {
            var result = client.walReadSync(0);
            assertThat(result.getRecords()).hasSize(2);
            assertThat(result.getNextOffset()).isEqualTo(2);
        }
    }

    @Test
    void walStats() throws IOException {
        server.setRequestHandler(req -> new WalStatsResult(
                100, 3, 1024000, 99,
                new IoStats(500000, 200000, 100, 50, 100)
        ));
        try (var client = connectToMock()) {
            var stats = client.walStatsSync();
            assertThat(stats.getEntryCount()).isEqualTo(100);
            assertThat(stats.getIoStats().getFsyncs()).isEqualTo(100);
        }
    }

    @Test
    void compact() throws IOException {
        server.setRequestHandler(req -> new CompactResult(2, 5, 1048576, 10, 3));
        try (var client = connectToMock()) {
            var result = client.compactSync();
            assertThat(result.getSnapshotsCreated()).isEqualTo(2);
            assertThat(result.getBytesReclaimed()).isEqualTo(1048576);
        }
    }

    @Test
    void errorCodeMapping() throws IOException {
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.NOT_FOUND, "not found", false));
        try (var client = connectToMock()) {
            assertThatThrownBy(() -> client.getInstanceSync("no-such-id"))
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> assertThat(RstmdbException.isNotFound(ex)).isTrue());
        }
    }

    @Test
    void errorInstanceNotFound() throws IOException {
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.INSTANCE_NOT_FOUND, "instance not found", false));
        try (var client = connectToMock()) {
            assertThatThrownBy(() -> client.getInstanceSync("no-such-id"))
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> assertThat(RstmdbException.isInstanceNotFound(ex)).isTrue());
        }
    }

    @Test
    void errorMachineNotFound() throws IOException {
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.MACHINE_NOT_FOUND, "machine not found", false));
        try (var client = connectToMock()) {
            assertThatThrownBy(() -> client.getMachineSync("no-such", 1))
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> assertThat(RstmdbException.isMachineNotFound(ex)).isTrue());
        }
    }

    @Test
    void errorInvalidTransition() throws IOException {
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.INVALID_TRANSITION, "invalid transition", false));
        try (var client = connectToMock()) {
            assertThatThrownBy(() -> client.applyEventSync(
                    new ApplyEventRequest("i1", "BAD", null, null, null, null, null)))
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> assertThat(RstmdbException.isInvalidTransition(ex)).isTrue());
        }
    }

    @Test
    void errorGuardFailed() throws IOException {
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.GUARD_FAILED, "guard failed", false));
        try (var client = connectToMock()) {
            assertThatThrownBy(() -> client.applyEventSync(
                    new ApplyEventRequest("i1", "E", null, null, null, null, null)))
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> assertThat(RstmdbException.isGuardFailed(ex)).isTrue());
        }
    }

    @Test
    void errorConflict() throws IOException {
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.CONFLICT, "conflict", false));
        try (var client = connectToMock()) {
            assertThatThrownBy(() -> client.applyEventSync(
                    new ApplyEventRequest("i1", "E", null, null, null, null, null)))
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> assertThat(RstmdbException.isConflict(ex)).isTrue());
        }
    }

    @Test
    void retryableErrors() throws IOException {
        server.setRequestHandler(req -> new MockServer.MockError(
                ErrorCodes.RATE_LIMITED, "slow down", true));
        try (var client = connectToMock()) {
            assertThatThrownBy(client::pingSync)
                    .isInstanceOf(RstmdbException.class)
                    .satisfies(ex -> {
                        var re = (RstmdbException) ex;
                        assertThat(re.isRetryable()).isTrue();
                        assertThat(re.getErrorCode()).isEqualTo(ErrorCodes.RATE_LIMITED);
                    });
        }
    }

    @Test
    void requestTimeout() throws IOException {
        // Handler that never responds
        server.setRequestHandler(req -> {
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ignored) {
                // expected
            }
            return null;
        });
        try (var client = connectToMock(RstmdbOptions.builder()
                .requestTimeout(Duration.ofMillis(200)).build())) {
            assertThatThrownBy(client::pingSync)
                    .isInstanceOf(Exception.class);
        }
    }

    @Test
    void connectionClose() throws IOException {
        try (var client = connectToMock()) {
            client.close();
            assertThatThrownBy(client::pingSync)
                    .isInstanceOf(RstmdbException.class);
        }
    }

    @Test
    void connectionDrop() throws Exception {
        try (var client = connectToMock()) {
            // Force-close the server side to simulate connection drop
            server.close();
            // Wait for read loop to detect the drop
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThatThrownBy(client::pingSync)
                            .isInstanceOf(Exception.class));
        }
    }

    @Test
    void allErrorCodesExist() {
        // Verify all 16 error code constants are distinct non-empty strings
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
        assertThat(codes).hasSize(16);
        assertThat(codes).doesNotContainNull();
        assertThat(codes).doesNotHaveDuplicates();
    }
}
