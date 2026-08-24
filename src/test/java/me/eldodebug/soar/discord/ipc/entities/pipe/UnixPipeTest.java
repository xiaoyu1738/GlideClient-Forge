package me.eldodebug.soar.discord.ipc.entities.pipe;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonObject;
import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import me.eldodebug.soar.discord.ipc.entities.Callback;
import me.eldodebug.soar.discord.ipc.entities.Packet;
import me.eldodebug.soar.platform.PlatformUtils;
import org.junit.Test;

public class UnixPipeTest {

    private static final CLibrary LIBC = (CLibrary) Native.loadLibrary("c", CLibrary.class);
    private static final int AF_UNIX = 1;
    private static final int SOCK_STREAM = 1;

    @Test
    public void exchangesFramesOverARealUnixSocket() throws Exception {
        assumeTrue(PlatformUtils.isLinux());

        String location = "/tmp/glide-ipc-test-" + UUID.randomUUID().toString().substring(0, 8);
        File socketFile = new File(location);
        int serverSocket = -1;
        UnixPipe pipe = null;
        Thread serverThread = null;
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("direction", "client-to-server");
        byte[] request = new Packet(Packet.OpCode.FRAME, requestJson).toBytes();

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("direction", "server-to-client");
        responseJson.add("unused", null);
        byte[] response = new Packet(Packet.OpCode.FRAME, responseJson).toBytes();

        try {
            serverSocket = LIBC.socket(AF_UNIX, SOCK_STREAM, 0);
            assertTrue("Unable to create test AF_UNIX socket", serverSocket >= 0);

            UnixPipe.LinuxSocketAddress address = new UnixPipe.LinuxSocketAddress(location);
            address.write();
            assertEquals("Unable to bind test AF_UNIX socket", 0,
                    LIBC.bind(serverSocket, address.getPointer(), address.size()));
            assertEquals("Unable to listen on test AF_UNIX socket", 0,
                    LIBC.listen(serverSocket, 1));

            final int listeningSocket = serverSocket;
            serverThread = new Thread(() -> {
                int acceptedSocket = -1;
                try {
                    acceptedSocket = LIBC.accept(listeningSocket, null, null);
                    assertTrue("Unable to accept test AF_UNIX connection", acceptedSocket >= 0);
                    assertArrayEquals(request, readFully(acceptedSocket, request.length));

                    // Fragment every byte to prove the client handles short reads.
                    for (byte value : response) {
                        writeFully(acceptedSocket, new byte[] { value });
                    }
                } catch (Throwable throwable) {
                    serverFailure.set(throwable);
                } finally {
                    if (acceptedSocket >= 0) {
                        LIBC.close(acceptedSocket);
                    }
                }
            }, "Glide Unix IPC integration test server");
            serverThread.start();

            pipe = new UnixPipe(null, new HashMap<String, Callback>(), location);
            pipe.write(request);
            Packet packet = pipe.read();

            assertEquals(Packet.OpCode.FRAME, packet.getOp());
            assertEquals("server-to-client",
                    packet.getJson().get("direction").getAsString());
            assertTrue(!packet.getJson().has("unused"));
        } finally {
            if (pipe != null) {
                pipe.close();
            }
            if (serverThread != null) {
                serverThread.join(5000L);
                assertTrue("Test AF_UNIX server did not stop", !serverThread.isAlive());
            }
            if (serverSocket >= 0) {
                LIBC.close(serverSocket);
            }
            if (socketFile.exists()) {
                assertTrue("Unable to remove test AF_UNIX socket", socketFile.delete());
            }
        }

        assertNull(serverFailure.get() == null ? null : serverFailure.get().toString(),
                serverFailure.get());
    }

    private static byte[] readFully(int socket, int length) throws IOException {
        byte[] output = new byte[length];
        int offset = 0;
        while (offset < length) {
            byte[] chunk = new byte[Math.min(7, length - offset)];
            long read = LIBC.read(socket, chunk, new NativeLong(chunk.length)).longValue();
            if (read <= 0) {
                throw new IOException("Test AF_UNIX socket closed while reading");
            }
            System.arraycopy(chunk, 0, output, offset, (int) read);
            offset += (int) read;
        }
        return output;
    }

    private static void writeFully(int socket, byte[] bytes) throws IOException {
        int offset = 0;
        while (offset < bytes.length) {
            byte[] chunk = new byte[bytes.length - offset];
            System.arraycopy(bytes, offset, chunk, 0, chunk.length);
            long written = LIBC.write(socket, chunk, new NativeLong(chunk.length)).longValue();
            if (written <= 0) {
                throw new IOException("Test AF_UNIX socket closed while writing");
            }
            offset += (int) written;
        }
    }

    private interface CLibrary extends Library {
        int socket(int domain, int type, int protocol) throws LastErrorException;
        int bind(int socket, Pointer address, int addressLength) throws LastErrorException;
        int listen(int socket, int backlog) throws LastErrorException;
        int accept(int socket, Pointer address, Pointer addressLength) throws LastErrorException;
        NativeLong read(int socket, byte[] buffer, NativeLong length) throws LastErrorException;
        NativeLong write(int socket, byte[] buffer, NativeLong length) throws LastErrorException;
        int close(int socket) throws LastErrorException;
    }
}
