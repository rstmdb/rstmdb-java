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
public class WatchInstanceRequest {
    @JsonProperty("instance_id")
    private String instanceId;
    
    @JsonProperty("include_ctx")
    private boolean includeCtx;
    
    @JsonProperty("from_offset")
    private long fromOffset;
}
