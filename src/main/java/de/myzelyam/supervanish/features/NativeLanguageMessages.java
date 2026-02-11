package de.myzelyam.supervanish.features;

import de.myzelyam.api.vanish.PlayerHideEvent;
import de.myzelyam.api.vanish.PlayerShowEvent;
import de.myzelyam.supervanish.SuperVanish;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class NativeLanguageMessages extends Feature {

    public NativeLanguageMessages(SuperVanish plugin) {
        super(plugin);
    }

    @Override
    public boolean isActive() {
        return plugin.getSettings().getBoolean("MessageOptions.UseNativeLanguageMessages", true)
                && plugin.getSettings().getBoolean("MessageOptions.HideRealJoinQuitMessages", true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVanish(PlayerHideEvent event) {
        if (!plugin.getSettings().getBoolean("MessageOptions.HideRealJoinQuitMessages", true)) return;

        Player player = event.getPlayer();
        TranslatableComponent message = Component.translatable(
                "multiplayer.player.left",
                NamedTextColor.YELLOW,
                Component.text(player.getName())
        );

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;
            if (plugin.hasPermissionToSee(online, player)) continue;
            
            online.sendMessage(message);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReappear(PlayerShowEvent event) {
        if (!plugin.getSettings().getBoolean("MessageOptions.HideRealJoinQuitMessages", true)) return;

        Player player = event.getPlayer();
        TranslatableComponent message = Component.translatable(
                "multiplayer.player.joined",
                NamedTextColor.YELLOW,
                Component.text(player.getName())
        );

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;
            if (plugin.hasPermissionToSee(online, player)) continue;
            
            online.sendMessage(message);
        }
    }

    @Override
    public void onEnable() {
    }
}
