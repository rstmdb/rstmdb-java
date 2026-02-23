package com.rstmdb.client.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rstmdb.client.RstmdbOptions;
import com.rstmdb.client.exception.ErrorCodes;
import com.rstmdb.client.exception.RstmdbException;
import com.rstmdb.client.model.HelloResult;
import com.rstmdb.client.protocol.FrameCodec;
import com.rstmdb.client.protocol.Operations;
import com.rstmdb.client.protocol.WireMessage;
import com.rstmdb.client.protocol.WireMessage.WireEvent;
import com.rstmdb.client.protocol.WireMessage.WireRequest;
import com.rstmdb.client.protocol.WireMessage.WireResponse;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Core transport layer managing TCP connection, multiplexing, and read loop.
 */
public class Connection {

    private final Socket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final ReentrantLock writeLock = new ReentrantLock();
    private final ConcurrentHashMap<String, CompletableFuture<WireResponse>> pending = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(0);
    private final ObjectMapper objectMapper = WireMessage.MAPPER;
    private final Duration requestTimeout;
    private volatile boolean closed;
    private String wireMode;
    private HelloResult helloResult;

    private Connection(Socket socket, InputStream inputStream, OutputStream outputStream, Duration requestTimeout) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.requestTimeout = requestTimeout;
        this.wireMode = "binary_json";
    }

    /**
     * Connect, handshake, start read loop.
     */
    public static Connection connect(String host, int port, RstmdbOptions opts) throws IOException {
        if (opts == null) {
            opts = RstmdbOptions.builder().build();
        }

        Duration connectTimeout = opts.getConnectTimeout();

        Socket socket;
        if (opts.getSslContext() != null) {
            SSLSocketFactory factory = opts.getSslContext().getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) factory.createSocket();
            sslSocket.connect(new InetSocketAddress(host, port), (int) connectTimeout.toMillis());
            sslSocket.startHandshake();
            socket = sslSocket;
        } else {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), (int) connectTimeout.toMillis());
        }

        socket.setTcpNoDelay(true);

        InputStream in = new BufferedInputStream(socket.getInputStream(), 8192);
        OutputStream out = new BufferedOutputStream(socket.getOutputStream());

        Connection conn = new Connection(socket, in, out, opts.getRequestTimeout());

        try {
            conn.handshake(opts);
        } catch (Exception e) {
            try {
                socket.close();
            } catch (Exception ignored) {
                // best effort
            }
            if (e instanceof IOException) {
                throw e;
            }
            throw new IOException("Handshake failed", e);
        }

        // Start read loop daemon thread
        Thread readThread = new Thread(conn::readLoop, "rstmdb-readloop");
        readThread.setDaemon(true);
        readThread.start();

        return conn;
    }

    private void handshake(RstmdbOptions opts) throws IOException {
        // Build HELLO params
        Map<String, Object> helloParams = new LinkedHashMap<>();
        helloParams.put("protocol_version", 1);
        helloParams.put("wire_mode", opts.getWireMode() != null ? opts.getWireMode() : "binary_json");
        if (opts.getClientName() != null) {
            helloParams.put("client_name", opts.getClientName());
        }
        if (opts.getFeatures() != null) {
            helloParams.put("features", opts.getFeatures());
        }

        // Send HELLO (always binary_json framing)
        String helloId = String.valueOf(nextId.incrementAndGet());
        WireRequest helloReq = WireRequest.of(helloId, Operations.HELLO, helloParams);
        writeFrame(helloReq);

        // Read HELLO response (synchronous, before read loop)
        byte[] responsePayload = FrameCodec.decode(inputStream);
        WireResponse helloResp = objectMapper.readValue(responsePayload, WireResponse.class);

        if ("error".equals(helloResp.getStatus()) && helloResp.getError() != null) {
            throw new RstmdbException(
                    helloResp.getError().getCode(),
                    helloResp.getError().getMessage(),
                    helloResp.getError().isRetryable(),
                    helloResp.getError().getDetails());
        }

        this.helloResult = objectMapper.treeToValue(helloResp.getResult(), HelloResult.class);

        // Switch to server-negotiated wire mode
        if (helloResult.getWireMode() != null) {
            this.wireMode = helloResult.getWireMode();
        }

        // AUTH if token provided
        if (opts.getAuth() != null && !opts.getAuth().isEmpty()) {
            String authId = String.valueOf(nextId.incrementAndGet());
            Map<String, Object> authParams = Map.of("token", opts.getAuth());
            WireRequest authReq = WireRequest.of(authId, Operations.AUTH, authParams);
            writeFrame(authReq);

            byte[] authPayload = FrameCodec.decode(inputStream);
            WireResponse authResp = objectMapper.readValue(authPayload, WireResponse.class);

            if ("error".equals(authResp.getStatus()) && authResp.getError() != null) {
                throw new RstmdbException(
                        authResp.getError().getCode(),
                        authResp.getError().getMessage(),
                        authResp.getError().isRetryable(),
                        authResp.getError().getDetails());
            }
        }
    }

    /**
     * Send a request and return a future for the response.
     */
    public CompletableFuture<WireResponse> sendRequest(String op, Object params) {
        if (closed) {
            return CompletableFuture.failedFuture(
                    new RstmdbException(ErrorCodes.INTERNAL_ERROR, "Connection closed", false, null));
        }

        String id = String.valueOf(nextId.incrementAndGet());
        WireRequest req = WireRequest.of(id, op, params);

        CompletableFuture<WireResponse> future = new CompletableFuture<>();
        pending.put(id, future);

        try {
            if ("jsonl".equals(wireMode)) {
                writeJsonl(req);
            } else {
                writeFrame(req);
            }
        } catch (IOException e) {
            pending.remove(id);
            future.completeExceptionally(
                    new RstmdbException(ErrorCodes.INTERNAL_ERROR,
                            "Write failed: " + e.getMessage(), false, null));
            return future;
        }

        // Apply request timeout
        future.orTimeout(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);

        return future;
    }

    private void writeFrame(Object message) throws IOException {
        byte[] json = objectMapper.writeValueAsBytes(message);
        byte[] frame = FrameCodec.encode(json);
        writeLock.lock();
        try {
            outputStream.write(frame);
            outputStream.flush();
        } finally {
            writeLock.unlock();
        }
    }

    private void writeJsonl(Object message) throws IOException {
        byte[] json = objectMapper.writeValueAsBytes(message);
        writeLock.lock();
        try {
            outputStream.write(json);
            outputStream.write('\n');
            outputStream.flush();
        } finally {
            writeLock.unlock();
        }
    }

    private void readLoop() {
        try {
            while (!closed) {
                byte[] payload;
                if ("jsonl".equals(wireMode)) {
                    payload = readJsonlMessage();
                } else {
                    payload = FrameCodec.decode(inputStream);
                }

                JsonNode root = objectMapper.readTree(payload);
                JsonNode typeNode = root.get("type");
                if (typeNode == null) {
                    continue;
                }
                String type = typeNode.asText();

                switch (type) {
                    case "response": {
                        WireResponse resp = objectMapper.treeToValue(root, WireResponse.class);
                        CompletableFuture<WireResponse> future = pending.remove(resp.getId());
                        if (future != null) {
                            future.complete(resp);
                        }
                        break;
                    }
                    case "event": {
                        WireEvent evt = objectMapper.treeToValue(root, WireEvent.class);
                        Subscription sub = subscriptions.get(evt.getSubscriptionId());
                        if (sub != null) {
                            sub.enqueue(evt);
                        }
                        break;
                    }
                    default: {
                        // ignore unknown message types
                        break;
                    }
                }
            }
        } catch (Exception e) {
            if (!closed) {
                closeWithError(e);
            }
        }
    }

    private byte[] readJsonlMessage() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        int b;
        while ((b = inputStream.read()) != -1) {
            if (b == '\n') {
                break;
            }
            baos.write(b);
        }
        if (b == -1 && baos.size() == 0) {
            throw new IOException("Connection closed");
        }
        return baos.toByteArray();
    }

    public void registerSubscription(String id, Subscription sub) {
        subscriptions.put(id, sub);
    }

    public void removeSubscription(String id) {
        subscriptions.remove(id);
    }

    public HelloResult getHelloResult() {
        return helloResult;
    }

    /**
     * Graceful close: BYE, close socket, complete pending futures, close
     * subscriptions.
     */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        // Best-effort BYE
        try {
            String byeId = String.valueOf(nextId.incrementAndGet());
            WireRequest bye = WireRequest.of(byeId, Operations.BYE, null);
            writeFrame(bye);
        } catch (Exception ignored) {
            // best effort
        }

        // Close socket (unblocks read loop)
        try {
            socket.close();
        } catch (Exception ignored) {
            // best effort
        }

        // Complete all pending futures exceptionally
        var ex = new RstmdbException(ErrorCodes.INTERNAL_ERROR, "Connection closed", false, null);
        pending.forEach((id, future) -> future.completeExceptionally(ex));
        pending.clear();

        // Close all subscriptions
        subscriptions.forEach((id, sub) -> sub.closeFromConnection());
        subscriptions.clear();
    }

    private void closeWithError(Exception cause) {
        if (closed) {
            return;
        }
        closed = true;

        try {
            socket.close();
        } catch (Exception ignored) {
            // best effort
        }

        var ex = new RstmdbException(ErrorCodes.INTERNAL_ERROR,
                "Connection lost: " + cause.getMessage(), false, null);
        pending.forEach((id, future) -> future.completeExceptionally(ex));
        pending.clear();

        subscriptions.forEach((id, sub) -> sub.closeFromConnection());
        subscriptions.clear();
    }

    public boolean isClosed() {
        return closed;
    }
}
