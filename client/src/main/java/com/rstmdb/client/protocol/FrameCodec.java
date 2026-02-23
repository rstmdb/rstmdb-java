package com.rstmdb.client.protocol;

import com.rstmdb.client.exception.ErrorCodes;
import com.rstmdb.client.exception.RstmdbException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32C;

/**
 * RCPX binary frame encoder/decoder with CRC32C validation.
 *
 * <p>Frame header layout (18 bytes, big-endian):
 * <pre>
 * Offset  Size  Field
 * 0-3     4     Magic ("RCPX")
 * 4-5     2     Version (1)
 * 6-7     2     Flags
 * 8-9     2     Header extension length (0 in v1)
 * 10-13   4     Payload length
 * 14-17   4     CRC32C of payload
 * </pre>
 */
public final class FrameCodec {

    static final byte[] MAGIC = "RCPX".getBytes(StandardCharsets.US_ASCII);
    public static final int HEADER_SIZE = 18;
    public static final int PROTOCOL_VERSION = 1;
    public static final int MAX_PAYLOAD_SIZE = 16 * 1024 * 1024; // 16 MiB

    public static final int FLAG_CRC_PRESENT = 0x0001;
    public static final int FLAG_COMPRESSED = 0x0002;
    public static final int FLAG_STREAM = 0x0004;
    public static final int FLAG_END_STREAM = 0x0008;
    static final int FLAG_VALID_MASK = 0x000F;

    private FrameCodec() {}

    /**
     * Encode a payload into a complete RCPX frame (header + payload).
     */
    public static byte[] encode(byte[] payload) {
        int plen = payload.length;
        if (plen > MAX_PAYLOAD_SIZE) {
            throw new RstmdbException(ErrorCodes.BAD_REQUEST, "Payload exceeds maximum size", false, null);
        }

        byte[] frame = new byte[HEADER_SIZE + plen];
        ByteBuffer buf = ByteBuffer.wrap(frame);

        // Magic
        buf.put(MAGIC);
        // Version
        buf.putShort((short) PROTOCOL_VERSION);
        // Flags (CRC_PRESENT)
        buf.putShort((short) FLAG_CRC_PRESENT);
        // Header extension length
        buf.putShort((short) 0);
        // Payload length
        buf.putInt(plen);
        // CRC32C
        buf.putInt(computeCrc32c(payload));

        // Payload
        System.arraycopy(payload, 0, frame, HEADER_SIZE, plen);
        return frame;
    }

    /**
     * Decode a single frame from the input stream. Validates magic, version, flags, and CRC.
     *
     * @throws IOException on I/O errors
     * @throws RstmdbException on protocol violations
     */
    public static byte[] decode(InputStream stream) throws IOException {
        byte[] header = new byte[HEADER_SIZE];
        readExact(stream, header, 0, HEADER_SIZE);

        ByteBuffer buf = ByteBuffer.wrap(header);

        // Validate magic
        byte[] magic = new byte[4];
        buf.get(magic);
        if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1] || magic[2] != MAGIC[2] || magic[3] != MAGIC[3]) {
            throw new RstmdbException(ErrorCodes.UNSUPPORTED_PROTOCOL, "Invalid magic bytes", false, null);
        }

        // Validate version
        int version = Short.toUnsignedInt(buf.getShort());
        if (version != PROTOCOL_VERSION) {
            throw new RstmdbException(ErrorCodes.UNSUPPORTED_PROTOCOL,
                    "Unsupported protocol version: " + version, false, null);
        }

        // Validate flags
        int flags = Short.toUnsignedInt(buf.getShort());
        if ((flags & ~FLAG_VALID_MASK) != 0) {
            throw new RstmdbException(ErrorCodes.UNSUPPORTED_PROTOCOL, "Invalid flags: " + flags, false, null);
        }

        // Header extension length
        int headerExtLen = Short.toUnsignedInt(buf.getShort());

        // Payload length
        long payloadLen = Integer.toUnsignedLong(buf.getInt());
        if (payloadLen > MAX_PAYLOAD_SIZE) {
            throw new RstmdbException(ErrorCodes.BAD_REQUEST,
                    "Payload too large: " + payloadLen, false, null);
        }

        // CRC32C from header
        int frameCrc = buf.getInt();

        // Skip header extension if present
        if (headerExtLen > 0) {
            byte[] ext = new byte[headerExtLen];
            readExact(stream, ext, 0, headerExtLen);
        }

        // Read payload
        byte[] payload = new byte[(int) payloadLen];
        if (payloadLen > 0) {
            readExact(stream, payload, 0, (int) payloadLen);
        }

        // Validate CRC if present
        if ((flags & FLAG_CRC_PRESENT) != 0) {
            int computedCrc = computeCrc32c(payload);
            if (computedCrc != frameCrc) {
                throw new RstmdbException(ErrorCodes.BAD_REQUEST,
                        "CRC32C mismatch: expected " + Integer.toHexString(frameCrc)
                                + ", got " + Integer.toHexString(computedCrc), false, null);
            }
        }

        return payload;
    }

    static int computeCrc32c(byte[] data) {
        CRC32C crc = new CRC32C();
        crc.update(data);
        return (int) crc.getValue();
    }

    private static void readExact(InputStream stream, byte[] buffer, int offset, int length) throws IOException {
        int remaining = length;
        while (remaining > 0) {
            int read = stream.read(buffer, offset + (length - remaining), remaining);
            if (read < 0) {
                throw new IOException("Unexpected end of stream (needed " + remaining + " more bytes)");
            }
            remaining -= read;
        }
    }
}
