package me.eldodebug.soar.management.remote.blacklists;

import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final String serverIp;
    private final CopyOnWriteArrayList<String> mods;

    public Server(String serverIp, CopyOnWriteArrayList<String> mods) {
        this.serverIp = serverIp;
        this.mods = mods;
    }

    public String getServerIp() {
        return serverIp;
    }

    public CopyOnWriteArrayList<String> getMods() {
        return mods;
    }
}