package de.myzelyam.supervanish.features;

import de.myzelyam.supervanish.SuperVanish;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;

public class ZeroCollision extends Feature {

    public ZeroCollision(SuperVanish plugin) {
        super(plugin);
    }

    @Override
    public boolean isActive() {
        return plugin.getSettings().getBoolean("InvisibilityFeatures.ZeroCollision", true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVehicleCollision(VehicleEntityCollisionEvent event) {
        Entity collider = event.getEntity();
        if (collider instanceof Player) {
            Player player = (Player) collider;
            if (plugin.getVanishStateMgr().isVanished(player.getUniqueId())) {
                event.setCancelled(true);
                event.setCollisionCancelled(true);
                event.setPickupCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (plugin.getVanishStateMgr().isVanished(damager.getUniqueId())) {
                if (!(event.getEntity() instanceof Player)) {
                    event.setCancelled(true);
                }
            }
        }

        if (event.getEntity() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            if (plugin.getVanishStateMgr().isVanished(damaged.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (plugin.getVanishStateMgr().isVanished(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEnable() {
    }
}
