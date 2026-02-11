package de.myzelyam.supervanish.features;

import de.myzelyam.api.vanish.PlayerHideEvent;
import de.myzelyam.api.vanish.PlayerShowEvent;
import de.myzelyam.supervanish.SuperVanish;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftArmorStand;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PhysicalBarrier extends Feature {
    private final Map<UUID, ArmorStand> barriers = new ConcurrentHashMap<>();

    public PhysicalBarrier(SuperVanish plugin) {
        super(plugin);
    }

    @Override
    public boolean isActive() {
        return plugin.getSettings().getBoolean("InvisibilityFeatures.PhysicalBarrier", true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVanish(PlayerHideEvent event) {
        createBarrier(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReappear(PlayerShowEvent event) {
        removeBarrier(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getVanishStateMgr().isVanished(event.getPlayer().getUniqueId())) {
            SuperVanish.getScheduler().runTaskLater(() -> createBarrier(event.getPlayer()), 10L);
        }
    }

    private void createBarrier(Player player) {
        try {
            removeBarrier(player);

            Location loc = player.getLocation();
            Level level = ((CraftWorld) loc.getWorld()).getHandle();
            
            ArmorStand barrier = new ArmorStand(EntityType.ARMOR_STAND, level);
            barrier.setPos(loc.getX(), loc.getY(), loc.getZ());
            barrier.setInvisible(true);
            barrier.setInvulnerable(true);
            barrier.setNoGravity(true);
            barrier.setSilent(true);
            barrier.setMarker(true);
            barrier.setSmall(true);

            barriers.put(player.getUniqueId(), barrier);

            SuperVanish.getScheduler().runTaskTimer(() -> {
                if (!player.isOnline() || !plugin.getVanishStateMgr().isVanished(player.getUniqueId())) {
                    removeBarrier(player);
                    return;
                }
                
                Location newLoc = player.getLocation();
                ArmorStand stand = barriers.get(player.getUniqueId());
                if (stand != null && !stand.isRemoved()) {
                    stand.setPos(newLoc.getX(), newLoc.getY(), newLoc.getZ());
                }
            }, 1L, 1L);

        } catch (Exception e) {
            plugin.logException(e);
        }
    }

    private void removeBarrier(Player player) {
        ArmorStand barrier = barriers.remove(player.getUniqueId());
        if (barrier != null) {
            try {
                barrier.discard();
            } catch (Exception e) {
                plugin.logException(e);
            }
        }
    }

    @Override
    public void onDisable() {
        for (UUID uuid : barriers.keySet()) {
            removeBarrier(Bukkit.getPlayer(uuid));
        }
        barriers.clear();
    }

    @Override
    public void onEnable() {
        for (UUID vanishedUUID : plugin.getVanishStateMgr().getOnlineVanishedPlayers()) {
            Player vanished = Bukkit.getPlayer(vanishedUUID);
            if (vanished != null && vanished.isOnline()) {
                createBarrier(vanished);
            }
        }
    }
}
