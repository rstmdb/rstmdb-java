package com.rstmdb.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Result returned from BACKUP_BEGIN.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BackupBeginResult {
    @JsonProperty("backup_id")
    private String backupId;

    @JsonProperty("manifest")
    private Map<String, Object> manifest;

    @JsonProperty("total_bytes")
    private long totalBytes;

    @JsonProperty("chunk_size")
    private long chunkSize;
}
