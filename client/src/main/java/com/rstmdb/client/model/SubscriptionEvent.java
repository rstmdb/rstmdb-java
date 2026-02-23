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
public class SubscriptionEvent {
    @JsonProperty("subscription_id")
    private String subscriptionId;
    
    @JsonProperty("instance_id")
    private String instanceId;
    
    @JsonProperty("machine")
    private String machine;
    
    @JsonProperty("version")
    private int version;
    
    @JsonProperty("wal_offset")
    private long walOffset;
    
    @JsonProperty("from_state")
    private String fromState;
    
    @JsonProperty("to_state")
    private String toState;
    
    @JsonProperty("event")
    private String event;
    
    @JsonProperty("payload")
    private Map<String, Object> payload;
    
    @JsonProperty("ctx")
    private Map<String, Object> ctx;
}
