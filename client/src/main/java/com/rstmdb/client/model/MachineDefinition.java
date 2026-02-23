package com.rstmdb.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineDefinition {
    @JsonProperty("states")
    private List<String> states;
    
    @JsonProperty("initial")
    private String initial;
    
    @JsonProperty("transitions")
    private List<Transition> transitions;
    
    @JsonProperty("meta")
    private Map<String, Object> meta;
}
