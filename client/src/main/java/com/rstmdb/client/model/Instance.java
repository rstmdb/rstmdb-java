package com.rstmdb.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instance {
    @JsonProperty("machine")
    private String machine;
    
    @JsonProperty("version")
    private int version;
    
    @JsonProperty("state")
    private String state;
    
    @JsonProperty("ctx")
    private Map<String, Object> ctx;
    
    @JsonProperty("last_event_id")
    private String lastEventId;
    
    @JsonProperty("last_wal_offset")
    private long lastWalOffset;
}
