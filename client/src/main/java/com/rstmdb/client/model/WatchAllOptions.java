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
public class WatchAllOptions {
    @JsonProperty("include_ctx")
    private Boolean includeCtx;
    
    @JsonProperty("from_offset")
    private Long fromOffset;
    
    @JsonProperty("machines")
    private List<String> machines;
    
    @JsonProperty("events")
    private List<String> events;
    
    @JsonProperty("from_states")
    private List<String> fromStates;
    
    @JsonProperty("to_states")
    private List<String> toStates;
}
