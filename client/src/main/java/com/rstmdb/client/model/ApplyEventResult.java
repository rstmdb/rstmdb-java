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
public class ApplyEventResult {
    @JsonProperty("from_state")
    private String fromState;
    
    @JsonProperty("to_state")
    private String toState;
    
    @JsonProperty("ctx")
    private Map<String, Object> ctx;
    
    @JsonProperty("wal_offset")
    private long walOffset;
    
    @JsonProperty("applied")
    private boolean applied;
    
    @JsonProperty("event_id")
    private String eventId;
}
