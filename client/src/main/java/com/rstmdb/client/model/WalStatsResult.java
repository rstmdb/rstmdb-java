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
public class WalStatsResult {
    @JsonProperty("entry_count")
    private long entryCount;
    
    @JsonProperty("segment_count")
    private int segmentCount;
    
    @JsonProperty("total_size_bytes")
    private long totalSizeBytes;
    
    @JsonProperty("latest_offset")
    private long latestOffset;
    
    @JsonProperty("io_stats")
    private IoStats ioStats;
}
