package com.rstmdb.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum BatchMode {
    @JsonProperty("atomic") ATOMIC,
    @JsonProperty("best_effort") BEST_EFFORT;
}
