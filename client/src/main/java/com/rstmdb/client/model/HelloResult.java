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
public class HelloResult {
    @JsonProperty("protocol_version")
    private int protocolVersion;
    
    @JsonProperty("wire_mode")
    private String wireMode;
    
    @JsonProperty("server_name")
    private String serverName;
    
    @JsonProperty("server_version")
    private String serverVersion;
    
    @JsonProperty("features")
    private List<String> features;
}
