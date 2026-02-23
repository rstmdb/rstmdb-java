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
public class CompactResult {
    @JsonProperty("snapshots_created")
    private int snapshotsCreated;
    
    @JsonProperty("segments_deleted")
    private int segmentsDeleted;
    
    @JsonProperty("bytes_reclaimed")
    private long bytesReclaimed;
    
    @JsonProperty("total_snapshots")
    private int totalSnapshots;
    
    @JsonProperty("wal_segments")
    private int walSegments;
}
