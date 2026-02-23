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
public class MachineSummary {
    @JsonProperty("machine")
    private String machine;
    
    @JsonProperty("versions")
    private List<Integer> versions;
}
