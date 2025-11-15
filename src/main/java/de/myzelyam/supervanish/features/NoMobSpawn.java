package de.myzelyam.supervanish.features;

import com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent;
import com.destroystokyo.paper.event.entity.PreSpawnerSpawnEvent;
import com.destroystokyo.paper.event.entity.SkeletonHorseTrapEvent;
import de.myzelyam.supervanish.SuperVanish;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.List;
import java.util.UUID;

public class NoMobSpawn extends Feature {

    private boolean suppressErrors = false;

    public NoMobSpawn(SuperVanish plugin) {
        super(plugin);
    }

    @EventHandler
    public void onSkeletonHorseTrap(SkeletonHorseTrapEvent e) {
        try {
            List<HumanEntity> humans = e.getEligibleHumans();
            int humansCount = humans.size();
            for (HumanEntity human : humans) {
                if (human instanceof Player) {
                    Player p = (Player) human;
                    if (plugin.getVanishStateMgr().isVanished(p.getUniqueId())) {
                        humansCount--;
                    }
                }
            }
            if (humansCount == 0)
                e.setCancelled(true);
        } catch (Exception er) {
            if (!suppressErrors) {
                plugin.logException(er);
                suppressErrors = true;
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(PlayerNaturallySpawnCreaturesEvent e) {
        try {
            if (plugin.getVanishStateMgr().isVanished(e.getPlayer().getUniqueId()))
                e.setCancelled(true);
        } catch (Exception er) {
            if (!suppressErrors) {
                plugin.logException(er);
                suppressErrors = true;
            }
        }
    }

    @EventHandler
    public void onEntitySpawnerSpawn(PreSpawnerSpawnEvent e) {
        final var world = e.getSpawnerLocation().getWorld();
        boolean vanishedNearby = !world.getNearbyPlayers(e.getSpawnerLocation(), 16.0, p ->
            p.getGameMode() != GameMode.SPECTATOR && plugin.getVanishStateMgr().isVanished(p.getUniqueId())
        ).isEmpty();
        if (!vanishedNearby) return;
        boolean visibleNearby = !world.getNearbyPlayers(e.getSpawnerLocation(), 16.0, p ->
            p.getGameMode() != GameMode.SPECTATOR && !plugin.getVanishStateMgr().isVanished(p.getUniqueId())
        ).isEmpty();
        if (visibleNearby) return;

        e.setCancelled(true);
    }

    @Override
    public boolean isActive() {
        return plugin.getSettings().getBoolean("InvisibilityFeatures.PreventMobSpawning", true);
    }
}
