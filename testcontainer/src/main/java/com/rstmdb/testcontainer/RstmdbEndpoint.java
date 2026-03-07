package com.rstmdb.testcontainer;

import java.net.URI;
import java.util.Objects;

/**
 * Connection details for an {@code rstmdb} server.
 * <p>
 * The rstmdb Docker image exposes a server port (default {@value RstmdbContainer#RSTMDB_PORT})
 * and an optional metrics port (default {@value RstmdbContainer#METRICS_PORT}).
 */
public record RstmdbEndpoint(String host, int port, Integer metricsPort) {

    /**
     * Creates an endpoint.
     *
     * @param host host name or IP address (typically returned by Testcontainers' {@code getHost()})
     * @param port mapped server port on {@code host}
     * @param metricsPort mapped metrics port on {@code host}, or {@code null} if not exposed
     */
    public RstmdbEndpoint {
        Objects.requireNonNull(host, "host");
        if (port <= 0) {
            throw new IllegalArgumentException("port must be > 0");
        }
        if (metricsPort != null && metricsPort <= 0) {
            throw new IllegalArgumentException("metricsPort must be > 0 when provided");
        }
    }

    /**
     * Returns {@code host:port}.
     */
    public String address() {
        return host + ":" + port;
    }

    /**
     * Returns an {@link URI} using the scheme {@code rstmdb}, e.g. {@code rstmdb://127.0.0.1:7401}.
     * <p>
     * This is a convenience; rstmdb itself may or may not use URI syntax.
     */
    public URI uri() {
        return URI.create("rstmdb://" + address());
    }
}
