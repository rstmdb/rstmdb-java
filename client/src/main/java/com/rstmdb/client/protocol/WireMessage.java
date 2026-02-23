package com.rstmdb.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Internal wire-level message types for JSON serialization.
 */
public final class WireMessage {

    private WireMessage() {}

    /**
     * Shared, immutable ObjectMapper configured for the wire protocol.
     */
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WireRequest {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("op")
        private String op;
        
        @JsonProperty("params")
        private Object params;
        
        public static WireRequest of(String id, String op, Object params) {
            return new WireRequest("request", id, op, params);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WireResponse {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("result")
        private JsonNode result;
        
        @JsonProperty("error")
        private WireError error;
        
        @JsonProperty("meta")
        private WireMeta meta;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WireError {
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("retryable")
        private boolean retryable;
        
        @JsonProperty("details")
        private Map<String, Object> details;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WireMeta {
        @JsonProperty("server_time")
        private String serverTime;
        
        @JsonProperty("leader")
        private Boolean leader;
        
        @JsonProperty("wal_offset")
        private Long walOffset;
        
        @JsonProperty("trace_id")
        private String traceId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WireEvent {
        @JsonProperty("type")
        private String type;
        
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
}
