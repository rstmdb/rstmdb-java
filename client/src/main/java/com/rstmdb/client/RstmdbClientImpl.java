package com.rstmdb.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rstmdb.client.exception.RstmdbException;
import com.rstmdb.client.model.ApplyEventRequest;
import com.rstmdb.client.model.ApplyEventResult;
import com.rstmdb.client.model.BatchMode;
import com.rstmdb.client.model.BatchOperation;
import com.rstmdb.client.model.BatchResult;
import com.rstmdb.client.model.CompactResult;
import com.rstmdb.client.model.CreateInstanceRequest;
import com.rstmdb.client.model.CreateInstanceResult;
import com.rstmdb.client.model.DeleteInstanceResult;
import com.rstmdb.client.model.HelloResult;
import com.rstmdb.client.model.Instance;
import com.rstmdb.client.model.InstanceList;
import com.rstmdb.client.model.ListInstancesOptions;
import com.rstmdb.client.model.MachineInfo;
import com.rstmdb.client.model.MachineSummary;
import com.rstmdb.client.model.PutMachineRequest;
import com.rstmdb.client.model.PutMachineResult;
import com.rstmdb.client.model.ServerInfo;
import com.rstmdb.client.model.SnapshotResult;
import com.rstmdb.client.model.WalReadResult;
import com.rstmdb.client.model.WalStatsResult;
import com.rstmdb.client.model.WatchAllOptions;
import com.rstmdb.client.model.WatchInstanceRequest;
import com.rstmdb.client.protocol.Operations;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import com.rstmdb.client.protocol.WireMessage;
import com.rstmdb.client.protocol.WireMessage.WireResponse;
import com.rstmdb.client.transport.Connection;
import com.rstmdb.client.transport.Subscription;

/**
 * Official Java client for rstmdb. Thread-safe, supports concurrent operations
 * on a single TCP connection.
 *
 * <p>Usage:
 * <pre>{@code
 * try (var client = RstmdbClient.connect("localhost", 7401)) {
 *     client.pingSync();
 * }
 * }</pre>
 */
public final class RstmdbClientImpl implements RstmdbClient {

    private final Connection connection;
    private final ObjectMapper objectMapper = WireMessage.MAPPER;

    private RstmdbClientImpl(Connection connection) {
        this.connection = connection;
    }

    // --- Factory ---

    public static RstmdbClientImpl connect(String host, int port) throws IOException {
        return connect(host, port, null);
    }

    public static RstmdbClientImpl connect(String host, int port, RstmdbOptions options) throws IOException {
        Connection conn = Connection.connect(host, port, options);
        return new RstmdbClientImpl(conn);
    }

    public static CompletableFuture<RstmdbClientImpl> connectAsync(String host, int port) {
        return connectAsync(host, port, null);
    }

    public static CompletableFuture<RstmdbClientImpl> connectAsync(String host, int port, RstmdbOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return connect(host, port, options);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    // --- Server Info ---

    public HelloResult getServerInfo() {
        return connection.getHelloResult();
    }

    // --- System Operations ---

    public CompletableFuture<Void> ping() {
        return doRequest(Operations.PING, null, Void.class);
    }

    public void pingSync() {
        sync(ping());
    }

    public CompletableFuture<ServerInfo> getInfo() {
        return doRequest(Operations.INFO, null, ServerInfo.class);
    }

    public ServerInfo getInfoSync() {
        return sync(getInfo());
    }

    // --- Machine Operations ---

    public CompletableFuture<PutMachineResult> putMachine(PutMachineRequest request) {
        return doRequest(Operations.PUT_MACHINE, request, PutMachineResult.class);
    }

    public PutMachineResult putMachineSync(PutMachineRequest request) {
        return sync(putMachine(request));
    }

    public CompletableFuture<MachineInfo> getMachine(String machine, int version) {
        Map<String, Object> params = Map.of("machine", machine, "version", version);
        return doRequest(Operations.GET_MACHINE, params, MachineInfo.class);
    }

    public MachineInfo getMachineSync(String machine, int version) {
        return sync(getMachine(machine, version));
    }

    public CompletableFuture<List<MachineSummary>> listMachines() {
        return doRequest(Operations.LIST_MACHINES, null, JsonNode.class)
                .thenApply(node -> {
                    try {
                        JsonNode items = node.get("items");
                        if (items != null) {
                            return objectMapper.treeToValue(items, objectMapper.getTypeFactory().constructCollectionType(List.class, MachineSummary.class));
                        }
                        return objectMapper.treeToValue(node, objectMapper.getTypeFactory().constructCollectionType(List.class, MachineSummary.class));
                    } catch (JsonProcessingException e) {
                        throw new CompletionException(e);
                    }
                });
    }

    public List<MachineSummary> listMachinesSync() {
        return sync(listMachines());
    }

    // --- Instance Operations ---

    public CompletableFuture<CreateInstanceResult> createInstance(CreateInstanceRequest request) {
        return doRequest(Operations.CREATE_INSTANCE, request, CreateInstanceResult.class);
    }

    public CreateInstanceResult createInstanceSync(CreateInstanceRequest request) {
        return sync(createInstance(request));
    }

    public CompletableFuture<Instance> getInstance(String instanceId) {
        return doRequest(Operations.GET_INSTANCE, Map.of("instance_id", instanceId), Instance.class);
    }

    public Instance getInstanceSync(String instanceId) {
        return sync(getInstance(instanceId));
    }

    public CompletableFuture<InstanceList> listInstances() {
        return doRequest(Operations.LIST_INSTANCES, null, InstanceList.class);
    }

    public CompletableFuture<InstanceList> listInstances(ListInstancesOptions options) {
        return doRequest(Operations.LIST_INSTANCES,
                options != null ? options.toParams() : null, InstanceList.class);
    }

    public InstanceList listInstancesSync() {
        return sync(listInstances());
    }

    public InstanceList listInstancesSync(ListInstancesOptions options) {
        return sync(listInstances(options));
    }

    public CompletableFuture<DeleteInstanceResult> deleteInstance(String instanceId) {
        return deleteInstance(instanceId, null);
    }

    public CompletableFuture<DeleteInstanceResult> deleteInstance(String instanceId, String idempotencyKey) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("instance_id", instanceId);
        if (idempotencyKey != null) {
            params.put("idempotency_key", idempotencyKey);
        }
        return doRequest(Operations.DELETE_INSTANCE, params, DeleteInstanceResult.class);
    }

    public DeleteInstanceResult deleteInstanceSync(String instanceId) {
        return sync(deleteInstance(instanceId));
    }

    // --- Event Operations ---

    public CompletableFuture<ApplyEventResult> applyEvent(ApplyEventRequest request) {
        return doRequest(Operations.APPLY_EVENT, request, ApplyEventResult.class);
    }

    public ApplyEventResult applyEventSync(ApplyEventRequest request) {
        return sync(applyEvent(request));
    }

    public CompletableFuture<List<BatchResult>> batch(BatchMode mode, BatchOperation... ops) {
        Map<String, Object> params = Map.of("mode", mode, "ops", ops);
        return doRequest(Operations.BATCH, params, JsonNode.class)
                .thenApply(node -> {
                    try {
                        JsonNode results = node.get("results");
                        if (results != null) {
                            return objectMapper.treeToValue(results, objectMapper.getTypeFactory().constructCollectionType(List.class, BatchResult.class));
                        }
                        return objectMapper.treeToValue(node, objectMapper.getTypeFactory().constructCollectionType(List.class, BatchResult.class));
                    } catch (JsonProcessingException e) {
                        throw new CompletionException(e);
                    }
                });
    }

    public List<BatchResult> batchSync(BatchMode mode, BatchOperation... ops) {
        return sync(batch(mode, ops));
    }

    // --- Watch/Subscription Operations ---

    public CompletableFuture<Subscription> watchInstance(WatchInstanceRequest request) {
        Subscription sub = new Subscription(connection);
        return connection.sendRequest(Operations.WATCH_INSTANCE, request)
                .thenApply(resp -> {
                    checkError(resp);
                    String subId = resp.getResult().get("subscription_id").asText();
                    sub.setId(subId);
                    connection.registerSubscription(subId, sub);
                    return sub;
                });
    }

    public Subscription watchInstanceSync(WatchInstanceRequest request) {
        return sync(watchInstance(request));
    }

    public CompletableFuture<Subscription> watchAll() {
        return watchAll(new WatchAllOptions(null, null, null, null, null, null));
    }

    public CompletableFuture<Subscription> watchAll(WatchAllOptions options) {
        Subscription sub = new Subscription(connection);
        Object params = options != null ? options : new WatchAllOptions(null, null, null, null, null, null);
        return connection.sendRequest(Operations.WATCH_ALL, params)
                .thenApply(resp -> {
                    checkError(resp);
                    String subId = resp.getResult().get("subscription_id").asText();
                    sub.setId(subId);
                    connection.registerSubscription(subId, sub);
                    return sub;
                });
    }

    public Subscription watchAllSync() {
        return sync(watchAll());
    }

    public Subscription watchAllSync(WatchAllOptions options) {
        return sync(watchAll(options));
    }

    // --- WAL Operations ---

    public CompletableFuture<SnapshotResult> snapshotInstance(String instanceId) {
        return doRequest(Operations.SNAPSHOT_INSTANCE,
                Map.of("instance_id", instanceId), SnapshotResult.class);
    }

    public SnapshotResult snapshotInstanceSync(String instanceId) {
        return sync(snapshotInstance(instanceId));
    }

    public CompletableFuture<WalReadResult> walRead(long fromOffset) {
        return walRead(fromOffset, null);
    }

    public CompletableFuture<WalReadResult> walRead(long fromOffset, Integer limit) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("from_offset", fromOffset);
        if (limit != null) {
            params.put("limit", limit);
        }
        return doRequest(Operations.WAL_READ, params, WalReadResult.class);
    }

    public WalReadResult walReadSync(long fromOffset) {
        return sync(walRead(fromOffset));
    }

    public CompletableFuture<WalStatsResult> walStats() {
        return doRequest(Operations.WAL_STATS, null, WalStatsResult.class);
    }

    public WalStatsResult walStatsSync() {
        return sync(walStats());
    }

    public CompletableFuture<CompactResult> compact() {
        return compact(false);
    }

    public CompletableFuture<CompactResult> compact(boolean forceSnapshot) {
        Map<String, Object> params = forceSnapshot ? Map.of("force_snapshot", true) : null;
        return doRequest(Operations.COMPACT, params, CompactResult.class);
    }

    public CompactResult compactSync() {
        return sync(compact());
    }

    // --- Lifecycle ---

    @Override
    public void close() {
        connection.close();
    }

    // --- Internal ---

    private <T> CompletableFuture<T> doRequest(String op, Object params, Class<T> resultType) {
        return connection.sendRequest(op, params)
                .thenApply(resp -> {
                    checkError(resp);
                    if (resultType == Void.class) {
                        return null;
                    }
                    try {
                        if (resp.getResult() == null || resp.getResult().isNull()) {
                            return null;
                        }
                        return objectMapper.treeToValue(resp.getResult(), resultType);
                    } catch (JsonProcessingException e) {
                        throw new CompletionException(e);
                    }
                });
    }

    private void checkError(WireResponse resp) {
        if ("error".equals(resp.getStatus()) && resp.getError() != null) {
            throw new RstmdbException(
                    resp.getError().getCode(),
                    resp.getError().getMessage(),
                    resp.getError().isRetryable(),
                    resp.getError().getDetails()
            );
        }
    }

    private <T> T sync(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RstmdbException) {
                throw (RstmdbException) e.getCause();
            }
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        }
    }
}
