package com.rstmdb.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rstmdb.client.exception.RstmdbException;
import com.rstmdb.client.model.BatchMode;
import com.rstmdb.client.model.Transition;
import com.rstmdb.client.protocol.FrameCodec;
import com.rstmdb.client.protocol.WireMessage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32C;

import static org.assertj.core.api.Assertions.*;

class FrameCodecTest {

    @Test
    void encodeDecodeRoundtrip() throws IOException {
        byte[] payload = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] frame = FrameCodec.encode(payload);

        assertThat(frame.length).isEqualTo(FrameCodec.HEADER_SIZE + payload.length);

        byte[] decoded = FrameCodec.decode(new ByteArrayInputStream(frame));
        assertThat(decoded).isEqualTo(payload);
    }

    @Test
    void crc32cKnownVector() {
        // CRC32C of "123456789" = 0xE3069283
        byte[] data = "123456789".getBytes(StandardCharsets.US_ASCII);
        CRC32C crc = new CRC32C();
        crc.update(data);
        assertThat((int) crc.getValue()).isEqualTo(0xE3069283);
    }

    @Test
    void invalidMagic() {
        byte[] frame = FrameCodec.encode("test".getBytes());
        // Corrupt magic
        frame[0] = 'X';
        assertThatThrownBy(() -> FrameCodec.decode(new ByteArrayInputStream(frame)))
                .isInstanceOf(RstmdbException.class)
                .hasMessageContaining("Invalid magic");
    }

    @Test
    void invalidVersion() {
        byte[] frame = FrameCodec.encode("test".getBytes());
        // Set version to 99
        ByteBuffer.wrap(frame).putShort(4, (short) 99);
        assertThatThrownBy(() -> FrameCodec.decode(new ByteArrayInputStream(frame)))
                .isInstanceOf(RstmdbException.class)
                .hasMessageContaining("Unsupported protocol version");
    }

    @Test
    void invalidFlags() {
        byte[] frame = FrameCodec.encode("test".getBytes());
        // Set unknown flag bits
        ByteBuffer.wrap(frame).putShort(6, (short) 0xFF00);
        assertThatThrownBy(() -> FrameCodec.decode(new ByteArrayInputStream(frame)))
                .isInstanceOf(RstmdbException.class)
                .hasMessageContaining("Invalid flags");
    }

    @Test
    void crcMismatch() {
        byte[] frame = FrameCodec.encode("test".getBytes());
        // Corrupt CRC
        ByteBuffer.wrap(frame).putInt(14, 0xDEADBEEF);
        assertThatThrownBy(() -> FrameCodec.decode(new ByteArrayInputStream(frame)))
                .isInstanceOf(RstmdbException.class)
                .hasMessageContaining("CRC32C mismatch");
    }

    @Test
    void oversizedPayload() {
        byte[] large = new byte[FrameCodec.MAX_PAYLOAD_SIZE + 1];
        assertThatThrownBy(() -> FrameCodec.encode(large))
                .isInstanceOf(RstmdbException.class)
                .hasMessageContaining("maximum size");
    }

    @Test
    void emptyPayload() throws IOException {
        byte[] frame = FrameCodec.encode(new byte[0]);
        byte[] decoded = FrameCodec.decode(new ByteArrayInputStream(frame));
        assertThat(decoded).isEmpty();
    }

    @Test
    void truncatedStream() {
        byte[] frame = FrameCodec.encode("test".getBytes());
        // Only provide half the frame
        byte[] truncated = new byte[10];
        System.arraycopy(frame, 0, truncated, 0, 10);
        assertThatThrownBy(() -> FrameCodec.decode(new ByteArrayInputStream(truncated)))
                .isInstanceOf(IOException.class);
    }

    @Test
    void stringOrArrayDeserializerSingleString() throws Exception {
        ObjectMapper mapper = WireMessage.MAPPER;
        String json = "{\"from\":\"created\",\"event\":\"PAY\",\"to\":\"paid\",\"guard\":null}";
        Transition t = mapper.readValue(json, Transition.class);
        assertThat(t.getFrom()).containsExactly("created");
    }

    @Test
    void stringOrArrayDeserializerArray() throws Exception {
        ObjectMapper mapper = WireMessage.MAPPER;
        String json = "{\"from\":[\"created\",\"pending\"],\"event\":\"PAY\",\"to\":\"paid\",\"guard\":null}";
        Transition t = mapper.readValue(json, Transition.class);
        assertThat(t.getFrom()).containsExactly("created", "pending");
    }

    @Test
    void batchModeSerialization() throws Exception {
        ObjectMapper mapper = WireMessage.MAPPER;
        String atomic = mapper.writeValueAsString(BatchMode.ATOMIC);
        assertThat(atomic).isEqualTo("\"atomic\"");

        String bestEffort = mapper.writeValueAsString(BatchMode.BEST_EFFORT);
        assertThat(bestEffort).isEqualTo("\"best_effort\"");

        assertThat(mapper.readValue("\"atomic\"", BatchMode.class)).isEqualTo(BatchMode.ATOMIC);
        assertThat(mapper.readValue("\"best_effort\"", BatchMode.class)).isEqualTo(BatchMode.BEST_EFFORT);
    }
}
