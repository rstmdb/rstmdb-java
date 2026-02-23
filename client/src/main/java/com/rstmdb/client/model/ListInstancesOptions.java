package com.rstmdb.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ListInstancesOptions {
    private final String machine;
    private final String state;
    private final Integer limit;
    private final Integer offset;

    private ListInstancesOptions(Builder builder) {
        this.machine = builder.machine;
        this.state = builder.state;
        this.limit = builder.limit;
        this.offset = builder.offset;
    }

    public Map<String, Object> toParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        if (machine != null) {
            params.put("machine", machine);
        }
        if (state != null) {
            params.put("state", state);
        }
        if (limit != null) {
            params.put("limit", limit);
        }
        if (offset != null) {
            params.put("offset", offset);
        }
        return params;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String machine;
        private String state;
        private Integer limit;
        private Integer offset;

        private Builder() {}

        public Builder machine(String machine) {
            this.machine = machine;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public ListInstancesOptions build() {
            return new ListInstancesOptions(this);
        }
    }
}
