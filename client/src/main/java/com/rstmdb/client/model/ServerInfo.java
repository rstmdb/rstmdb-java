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
public class ServerInfo {
    @JsonProperty("server_name")
    private String serverName;
    
    @JsonProperty("server_version")
    private String serverVersion;
    
    @JsonProperty("protocol_version")
    private int protocolVersion;
    
    @JsonProperty("features")
    private List<String> features;
    
    @JsonProperty("max_frame_bytes")
    private int maxFrameBytes;
    
    @JsonProperty("max_batch_ops")
    private int maxBatchOps;
}
