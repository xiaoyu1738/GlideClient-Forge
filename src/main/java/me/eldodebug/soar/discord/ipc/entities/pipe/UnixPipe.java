package me.eldodebug.soar.discord.ipc.entities.pipe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import me.eldodebug.soar.discord.ipc.IPCClient;
import me.eldodebug.soar.discord.ipc.entities.Callback;
import me.eldodebug.soar.discord.ipc.entities.Packet;
import me.eldodebug.soar.platform.PlatformUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Java 8 Unix-domain socket transport backed by Minecraft's bundled JNA. */
final class UnixPipe extends Pipe {

    private static final Logger LOGGER = LogManager.getLogger(UnixPipe.class);
    private static final int AF_UNIX = 1;
    private static final int SOCK_STREAM = 1;
    private static final int EINTR = 4;
    private static final CLibrary LIBC = (CLibrary) Native.loadLibrary("c", CLibrary.class);

    private volatile int socket;

    UnixPipe(IPCClient ipcClient, HashMap<String, Callback> callbacks, String location) {
        super(ipcClient, callbacks);
        socket = LIBC.socket(AF_UNIX, SOCK_STREAM, 0);
        if (socket < 0) {
            throw new IllegalStateException("Could not create Discord IPC socket: errno "
                    + Native.getLastError());
        }

        UnixSocketAddress address = PlatformUtils.isMacOS()
                ? new MacSocketAddress(location)
                : new LinuxSocketAddress(location);
        address.write();
        try {
            if (LIBC.connect(socket, address.getPointer(), address.size()) != 0) {
                throw new IllegalStateException("Could not connect to " + location
                        + ": errno " + Native.getLastError());
            }
        } catch (RuntimeException exception) {
            closeDescriptor();
            throw exception;
        }
    }

    @Override
    public synchronized void write(byte[] bytes) throws IOException {
        int offset = 0;
        while (offset < bytes.length) {
            byte[] remaining = offset == 0 ? bytes : Arrays.copyOfRange(bytes, offset, bytes.length);
            long written = nativeWrite(remaining);
            if (written <= 0) {
                throw new IOException("Discord IPC socket closed while writing");
            }
            offset += (int) written;
        }
    }

    @Override
    public Packet read() throws IOException {
        byte[] header = readFully(8);
        ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = buffer.getInt();
        int length = PacketDecoder.validatePayloadSize(buffer.getInt());
        Packet packet = PacketDecoder.decode(opcode, readFully(length));
        LOGGER.debug(String.format("Received packet: %s", packet.toString()));
        if (listener != null) {
            listener.onPacketReceived(ipcClient, packet);
        }
        return packet;
    }

    @Override
    protected void closeTransport() {
        LOGGER.debug("Closing IPC socket...");
        closeDescriptor();
    }

    private byte[] readFully(int length) throws IOException {
        byte[] output = new byte[length];
        int offset = 0;
        while (offset < length) {
            byte[] remaining = new byte[length - offset];
            long read = nativeRead(remaining);
            if (read < 0) {
                throw new IOException("Discord IPC socket returned a negative read length");
            }
            if (read == 0) {
                throw new IOException("Discord IPC socket closed while reading");
            }
            if (read > remaining.length) {
                throw new IOException("Discord IPC socket returned an invalid read length: " + read);
            }
            System.arraycopy(remaining, 0, output, offset, (int) read);
            offset += (int) read;
        }
        return output;
    }

    private long nativeRead(byte[] buffer) throws IOException {
        while (true) {
            try {
                return LIBC.read(socket, buffer, new NativeLong(buffer.length)).longValue();
            } catch (LastErrorException exception) {
                if (exception.getErrorCode() != EINTR) {
                    throw new IOException("Discord IPC read failed: errno "
                            + exception.getErrorCode(), exception);
                }
            }
        }
    }

    private long nativeWrite(byte[] buffer) throws IOException {
        while (true) {
            try {
                return LIBC.write(socket, buffer, new NativeLong(buffer.length)).longValue();
            } catch (LastErrorException exception) {
                if (exception.getErrorCode() != EINTR) {
                    throw new IOException("Discord IPC write failed: errno "
                            + exception.getErrorCode(), exception);
                }
            }
        }
    }

    private synchronized void closeDescriptor() {
        int descriptor = socket;
        socket = -1;
        if (descriptor >= 0) {
            LIBC.close(descriptor);
        }
    }

    private interface CLibrary extends Library {
        int socket(int domain, int type, int protocol) throws LastErrorException;
        int connect(int socket, Pointer address, int addressLength) throws LastErrorException;
        NativeLong read(int socket, byte[] buffer, NativeLong length) throws LastErrorException;
        NativeLong write(int socket, byte[] buffer, NativeLong length) throws LastErrorException;
        int close(int socket) throws LastErrorException;
    }

    private abstract static class UnixSocketAddress extends Structure {
        abstract void setPath(String path);

        final void copyPath(String path, byte[] destination) {
            byte[] encoded = path.getBytes(StandardCharsets.UTF_8);
            if (encoded.length >= destination.length) {
                throw new IllegalArgumentException("Discord IPC socket path is too long: " + path);
            }
            System.arraycopy(encoded, 0, destination, 0, encoded.length);
        }
    }

    public static final class LinuxSocketAddress extends UnixSocketAddress {
        public short family = AF_UNIX;
        public byte[] path = new byte[108];

        LinuxSocketAddress(String path) {
            setPath(path);
        }

        @Override
        void setPath(String value) {
            copyPath(value, path);
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("family", "path");
        }
    }

    public static final class MacSocketAddress extends UnixSocketAddress {
        public byte length;
        public byte family = AF_UNIX;
        public byte[] path = new byte[104];

        MacSocketAddress(String path) {
            setPath(path);
            length = (byte) size();
        }

        @Override
        void setPath(String value) {
            copyPath(value, path);
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("length", "family", "path");
        }
    }
}
