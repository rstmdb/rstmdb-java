package com.rstmdb.testcontainer;

/**
 * Factory interface for creating an rstmdb client given container connection details.
 * <p>
 * This library intentionally does not depend on a concrete rstmdb Java client implementation.
 * Consumers can provide their own factory (lambda, method reference, etc.).
 *
 * @param <T> client type
 */
@FunctionalInterface
public interface RstmdbClientFactory<T> {

    /**
     * Create a client instance for a running rstmdb server.
     *
     * @param endpoint host/port information for the started container
     * @return a client instance
     */
    T create(RstmdbEndpoint endpoint);
}
