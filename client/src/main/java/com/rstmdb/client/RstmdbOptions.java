package com.rstmdb.client;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Connection options for the rstmdb client. Use {@link #builder()} to construct.
 */
public final class RstmdbOptions {
    private final String auth;
    private final SSLContext sslContext;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final String clientName;
    private final String wireMode;
    private final String[] features;

    private RstmdbOptions(Builder builder) {
        this.auth = builder.auth;
        this.sslContext = builder.sslContext;
        this.connectTimeout = builder.connectTimeout;
        this.requestTimeout = builder.requestTimeout;
        this.clientName = builder.clientName;
        this.wireMode = builder.wireMode;
        this.features = builder.features;
    }

    public String getAuth() {
        return auth;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public String getClientName() {
        return clientName;
    }

    public String getWireMode() {
        return wireMode;
    }

    public String[] getFeatures() {
        return features;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String auth;
        private SSLContext sslContext;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout = Duration.ofSeconds(30);
        private String clientName;
        private String wireMode = "binary_json";
        private String[] features;

        private Builder() {}

        public Builder auth(String token) {
            this.auth = token;
            return this;
        }

        public Builder sslContext(SSLContext ctx) {
            this.sslContext = ctx;
            return this;
        }

        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        public Builder requestTimeout(Duration timeout) {
            this.requestTimeout = timeout;
            return this;
        }

        public Builder clientName(String name) {
            this.clientName = name;
            return this;
        }

        public Builder wireMode(String mode) {
            this.wireMode = mode;
            return this;
        }

        public Builder features(String... features) {
            this.features = features;
            return this;
        }

        public RstmdbOptions build() {
            return new RstmdbOptions(this);
        }
    }

    /**
     * Create a TLS context using a CA certificate file.
     */
    public static SSLContext createTlsContext(Path caFile) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            try (InputStream is = Files.newInputStream(caFile)) {
                ks.setCertificateEntry("ca", cf.generateCertificate(is));
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create TLS context", e);
        }
    }

    /**
     * Create an insecure TLS context that skips certificate verification. Development only.
     */
    @SuppressWarnings("java:S4830")
    public static SSLContext insecureTlsContext() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        // intentionally empty — insecure mode
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        // intentionally empty — insecure mode
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            }, new SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create insecure TLS context", e);
        }
    }
}
