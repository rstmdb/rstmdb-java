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
public class MachineInfo {
    @JsonProperty("definition")
    private MachineDefinition definition;
    
    @JsonProperty("checksum")
    private String checksum;
}
