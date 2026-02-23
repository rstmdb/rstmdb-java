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
public class ApplyEventRequest {
    @JsonProperty("instance_id")
    private String instanceId;
    
    @JsonProperty("event")
    private String event;
    
    @JsonProperty("payload")
    private Map<String, Object> payload;
    
    @JsonProperty("expected_state")
    private String expectedState;
    
    @JsonProperty("expected_wal_offset")
    private Long expectedWalOffset;
    
    @JsonProperty("event_id")
    private String eventId;
    
    @JsonProperty("idempotency_key")
    private String idempotencyKey;
}
