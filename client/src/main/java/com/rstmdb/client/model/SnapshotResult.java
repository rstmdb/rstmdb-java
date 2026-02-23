package com.rstmdb.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotResult {
    @JsonProperty("instance_id")
    private String instanceId;
    
    @JsonProperty("snapshot_id")
    private String snapshotId;
    
    @JsonProperty("wal_offset")
    private long walOffset;
    
    @JsonProperty("size_bytes")
    private Long sizeBytes;
    
    @JsonProperty("checksum")
    private String checksum;
}
