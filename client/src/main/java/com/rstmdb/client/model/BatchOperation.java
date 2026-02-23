package com.rstmdb.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchOperation {
    @JsonProperty("op")
    private String op;
    
    @JsonProperty("params")
    private Object params;
    
    public static BatchOperation createInstance(CreateInstanceRequest req) {
        return new BatchOperation("CREATE_INSTANCE", req);
    }

    public static BatchOperation applyEvent(ApplyEventRequest req) {
        return new BatchOperation("APPLY_EVENT", req);
    }

    public static BatchOperation deleteInstance(String instanceId) {
        return new BatchOperation("DELETE_INSTANCE", Map.of("instance_id", instanceId));
    }
}
