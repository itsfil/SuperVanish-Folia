package de.myzelyam.supervanish.features;

import de.myzelyam.api.vanish.PlayerHideEvent;
import de.myzelyam.api.vanish.PlayerShowEvent;
import de.myzelyam.supervanish.SuperVanish;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class MobIgnorance extends Feature {

    public MobIgnorance(SuperVanish plugin) {
        super(plugin);
    }

    @Override
    public boolean isActive() {
        return plugin.getSettings().getBoolean("InvisibilityFeatures.MobIgnorance", true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (!(event.getTarget() instanceof Player)) return;

        Player target = (Player) event.getTarget();
        if (plugin.getVanishStateMgr().isVanished(target.getUniqueId())) {
            event.setCancelled(true);
            event.setTarget(null);
            
            try {
                LivingEntity mob = (LivingEntity) event.getEntity();
                if (mob instanceof org.bukkit.entity.Mob) {
                    Mob nmsEntity = ((CraftMob) mob).getHandle();
                    nmsEntity.setTarget(null);
                }
            } catch (Exception e) {
                plugin.logException(e);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVanish(PlayerHideEvent event) {
        clearMobTargets(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getVanishStateMgr().isVanished(event.getPlayer().getUniqueId())) {
            SuperVanish.getScheduler().runTaskLater(() -> clearMobTargets(event.getPlayer()), 5L);
        }
    }

    private void clearMobTargets(Player player) {
        ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        
        player.getWorld().getLivingEntities().stream()
                .filter(entity -> entity instanceof org.bukkit.entity.Mob)
                .forEach(entity -> {
                    try {
                        Mob nmsEntity = ((CraftMob) entity).getHandle();
                        if (nmsEntity.getTarget() == nmsPlayer) {
                            nmsEntity.setTarget(null);
                        }
                    } catch (Exception e) {
                        plugin.logException(e);
                    }
                });
    }

    @Override
    public void onEnable() {
        for (UUID vanishedUUID : plugin.getVanishStateMgr().getOnlineVanishedPlayers()) {
            Player vanished = Bukkit.getPlayer(vanishedUUID);
            if (vanished != null && vanished.isOnline()) {
                clearMobTargets(vanished);
            }
        }
    }
}
