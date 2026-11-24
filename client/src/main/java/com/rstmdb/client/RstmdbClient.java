package com.rstmdb.client;

import com.rstmdb.client.model.*;
import com.rstmdb.client.transport.Subscription;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RstmdbClient extends AutoCloseable {

    HelloResult getServerInfo();

    List<String> getNegotiatedFeatures();

    CompletableFuture<Void> ping();
    void pingSync();

    CompletableFuture<ServerInfo> getInfo();
    ServerInfo getInfoSync();

    CompletableFuture<ReplicationStatusResult> replicationStatus();
    ReplicationStatusResult replicationStatusSync();
    
    CompletableFuture<PutMachineResult> putMachine(PutMachineRequest request);
    PutMachineResult putMachineSync(PutMachineRequest request);
    
    CompletableFuture<MachineInfo> getMachine(String machine, int version);
    MachineInfo getMachineSync(String machine, int version);
    
    CompletableFuture<List<MachineSummary>> listMachines();
    List<MachineSummary> listMachinesSync();
    
    CompletableFuture<CreateInstanceResult> createInstance(CreateInstanceRequest request);
    CreateInstanceResult createInstanceSync(CreateInstanceRequest request);
    
    CompletableFuture<Instance> getInstance(String instanceId);
    Instance getInstanceSync(String instanceId);
    
    CompletableFuture<InstanceList> listInstances();
    CompletableFuture<InstanceList> listInstances(ListInstancesOptions options);
    InstanceList listInstancesSync();
    InstanceList listInstancesSync(ListInstancesOptions options);
    
    CompletableFuture<DeleteInstanceResult> deleteInstance(String instanceId);
    CompletableFuture<DeleteInstanceResult> deleteInstance(String instanceId, String idempotencyKey);
    DeleteInstanceResult deleteInstanceSync(String instanceId);
    
    CompletableFuture<ApplyEventResult> applyEvent(ApplyEventRequest request);
    ApplyEventResult applyEventSync(ApplyEventRequest request);
    
    CompletableFuture<List<BatchResult>> batch(BatchMode mode, BatchOperation... ops);
    List<BatchResult> batchSync(BatchMode mode, BatchOperation... ops);
    
    CompletableFuture<Subscription> watchInstance(WatchInstanceRequest request);
    Subscription watchInstanceSync(WatchInstanceRequest request);
    
    CompletableFuture<Subscription> watchAll();
    CompletableFuture<Subscription> watchAll(WatchAllOptions options);
    Subscription watchAllSync();
    Subscription watchAllSync(WatchAllOptions options);
    
    CompletableFuture<SnapshotResult> snapshotInstance(String instanceId);
    SnapshotResult snapshotInstanceSync(String instanceId);
    
    CompletableFuture<WalReadResult> walRead(long fromOffset);
    CompletableFuture<WalReadResult> walRead(long fromOffset, Integer limit);
    WalReadResult walReadSync(long fromOffset);
    
    CompletableFuture<WalStatsResult> walStats();
    WalStatsResult walStatsSync();
    
    CompletableFuture<CompactResult> compact();
    CompletableFuture<CompactResult> compact(boolean forceSnapshot);
    CompactResult compactSync();

    CompletableFuture<FlushAllResult> flushAll();
    FlushAllResult flushAllSync();

    CompletableFuture<BackupBeginResult> backupBegin();
    CompletableFuture<BackupBeginResult> backupBegin(String compression);
    BackupBeginResult backupBeginSync();
    BackupBeginResult backupBeginSync(String compression);

    CompletableFuture<BackupChunkResult> backupChunk(String backupId, long offset, long len);
    BackupChunkResult backupChunkSync(String backupId, long offset, long len);

    CompletableFuture<BackupEndResult> backupEnd(String backupId);
    BackupEndResult backupEndSync(String backupId);
}
