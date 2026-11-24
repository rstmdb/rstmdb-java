package com.rstmdb.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Result returned from REPLICATION_STATUS.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReplicationStatusResult {
    @JsonProperty("role")
    private String role;

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("primary_sequence")
    private Long primarySequence;

    @JsonProperty("connected_replicas")
    private Integer connectedReplicas;

    @JsonProperty("replicas")
    private List<Map<String, Object>> replicas;

    @JsonProperty("upstream")
    private String upstream;

    @JsonProperty("last_applied_sequence")
    private Long lastAppliedSequence;

    @JsonProperty("lag_entries")
    private Long lagEntries;

    @JsonProperty("lag_seconds")
    private Double lagSeconds;
}
