package me.eldodebug.soar.discord.ipc.entities.pipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import me.eldodebug.soar.discord.ipc.entities.Packet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PacketDecoderTest {

    @Test
    public void validFrameIsDecodedAndNullPropertiesAreRemoved() throws Exception {
        Packet packet = PacketDecoder.decode(Packet.OpCode.FRAME.ordinal(),
                "{\"cmd\":\"READY\",\"unused\":null}".getBytes(StandardCharsets.UTF_8));

        assertEquals(Packet.OpCode.FRAME, packet.getOp());
        assertEquals("READY", packet.getJson().get("cmd").getAsString());
        assertFalse(packet.getJson().has("unused"));
    }

    @Test(expected = IOException.class)
    public void unknownOpcodeIsRejected() throws Exception {
        PacketDecoder.decode(99, "{}".getBytes(StandardCharsets.UTF_8));
    }

    @Test(expected = IOException.class)
    public void invalidJsonIsRejected() throws Exception {
        PacketDecoder.decode(Packet.OpCode.FRAME.ordinal(),
                "not-json".getBytes(StandardCharsets.UTF_8));
    }

    @Test(expected = IOException.class)
    public void nonObjectJsonIsRejected() throws Exception {
        PacketDecoder.decode(Packet.OpCode.FRAME.ordinal(),
                "[]".getBytes(StandardCharsets.UTF_8));
    }

    @Test(expected = IOException.class)
    public void oversizedPayloadIsRejectedBeforeAllocation() throws Exception {
        PacketDecoder.validatePayloadSize(PacketDecoder.MAX_PAYLOAD_SIZE + 1);
    }
}
