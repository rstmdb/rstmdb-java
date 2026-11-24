package com.rstmdb.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Base64;

/**
 * Result returned from BACKUP_CHUNK.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BackupChunkResult {
    @JsonProperty("bytes")
    private String bytes;

    @JsonProperty("eof")
    private boolean eof;

    /**
     * Decodes the base64 chunk payload into raw bytes.
     *
     * @return decoded byte array (empty if bytes is null or empty)
     */
    public byte[] decodeBytes() {
        if (bytes == null || bytes.isEmpty()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(bytes);
    }
}
