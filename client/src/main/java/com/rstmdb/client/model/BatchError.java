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
public class BatchError {
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("retryable")
    private boolean retryable;
}
