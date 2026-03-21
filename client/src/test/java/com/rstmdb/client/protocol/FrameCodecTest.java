package com.rstmdb.client.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rstmdb.client.exception.RstmdbException;
import com.rstmdb.client.model.BatchMode;
import com.rstmdb.client.model.Transition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32C;

import static org.assertj.core.api.BDDAssertions.*;

class FrameCodecTest {

    @Test
    @DisplayName("When payload is encoded and decoded Then original payload is returned")
    void encodeDecodeRoundtrip() throws IOException {
        // given
        byte[] payload = "hello world".getBytes(StandardCharsets.UTF_8);

        // when
        byte[] frame = FrameCodec.encode(payload);
        byte[] decoded = FrameCodec.decode(new ByteArrayInputStream(frame));

        // then
        then(frame.length).isEqualTo(FrameCodec.HEADER_SIZE + payload.length);
        then(decoded).isEqualTo(payload);
    }

    @Test
    @DisplayName("When CRC32C is computed for known vector Then expected checksum is returned")
    void crc32cKnownVector() {
        // given
        byte[] data = "123456789".getBytes(StandardCharsets.US_ASCII);

        // when
        CRC32C crc = new CRC32C();
        crc.update(data);

        // then
        then((int) crc.getValue()).isEqualTo(0xE3069283);
    }

    @Test
    @DisplayName("When frame has invalid magic bytes Then RstmdbException is thrown")
    void invalidMagic() {
        // given
        byte[] frame = FrameCodec.encode("test".getBytes());
        frame[0] = 'X';

        // when / then
        thenThrownBy(() -> FrameCodec.decode(new ByteArrayInputStream(frame)))
                .isInstanceOf(RstmdbException.class)
                .hasMessageContaining("Invalid magic");
    }

    @Test
    @DisplayName("When frame has unsupported version Then RstmdbException is thrown")
    void invalidVersion() {
        // given
        byte[] frame = FrameCodec.encode("test".getBytes());
        ByteBuffer.wrap(frame).putShort(4, (short) 99);

        // when / then
        thenThrownBy(() -> FrameCodec.decode(new ByteArrayInputStream(frame)))
                .isInstanceOf(RstmdbException.class)
                .hasMessageContaining("Unsupported protocol version");
    }

    @Test
    @DisplayName("When frame has invalid flags Then RstmdbException is thrown")
    void invalidFlags() {
        // given
        byte[] frame = FrameCodec.encode("test".getBytes());
        ByteBuffer.wrap(frame).putShort(6, (short) 0xFF00);

        // when / then
        thenThrownBy(() -> FrameCodec.decode(new ByteArrayInputStream(frame)))
                .isInstanceOf(RstmdbException.class)
                .hasMessageContaining("Invalid flags");
    }

    @Test
    @DisplayName("When frame CRC is corrupted Then RstmdbException is thrown")
    void crcMismatch() {
        // given
        byte[] frame = FrameCodec.encode("test".getBytes());
        ByteBuffer.wrap(frame).putInt(14, 0xDEADBEEF);

        // when / then
        thenThrownBy(() -> FrameCodec.decode(new ByteArrayInputStream(frame)))
                .isInstanceOf(RstmdbException.class)
                .hasMessageContaining("CRC32C mismatch");
    }

    @Test
    @DisplayName("When payload exceeds max size Then RstmdbException is thrown")
    void oversizedPayload() {
        // given
        byte[] large = new byte[FrameCodec.MAX_PAYLOAD_SIZE + 1];

        // when / then
        thenThrownBy(() -> FrameCodec.encode(large))
                .isInstanceOf(RstmdbException.class)
                .hasMessageContaining("maximum size");
    }

    @Test
    @DisplayName("When empty payload is encoded Then empty payload is decoded")
    void emptyPayload() throws IOException {
        // given
        byte[] empty = new byte[0];

        // when
        byte[] frame = FrameCodec.encode(empty);
        byte[] decoded = FrameCodec.decode(new ByteArrayInputStream(frame));

        // then
        then(decoded).isEmpty();
    }

    @Test
    @DisplayName("When stream is truncated Then IOException is thrown")
    void truncatedStream() {
        // given
        byte[] frame = FrameCodec.encode("test".getBytes());
        byte[] truncated = new byte[10];
        System.arraycopy(frame, 0, truncated, 0, 10);

        // when / then
        thenThrownBy(() -> FrameCodec.decode(new ByteArrayInputStream(truncated)))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("When transition from is a single string Then it deserializes as single-element list")
    void stringOrArrayDeserializerSingleString() throws Exception {
        // given
        ObjectMapper mapper = WireMessage.MAPPER;
        String json = "{\"from\":\"created\",\"event\":\"PAY\",\"to\":\"paid\",\"guard\":null}";

        // when
        Transition t = mapper.readValue(json, Transition.class);

        // then
        then(t.getFrom()).containsExactly("created");
    }

    @Test
    @DisplayName("When transition from is an array Then it deserializes as list")
    void stringOrArrayDeserializerArray() throws Exception {
        // given
        ObjectMapper mapper = WireMessage.MAPPER;
        String json = "{\"from\":[\"created\",\"pending\"],\"event\":\"PAY\",\"to\":\"paid\",\"guard\":null}";

        // when
        Transition t = mapper.readValue(json, Transition.class);

        // then
        then(t.getFrom()).containsExactly("created", "pending");
    }

    @Test
    @DisplayName("When BatchMode is serialized Then correct JSON string is produced")
    void batchModeSerialization() throws Exception {
        // given
        ObjectMapper mapper = WireMessage.MAPPER;

        // when
        String atomic = mapper.writeValueAsString(BatchMode.ATOMIC);
        String bestEffort = mapper.writeValueAsString(BatchMode.BEST_EFFORT);

        // then
        then(atomic).isEqualTo("\"atomic\"");
        then(bestEffort).isEqualTo("\"best_effort\"");
        then(mapper.readValue("\"atomic\"", BatchMode.class)).isEqualTo(BatchMode.ATOMIC);
        then(mapper.readValue("\"best_effort\"", BatchMode.class)).isEqualTo(BatchMode.BEST_EFFORT);
    }
}
