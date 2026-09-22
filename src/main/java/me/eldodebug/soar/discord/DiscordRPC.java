package me.eldodebug.soar.discord;

import java.time.OffsetDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import me.eldodebug.soar.discord.ipc.entities.pipe.PipeStatus;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.discord.ipc.IPCClient;
import me.eldodebug.soar.discord.ipc.IPCListener;
import me.eldodebug.soar.discord.ipc.entities.RichPresence;
import me.eldodebug.soar.discord.ipc.exceptions.NoDiscordClientException;

public class DiscordRPC {

    private static final Logger LOGGER = LogManager.getLogger(DiscordRPC.class);

    private IPCClient client;

    public synchronized void start() {
        if (isStarted()) return;
        stop();

        client = new IPCClient(1059341815205068901L);
        client.setListener(new IPCListener() {
            @Override
            public void onReady(IPCClient client) {

                RichPresence.Builder builder = new RichPresence.Builder();

                builder.setState("Playing Glide Client v" + Glide.getInstance().getVersion())
                        .setStartTimestamp(OffsetDateTime.now())
                        .setLargeImage("icon");

                client.sendRichPresence(builder.build());
            }
        });

        try {
            client.connect();
        } catch (NoDiscordClientException e) {
            stop();
            LOGGER.info("Discord RPC unavailable: no running Discord desktop client was found.");
        }
    }

    public synchronized void stop() {
        IPCClient current = client;
        client = null;
        if (current != null) current.close();
    }

    public synchronized IPCClient getClient() {
        return client;
    }

    public synchronized boolean isStarted() {
        return client != null && client.getStatus() == PipeStatus.CONNECTED;
    }
}
