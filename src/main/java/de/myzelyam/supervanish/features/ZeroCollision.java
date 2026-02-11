package de.myzelyam.supervanish.features;

import de.myzelyam.supervanish.SuperVanish;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.util.Vector;

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
        
        if (event.getVehicle().getPassengers().stream().anyMatch(p -> {
            if (p instanceof Player) {
                return plugin.getVanishStateMgr().isVanished(p.getUniqueId());
            }
            return false;
        })) {
            event.setCancelled(true);
            event.setCollisionCancelled(true);
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
        
        if (event.getRightClicked() instanceof Player) {
            Player target = (Player) event.getRightClicked();
            if (plugin.getVanishStateMgr().isVanished(target.getUniqueId())) {
                if (!plugin.hasPermissionToSee(event.getPlayer(), target)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        if (plugin.getVanishStateMgr().isVanished(event.getPlayer().getUniqueId())) {
            Vector velocity = event.getVelocity();
            if (velocity.lengthSquared() < 0.1) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGlideToggle(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (plugin.getVanishStateMgr().isVanished(player.getUniqueId())) {
                try {
                    ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
                    nmsPlayer.setNoGravity(false);
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public void onEnable() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getVanishStateMgr().isVanished(player.getUniqueId())) {
                try {
                    ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
                    nmsPlayer.setNoGravity(false);
                } catch (Exception e) {
                }
            }
        }
    }
}
