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
public class PutMachineResult {
    @JsonProperty("machine")
    private String machine;
    
    @JsonProperty("version")
    private int version;
    
    @JsonProperty("stored_checksum")
    private String storedChecksum;
    
    @JsonProperty("created")
    private boolean created;
}
