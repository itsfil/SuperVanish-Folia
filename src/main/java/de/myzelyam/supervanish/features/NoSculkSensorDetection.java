package de.myzelyam.supervanish.features;

import de.myzelyam.supervanish.SuperVanish;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class NoSculkSensorDetection extends Feature {

    public NoSculkSensorDetection(SuperVanish plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSculkSensorTrigger(BlockReceiveGameEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (!plugin.getVanishStateMgr().isVanished(p.getUniqueId())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getVanishStateMgr().isVanished(p.getUniqueId())) return;
        if (!e.getAction().equals(Action.PHYSICAL) || e.getClickedBlock() == null) return;
        Material type = e.getClickedBlock().getType();
        if (!(type == Material.SCULK_SENSOR || type == Material.CALIBRATED_SCULK_SENSOR || type == Material.SCULK_SHRIEKER)) return;
        e.setCancelled(true);
    }

    @Override
    public boolean isActive() {
        return plugin.getSettings().getBoolean("InvisibilityFeatures.PreventSculkSensorActivation", true);
    }
}
