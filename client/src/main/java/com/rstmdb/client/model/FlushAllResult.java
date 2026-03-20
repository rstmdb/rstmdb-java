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
public class FlushAllResult {
    private boolean flushed;

    @JsonProperty("instances_removed")
    private int instancesRemoved;

    @JsonProperty("machines_removed")
    private int machinesRemoved;
}
