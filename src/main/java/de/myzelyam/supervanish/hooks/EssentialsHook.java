package de.myzelyam.supervanish.hooks;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import de.myzelyam.api.vanish.PlayerHideEvent;
import de.myzelyam.api.vanish.PostPlayerShowEvent;
import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.commands.CommandAction;
import de.myzelyam.supervanish.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class EssentialsHook extends PluginHook {

    private final Set<UUID> preVanishHiddenPlayers = new HashSet<>();
    private Essentials essentials;
    private ScheduledTask forcedInvisibilityTask;

    public EssentialsHook(SuperVanish superVanish) {
        super(superVanish);
    }

    private void runForcedInvisibilityTask() {
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("Essentials")) return;
            for (UUID uuid : superVanish.getVanishStateMgr().getOnlineVanishedPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                User user = essentials.getUser(p);
                if (user == null) continue;
                if (!user.isHidden()) {
                    user.setHidden(true);
                }
            }
        } catch (Exception e) {
            if (forcedInvisibilityTask != null) {
                forcedInvisibilityTask.cancel();
            }
            superVanish.logException(e);
        }
    }

    @Override
    public void onPluginEnable(Plugin plugin) {
        essentials = (Essentials) plugin;
        forcedInvisibilityTask = SuperVanish.getScheduler()
                .runTaskTimer(() -> runForcedInvisibilityTask(), 1L, 100L);
    }

    @Override
    public void onPluginDisable(Plugin plugin) {
        essentials = null;
        if (forcedInvisibilityTask != null) {
            forcedInvisibilityTask.cancel();
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent e) {
        User user = essentials.getUser(e.getPlayer());
        if (user == null) return;
        if (superVanish.getVanishStateMgr().isVanished(e.getPlayer().getUniqueId()) && !user.isHidden()) {
            user.setHidden(true);
        } else {
            user.setHidden(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVanish(PlayerHideEvent e) {
        User user = essentials.getUser(e.getPlayer());
        if (user == null) return;
        if (user.isVanished()) {
            user.setVanished(false);
        }
        preVanishHiddenPlayers.remove(e.getPlayer().getUniqueId());
        user.setHidden(true);
    }

    @EventHandler
    public void onReappear(PostPlayerShowEvent e) {
        User user = essentials.getUser(e.getPlayer());
        if (user == null) return;
        user.setHidden(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(final PlayerCommandPreprocessEvent e) {
        if (!CommandAction.VANISH_SELF.checkPermission(e.getPlayer(), superVanish)) return;
        if (superVanish.getVanishStateMgr().isVanished(e.getPlayer().getUniqueId())) return;
        String command = e.getMessage().toLowerCase(Locale.ENGLISH).split(" ")[0].replace("/", "")
                .toLowerCase(Locale.ENGLISH);
        if (command.split(":").length > 1) command = command.split(":")[1];
        if (command.equals("supervanish") || command.equals("sv")
                || command.equals("v") || command.equals("vanish")) {
            final User user = essentials.getUser(e.getPlayer());
            if (user == null || !user.isAfk()) return;
            user.setHidden(true);
            preVanishHiddenPlayers.add(e.getPlayer().getUniqueId());
            SuperVanish.getScheduler().runTaskLater(() -> {
              if (preVanishHiddenPlayers.remove(e.getPlayer().getUniqueId())) {
                user.setHidden(false);
              }
            }, 1L);
        }
    }
}