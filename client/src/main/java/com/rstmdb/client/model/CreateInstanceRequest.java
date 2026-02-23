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
public class CreateInstanceRequest {
        @JsonProperty("instance_id")
        private String instanceId;

        @JsonProperty("machine")
        private String machine;

        @JsonProperty("version")
        private int version;

        @JsonProperty("initial_ctx")
        private Map<String, Object> initialCtx;

        @JsonProperty("idempotency_key")
        private String idempotencyKey;
}
