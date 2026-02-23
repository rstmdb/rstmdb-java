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
public class WalRecord {
    @JsonProperty("sequence")
    private long sequence;
    
    @JsonProperty("offset")
    private long offset;
    
    @JsonProperty("entry")
    private Object entry;
}
