package de.myzelyam.supervanish.features;

import de.myzelyam.supervanish.SuperVanish;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class ProjectilePassThrough extends Feature {

    public ProjectilePassThrough(SuperVanish plugin) {
        super(plugin);
    }

    @Override
    public boolean isActive() {
        return plugin.getSettings().getBoolean("InvisibilityFeatures.ProjectilePassThrough", true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player shooter = (Player) event.getEntity().getShooter();
            if (plugin.getVanishStateMgr().isVanished(shooter.getUniqueId())) {
                event.getEntity().setGravity(true);
                event.getEntity().setInvulnerable(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof Player) {
            Player hitPlayer = (Player) event.getHitEntity();
            if (plugin.getVanishStateMgr().isVanished(hitPlayer.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }
        
        if (event.getEntity().getShooter() instanceof Player) {
            Player shooter = (Player) event.getEntity().getShooter();
            if (plugin.getVanishStateMgr().isVanished(shooter.getUniqueId())) {
                if (event.getHitEntity() != null) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            if (event.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) event.getDamager();
                if (projectile.getShooter() instanceof Player) {
                    Player shooter = (Player) projectile.getShooter();
                    if (plugin.getVanishStateMgr().isVanished(shooter.getUniqueId())) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
        
        if (event.getEntity() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            if (plugin.getVanishStateMgr().isVanished(damaged.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }
        
        if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                Player shooter = (Player) projectile.getShooter();
                if (plugin.getVanishStateMgr().isVanished(shooter.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onEnable() {
    }
}
