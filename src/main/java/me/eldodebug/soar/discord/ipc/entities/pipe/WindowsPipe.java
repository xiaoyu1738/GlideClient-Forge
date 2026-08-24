package me.eldodebug.soar.discord.ipc.entities.pipe;

import me.eldodebug.soar.discord.ipc.IPCClient;
import me.eldodebug.soar.discord.ipc.entities.Callback;
import me.eldodebug.soar.discord.ipc.entities.Packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

public class WindowsPipe extends Pipe {

    private static final Logger LOGGER = LogManager.getLogger(WindowsPipe.class);

    private final RandomAccessFile file;

    WindowsPipe(IPCClient ipcClient, HashMap<String, Callback> callbacks, String location) {
        super(ipcClient, callbacks);
        try {
            this.file = new RandomAccessFile(location, "rw");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        file.write(b);
    }

    @Override
    public Packet read() throws IOException {
    	
        while(file.length() == 0 && status == PipeStatus.CONNECTED) {
            try {
                Thread.sleep(50);
            } catch(InterruptedException ignored) {}
        }

        if(status==PipeStatus.DISCONNECTED) {
            throw new IOException("Disconnected!");
        }

        if(status==PipeStatus.CLOSED) {
            return new Packet(Packet.OpCode.CLOSE, null);
        }

        int opcode = Integer.reverseBytes(file.readInt());
        int len = PacketDecoder.validatePayloadSize(Integer.reverseBytes(file.readInt()));
        byte[] d = new byte[len];

        file.readFully(d);

        Packet p = PacketDecoder.decode(opcode, d);

        LOGGER.debug(String.format("Received packet: %s", p.toString()));
        
        if(listener != null) {
            listener.onPacketReceived(ipcClient, p);
        }
        
        return p;
    }

    @Override
    protected void closeTransport() throws IOException {
        LOGGER.debug("Closing IPC pipe...");
        file.close();
    }
}
