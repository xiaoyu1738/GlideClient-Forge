package me.eldodebug.soar.discord.ipc.entities.pipe;

import com.google.gson.JsonObject;
import me.eldodebug.soar.discord.DiscordRPC;
import me.eldodebug.soar.discord.ipc.IPCClient;
import me.eldodebug.soar.discord.ipc.entities.Callback;
import me.eldodebug.soar.discord.ipc.entities.Packet;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.Assert.*;

public class IPCClientLifecycleTest {
    @Test
    public void unavailableClientCanBeUsedAndClosedRepeatedly() {
        // This is also the state left when Pipe.openPipe throws NoDiscordClientException.
        IPCClient client = new IPCClient(1L);
        client.sendRichPresence(null);
        client.subscribe(IPCClient.Event.ACTIVITY_JOIN);
        client.close();
        client.close();
        assertEquals(PipeStatus.UNINITIALIZED, client.getStatus());
    }

    @Test
    public void connectedClientSendsAndClosesOnlyOnce() throws Exception {
        IPCClient client = new IPCClient(1L);
        TestPipe pipe = attach(client, PipeStatus.CONNECTED);
        client.sendRichPresence(null);
        client.subscribe(IPCClient.Event.ACTIVITY_JOIN);
        assertEquals(2, pipe.writes);
        client.close();
        client.close();
        client.sendRichPresence(null);
        assertEquals(3, pipe.writes); // activity, subscription, CLOSE
        assertEquals(1, pipe.closes);
        assertEquals(PipeStatus.CLOSED, client.getStatus());
    }

    @Test
    public void disconnectedTransportIsStillReleased() throws Exception {
        IPCClient client = new IPCClient(1L);
        TestPipe pipe = attach(client, PipeStatus.DISCONNECTED);
        client.sendRichPresence(null);
        client.subscribe(IPCClient.Event.ACTIVITY_JOIN);
        client.close();
        assertEquals(0, pipe.writes);
        assertEquals(1, pipe.closes);
    }

    @Test
    public void rpcStopClearsStateAndSupportsRepeatedToggles() throws Exception {
        DiscordRPC rpc = new DiscordRPC();
        rpc.stop();
        Field field = DiscordRPC.class.getDeclaredField("client");
        field.setAccessible(true);
        for (PipeStatus status : new PipeStatus[] {PipeStatus.CONNECTED, PipeStatus.DISCONNECTED}) {
            IPCClient client = new IPCClient(1L);
            TestPipe pipe = attach(client, status);
            field.set(rpc, client);
            assertEquals(status == PipeStatus.CONNECTED, rpc.isStarted());
            rpc.stop();
            rpc.stop();
            assertFalse(rpc.isStarted());
            assertNull(rpc.getClient());
            assertEquals(1, pipe.closes);
        }
        field.set(rpc, new IPCClient(1L));
        assertFalse(rpc.isStarted());
        rpc.stop();
        assertNull(rpc.getClient());
    }

    private static TestPipe attach(IPCClient client, PipeStatus status) throws Exception {
        TestPipe pipe = new TestPipe(client);
        pipe.setStatus(status);
        Field field = IPCClient.class.getDeclaredField("pipe");
        field.setAccessible(true);
        field.set(client, pipe);
        return pipe;
    }

    private static final class TestPipe extends Pipe {
        int writes;
        int closes;
        TestPipe(IPCClient client) { super(client, new HashMap<String, Callback>()); }
        @Override public Packet read() throws IOException { return new Packet(Packet.OpCode.CLOSE, new JsonObject()); }
        @Override public void write(byte[] bytes) { writes++; }
        @Override protected void closeTransport() { closes++; }
    }
}
