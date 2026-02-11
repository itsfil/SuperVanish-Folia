package de.myzelyam.supervanish.visibility.nms;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import de.myzelyam.supervanish.SuperVanish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class ServerListPacketHandler implements Listener {
    private final SuperVanish plugin;

    public ServerListPacketHandler(SuperVanish plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerListPing(PaperServerListPingEvent event) {
        if (!plugin.getSettings().getBoolean("ExternalInvisibility.ServerList.AdjustAmountOfOnlinePlayers", false)
                && !plugin.getSettings().getBoolean("ExternalInvisibility.ServerList.AdjustListOfLoggedInPlayers", false)) {
            return;
        }

        int vanishedCount = plugin.getVanishStateMgr().getOnlineVanishedPlayers().size();

        if (plugin.getSettings().getBoolean("ExternalInvisibility.ServerList.AdjustAmountOfOnlinePlayers", false)) {
            event.setNumPlayers(event.getNumPlayers() - vanishedCount);
        }

        if (plugin.getSettings().getBoolean("ExternalInvisibility.ServerList.AdjustListOfLoggedInPlayers", false)) {
            var iterator = event.getPlayerSample().iterator();
            while (iterator.hasNext()) {
                var profile = iterator.next();
                UUID uuid = profile.getId();
                if (plugin.getVanishStateMgr().isVanished(uuid)) {
                    iterator.remove();
                }
            }
        }
    }

    public static boolean isEnabled(SuperVanish plugin) {
        return plugin.getSettings().getBoolean("ExternalInvisibility.ServerList.AdjustAmountOfOnlinePlayers", false)
                || plugin.getSettings().getBoolean("ExternalInvisibility.ServerList.AdjustListOfLoggedInPlayers", false);
    }
}
