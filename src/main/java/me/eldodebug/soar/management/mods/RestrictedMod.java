package me.eldodebug.soar.management.mods;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.notification.NotificationType;
import me.eldodebug.soar.management.remote.blacklists.BlacklistManager;
import me.eldodebug.soar.management.remote.blacklists.Server;
import me.eldodebug.soar.utils.ServerUtils;

import java.util.List;

public class RestrictedMod {

    String currentServerIP = "";
    public Boolean shouldCheck = true;

    BlacklistManager blm = Glide.getInstance().getBlacklistManager();

    public boolean checkAllowed(Mod m) {
        if (shouldCheck) {
            List<Server> servers = blm.getBlacklist();
            for (Server server : servers) {
                if (currentServerIP.contains(server.getServerIp())) {
                    List<String> blacklistedMods = server.getMods();
                    if (blacklistedMods.contains(m.getNameKey())) {
                        m.setAllowed(false);
                        return false;
                    }
                }
            }
        }
        m.setAllowed(true);
        return true;
    }

    public void joinServer(String ip) {
        blm.check();
    }

    public void joinWorld(){
        this.currentServerIP = ServerUtils.getServerIP();
        for(Mod m : Glide.getInstance().getModManager().getMods()){
            if(!checkAllowed(m) && m.isToggled()){
                m.setToggled(false);
                Glide.getInstance().getNotificationManager().post(m.getName(),  "Disabled due to serverside blacklist" , NotificationType.INFO);
            }
        }
    }

}
