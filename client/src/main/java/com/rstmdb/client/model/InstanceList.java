package com.rstmdb.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstanceList {
    @JsonProperty("instances")
    private List<InstanceSummary> instances;
    
    @JsonProperty("total")
    private int total;
    
    @JsonProperty("has_more")
    private boolean hasMore;
}
