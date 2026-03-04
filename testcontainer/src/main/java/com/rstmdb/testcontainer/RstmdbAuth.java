package com.rstmdb.testcontainer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Small helper utilities for rstmdb authentication configuration.
 * <p>
 * The rstmdb Docker image supports enabling auth via environment variables. When using
 * token-hash based configuration, the image expects a SHA-256 hash string.
 */
public final class RstmdbAuth {

    private RstmdbAuth() {
    }

    /**
     * Computes {@code SHA-256} and returns a lowercase hex string.
     *
     * @param value value to hash
     * @return lowercase hex SHA-256 of {@code value}
     */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Should never happen on a standard Java runtime.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
