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
public class IoStats {
    @JsonProperty("bytes_written")
    private long bytesWritten;
    
    @JsonProperty("bytes_read")
    private long bytesRead;
    
    @JsonProperty("writes")
    private long writes;
    
    @JsonProperty("reads")
    private long reads;
    
    @JsonProperty("fsyncs")
    private long fsyncs;
}
