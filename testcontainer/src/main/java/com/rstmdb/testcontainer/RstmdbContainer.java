package com.rstmdb.testcontainer;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import static java.util.Objects.requireNonNull;

/**
 * Testcontainers {@link GenericContainer} for the {@code rstmdb/rstmdb} Docker image.
 * <p>
 * This container is designed to be used from tests (JUnit Jupiter, etc.) and therefore focuses on:
 * <ul>
 *   <li>minimal, deterministic defaults</li>
 *   <li>a robust readiness check (listen + CLI ping)</li>
 *   <li>easy access to host/port connection details</li>
 * </ul>
 *
 * <h2>Quick usage (JUnit Jupiter)</h2>
 *
 * <pre>{@code
 * @Testcontainers
 * class MyTest {
 *
 *   @Container
 *   static final RstmdbContainer rstmdb = new RstmdbContainer();
 *
 *   @Test
 *   void canConnect() throws Exception {
 *     // Use connection info:
 *     String host = rstmdb.getHost();
 *     int port = rstmdb.getRstmdbPort();
 *   }
 * }
 * }</pre>
 *
 * <h2>Image override</h2>
 * To support private registries and pre-release builds, the default image can be overridden:
 * <ul>
 *   <li>{@code -Drstmdb.testcontainers.image=rstmdb/rstmdb:main}</li>
 *   <li>{@code -Drstmdb.testcontainers.image=registry.example.com/rstmdb/rstmdb:1.2.3}</li>
 * </ul>
 *
 * <h2>Ports</h2>
 * The rstmdb image uses a server port {@value #RSTMDB_PORT} by default and (optionally) exposes a metrics port
 * {@value #METRICS_PORT}.
 */
public class RstmdbContainer extends GenericContainer<RstmdbContainer> {

    /** Default rstmdb server port inside the container. */
    public static final int RSTMDB_PORT = 7401;

    /** Default rstmdb metrics port inside the container. */
    public static final int METRICS_PORT = 9090;

    /** Default docker image (without tag). */
    public static final String DEFAULT_IMAGE = "rstmdb/rstmdb";

    /** Default docker tag. */
    public static final String DEFAULT_TAG = "latest";

    /** System property to override the docker image name (including optional tag). */
    public static final String IMAGE_PROPERTY = "rstmdb.testcontainers.image";

    private static final String DEFAULT_BIND = "0.0.0.0:" + RSTMDB_PORT;
    private static final String DEFAULT_DATA_DIR = "/data";
    private static final String DEFAULT_CONFIG_DIR = "/etc/rstmdb";

    private static final String CLI_PING_COMMAND = "rstmdb-cli -s localhost:" + RSTMDB_PORT + " ping";

    private final WaitAllStrategy waitAllStrategy = new WaitAllStrategy()
        .withStrategy(Wait.forListeningPort())
        .withStrategy(Wait.forSuccessfulCommand(CLI_PING_COMMAND))
        .withStartupTimeout(Duration.ofSeconds(90));

    private boolean metricsExposed;

    /**
     * Creates a container using either the {@link #IMAGE_PROPERTY} system property (if set),
     * or {@code rstmdb/rstmdb:latest}.
     */
    public RstmdbContainer() {
        this(resolveDefaultImage());
    }

    /**
     * Creates a container for the given docker image name.
     *
     * @param dockerImageName image name, including tag (recommended) or digest
     */
    public RstmdbContainer(String dockerImageName) {
        this(DockerImageName.parse(requireNonNull(dockerImageName, "dockerImageName should not be null")));
    }

    /**
     * Creates a container for the given docker image name.
     *
     * @param dockerImageName image name, including tag (recommended) or digest
     */
    public RstmdbContainer(DockerImageName dockerImageName) {
        super(requireNonNull(dockerImageName, "dockerImageName should not be null"));
        // Avoid breaking on future image renames; callers can still pass any compatible image.
        dockerImageName.assertCompatibleWith(DockerImageName.parse(DEFAULT_IMAGE));

        withExposedPorts(RSTMDB_PORT);
        withEnv("RSTMDB_BIND", DEFAULT_BIND);
        withEnv("RSTMDB_DATA", DEFAULT_DATA_DIR);
        withEnv("RUST_LOG", "info");

        // Robust startup: open port + a "ping" from inside the container.
        waitingFor(waitAllStrategy);
    }

    /**
     * Exposes the metrics port ({@value #METRICS_PORT}).
     * <p>
     * Call this <em>before</em> {@link #start()} (or use it in the field initializer when using JUnit Jupiter).
     *
     * @return this container
     */
    public RstmdbContainer withMetricsPort() {
        if (!metricsExposed) {
            addExposedPort(METRICS_PORT);
            metricsExposed = true;
        }
        return self();
    }

    /**
     * Enables the {@code FLUSH_ALL} operation on the server.
     * <p>
     * By default, the rstmdb server disables the flush-all operation for safety.
     * Call this method to set {@code RSTMDB_ALLOW_FLUSH_ALL=true}, which is typically
     * required for integration tests that need to reset state between runs.
     *
     * @return this container
     */
    public RstmdbContainer withFlushAll() {
        withEnv("RSTMDB_ALLOW_FLUSH_ALL", "true");
        return self();
    }

    /**
     * Enables token-hash based authentication.
     * <p>
     * The token hash must be a lowercase hex SHA-256 string.
     *
     * @param sha256TokenHash lowercase hex SHA-256 token hash
     * @return this container
     */
    public RstmdbContainer withAuthTokenHash(String sha256TokenHash) {
        requireNonNull(sha256TokenHash, "sha256TokenHash");
        withEnv("RSTMDB_AUTH_REQUIRED", "true");
        withEnv("RSTMDB_AUTH_TOKEN_HASH", sha256TokenHash);
        return self();
    }

    /**
     * Enables token-hash based authentication by hashing the provided token with SHA-256.
     * <p>
     * For real systems, consider passing a precomputed hash to avoid leaking raw tokens into
     * environment variables (logs, diagnostics). For tests, this is often acceptable.
     *
     * @param token token to hash with SHA-256 and configure as {@code RSTMDB_AUTH_TOKEN_HASH}
     * @return this container
     */
    public RstmdbContainer withAuthToken(String token) {
        return withAuthTokenHash(RstmdbAuth.sha256Hex(requireNonNull(token, "token")));
    }

    /**
     * Configures authentication using a secrets/tokens file.
     * <p>
     * This copies a file into the container and sets {@code RSTMDB_AUTH_REQUIRED=true} and
     * {@code RSTMDB_AUTH_SECRETS_FILE=/etc/rstmdb/tokens}.
     *
     * @param tokensFile file to copy into {@code /etc/rstmdb/tokens}
     * @return this container
     */
    public RstmdbContainer withAuthSecretsFile(MountableFile tokensFile) {
        requireNonNull(tokensFile, "tokensFile");
        withEnv("RSTMDB_AUTH_REQUIRED", "true");
        withCopyFileToContainer(tokensFile, DEFAULT_CONFIG_DIR + "/tokens");
        withEnv("RSTMDB_AUTH_SECRETS_FILE", DEFAULT_CONFIG_DIR + "/tokens");
        return self();
    }

    /**
     * Enables TLS by copying the provided certificate and key files into the container and setting the
     * relevant environment variables.
     *
     * @param serverPem certificate PEM file
     * @param serverKeyPem private key PEM file
     * @return this container
     */
    public RstmdbContainer withTls(MountableFile serverPem, MountableFile serverKeyPem) {
        requireNonNull(serverPem, "serverPem");
        requireNonNull(serverKeyPem, "serverKeyPem");

        String certDir = DEFAULT_CONFIG_DIR + "/certs";
        withCopyFileToContainer(serverPem, certDir + "/server.pem");
        withCopyFileToContainer(serverKeyPem, certDir + "/server-key.pem");

        withEnv("RSTMDB_TLS_ENABLED", "true");
        withEnv("RSTMDB_TLS_CERT", certDir + "/server.pem");
        withEnv("RSTMDB_TLS_KEY", certDir + "/server-key.pem");
        return self();
    }

    /**
     * Uses a custom rstmdb configuration file.
     * <p>
     * This copies the provided file to {@code /etc/rstmdb/config.yaml} and sets {@code RSTMDB_CONFIG}.
     *
     * @param configYaml config file (YAML) to copy into the container
     * @return this container
     */
    public RstmdbContainer withConfigYaml(MountableFile configYaml) {
        requireNonNull(configYaml, "configYaml");
        withCopyFileToContainer(configYaml, DEFAULT_CONFIG_DIR + "/config.yaml");
        withEnv("RSTMDB_CONFIG", DEFAULT_CONFIG_DIR + "/config.yaml");
        return self();
    }

    /**
     * Adjusts the overall startup timeout used by this container's readiness checks.
     *
     * @param startupTimeout timeout
     * @return this container
     */
    @Override
    public RstmdbContainer withStartupTimeout(Duration startupTimeout) {
        waitAllStrategy.withStartupTimeout(requireNonNull(startupTimeout, "startupTimeout"));
        return self();
    }

    /**
     * Returns the mapped server port on the Docker host.
     */
    public int getRstmdbPort() {
        return getMappedPort(RSTMDB_PORT);
    }

    /**
     * Returns the mapped metrics port on the Docker host.
     *
     * @return mapped metrics port
     * @throws IllegalStateException if metrics were not exposed via {@link #withMetricsPort()}
     */
    public int getMetricsPort() {
        if (!metricsExposed) {
            throw new IllegalStateException("Metrics port was not exposed. Call withMetricsPort() before start().");
        }
        return getMappedPort(METRICS_PORT);
    }

    /**
     * Returns connection details for this container.
     */
    public RstmdbEndpoint getEndpoint() {
        Integer metrics = metricsExposed ? getMappedPort(METRICS_PORT) : null;
        return new RstmdbEndpoint(getHost(), getRstmdbPort(), metrics);
    }

    /**
     * Convenience helper: create a client instance using the provided factory.
     * <p>
     * This is intentionally decoupled from any specific rstmdb Java client implementation.
     *
     * @param factory client factory
     * @param <T> client type
     * @return created client
     */
    public <T> T createClient(RstmdbClientFactory<T> factory) {
        requireNonNull(factory, "factory");
        return factory.create(getEndpoint());
    }

    private static String resolveDefaultImage() {
        String overridden = System.getProperty(IMAGE_PROPERTY);
        if (overridden != null && !overridden.isBlank()) {
            return overridden.trim();
        }
        return DEFAULT_IMAGE + ":" + DEFAULT_TAG;
    }
}
