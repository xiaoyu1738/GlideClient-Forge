package me.eldodebug.soar.discord.ipc.entities.pipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.eldodebug.soar.discord.ipc.entities.Packet;
import me.eldodebug.soar.discord.ipc.entities.serialize.PacketDeserializer;

final class PacketDecoder {

    static final int MAX_PAYLOAD_SIZE = 16 * 1024 * 1024;

    private PacketDecoder() {
    }

    static int validatePayloadSize(int length) throws IOException {
        if (length < 0 || length > MAX_PAYLOAD_SIZE) {
            throw new IOException("Invalid Discord IPC payload length: " + length);
        }
        return length;
    }

    static Packet decode(int opcode, byte[] payload) throws IOException {
        Packet.OpCode[] opcodes = Packet.OpCode.values();
        if (opcode < 0 || opcode >= opcodes.length) {
            throw new IOException("Invalid Discord IPC opcode: " + opcode);
        }

        Packet.OpCode operation = opcodes[opcode];
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Packet.class, new PacketDeserializer(operation))
                .create();
        try {
            JsonElement element = gson.fromJson(
                    new String(payload, StandardCharsets.UTF_8), JsonElement.class);
            if (element == null || !element.isJsonObject()) {
                throw new IOException("Discord IPC packet contained no JSON object");
            }
            JsonObject json = element.getAsJsonObject();
            return gson.fromJson(json, Packet.class);
        } catch (IOException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IOException("Discord IPC packet contained invalid JSON", exception);
        }
    }
}
