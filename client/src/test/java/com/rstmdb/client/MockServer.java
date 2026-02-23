package com.rstmdb.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rstmdb.client.model.HelloResult;
import com.rstmdb.client.protocol.FrameCodec;
import com.rstmdb.client.protocol.WireMessage;
import com.rstmdb.client.protocol.WireMessage.WireError;
import com.rstmdb.client.protocol.WireMessage.WireEvent;
import com.rstmdb.client.protocol.WireMessage.WireRequest;
import com.rstmdb.client.protocol.WireMessage.WireResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * TCP mock server that speaks the RCPX binary protocol for testing.
 */
class MockServer implements AutoCloseable {

    private final ServerSocket serverSocket;
    private final ObjectMapper mapper = WireMessage.MAPPER;
    private volatile Function<WireRequest, Object> requestHandler;
    private final CopyOnWriteArrayList<Socket> connections = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OutputStream> outputStreams = new CopyOnWriteArrayList<>();
    private volatile boolean closed;
    private final Thread acceptThread;

    MockServer() throws IOException {
        this.serverSocket = new ServerSocket(0, 50, java.net.InetAddress.getLoopbackAddress());
        this.acceptThread = new Thread(this::acceptLoop, "mock-server-accept");
        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    int getPort() {
        return serverSocket.getLocalPort();
    }

    void setRequestHandler(Function<WireRequest, Object> handler) {
        this.requestHandler = handler;
    }

    /**
     * Send an event to all connected clients (for watch tests).
     */
    void sendEvent(WireEvent event) throws IOException {
        byte[] json = mapper.writeValueAsBytes(event);
        byte[] frame = FrameCodec.encode(json);
        for (OutputStream out : outputStreams) {
            try {
                synchronized (out) {
                    out.write(frame);
                    out.flush();
                }
            } catch (IOException ignored) {
                // best effort
            }
        }
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                Socket socket = serverSocket.accept();
                connections.add(socket);
                Thread handler = new Thread(() -> handleConnection(socket), "mock-server-handler");
                handler.setDaemon(true);
                handler.start();
            } catch (IOException e) {
                if (!closed) {
                    break;
                }
            }
        }
    }

    private void handleConnection(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            outputStreams.add(out);

            while (!closed && !socket.isClosed()) {
                byte[] payload;
                try {
                    payload = FrameCodec.decode(in);
                } catch (Exception e) {
                    break;
                }

                WireRequest req = mapper.readValue(payload, WireRequest.class);

                Object result;
                // Default handling for HELLO, AUTH, BYE
                switch (req.getOp()) {
                    case "HELLO": {
                        HelloResult hello = new HelloResult(1, "binary_json", "mock-rstmdb", "0.1.0",
                                List.of("watch", "batch"));
                        result = hello;
                        break;
                    }
                    case "AUTH": {
                        result = Map.of("authenticated", true);
                        break;
                    }
                    case "BYE": {
                        sendResponse(out, req.getId(), "ok", Map.of());
                        return;
                    }
                    default: {
                        if (requestHandler != null) {
                            result = requestHandler.apply(req);
                        } else {
                            result = Map.of();
                        }
                        break;
                    }
                }

                if (result instanceof MockError) {
                    MockError err = (MockError) result;
                    sendErrorResponse(out, req.getId(), err);
                } else if (result != null) {
                    sendResponse(out, req.getId(), "ok", result);
                }
            }
        } catch (Exception e) {
            // Connection closed
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
                // best effort
            }
        }
    }

    private void sendResponse(OutputStream out, String id, String status, Object result) throws IOException {
        var resultNode = mapper.valueToTree(result);
        var resp = new WireResponse("response", id, status, resultNode, null, null);
        byte[] json = mapper.writeValueAsBytes(resp);
        byte[] frame = FrameCodec.encode(json);
        synchronized (out) {
            out.write(frame);
            out.flush();
        }
    }

    private void sendErrorResponse(OutputStream out, String id, MockError err) throws IOException {
        var wireErr = new WireError(err.getCode(), err.getMessage(), err.isRetryable(), null);
        var resp = new WireResponse("response", id, "error", null, wireErr, null);
        byte[] json = mapper.writeValueAsBytes(resp);
        byte[] frame = FrameCodec.encode(json);
        synchronized (out) {
            out.write(frame);
            out.flush();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class MockError {
        private String code;
        private String message;
        private boolean retryable;
    }

    @Override
    public void close() {
        closed = true;
        try {
            serverSocket.close();
        } catch (Exception ignored) {
            // best effort
        }
        for (Socket s : connections) {
            try {
                s.close();
            } catch (Exception ignored) {
                // best effort
            }
        }
    }
}
