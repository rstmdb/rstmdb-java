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
public class WalReadResult {
    @JsonProperty("records")
    private List<WalRecord> records;
    
    @JsonProperty("next_offset")
    private long nextOffset;
}
