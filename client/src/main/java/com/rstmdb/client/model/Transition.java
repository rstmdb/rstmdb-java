package com.rstmdb.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transition {
    @JsonProperty("from")
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private List<String> from;
    
    @JsonProperty("event")
    private String event;
    
    @JsonProperty("to")
    private String to;
    
    @JsonProperty("guard")
    private String guard;
}
