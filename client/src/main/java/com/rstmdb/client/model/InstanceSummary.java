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
public class InstanceSummary {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("machine")
    private String machine;
    
    @JsonProperty("version")
    private int version;
    
    @JsonProperty("state")
    private String state;
    
    @JsonProperty("created_at")
    private long createdAt;
    
    @JsonProperty("updated_at")
    private long updatedAt;
    
    @JsonProperty("last_wal_offset")
    private long lastWalOffset;
}
