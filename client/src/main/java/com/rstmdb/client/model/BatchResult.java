package com.rstmdb.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchResult {
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("result")
    private JsonNode result;
    
    @JsonProperty("error")
    private BatchError error;
}
