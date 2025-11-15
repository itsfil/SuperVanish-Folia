/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility.hiders;

import de.myzelyam.supervanish.SuperVanish;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import ca.spottedleaf.concurrentutil.map.SWMRHashTable;

public abstract class PlayerHider implements Listener {

    protected final SuperVanish plugin;
    protected final Map<UUID, Set<UUID>> playerHiddenFromPlayersMap = new SWMRHashTable<>();

    public PlayerHider(SuperVanish plugin) {
        this.plugin = plugin;
        registerQuitListener();
    }

    public abstract String getName();

    public boolean isHidden(Player player, Player viewer) {
        UUID pId = player.getUniqueId();
        UUID vId = viewer.getUniqueId();
        if (pId.equals(vId)) return false;
        Set<UUID> set = playerHiddenFromPlayersMap.get(pId);
        return set != null && set.contains(vId);
    }

    public boolean isHidden(UUID playerUUID, Player viewer) {
        if (playerUUID.equals(viewer.getUniqueId())) return false;
        Set<UUID> set = playerHiddenFromPlayersMap.get(playerUUID);
        return set != null && set.contains(viewer.getUniqueId());
    }

    public boolean isHidden(String playerName, Player viewer) {
        if (playerName.equalsIgnoreCase(viewer.getName())) return false;
        Player target = Bukkit.getPlayerExact(playerName);
        return target != null && isHidden(target, viewer);
    }

    /**
     * @return TRUE if the operation changed the state, FALSE if it did not
     */
    public boolean setHidden(Player player, Player viewer, boolean hidden) {
        UUID pId = player.getUniqueId();
        UUID vId = viewer.getUniqueId();
        if (pId.equals(vId)) return false;
        Set<UUID> hiddenFromPlayers = playerHiddenFromPlayersMap.computeIfAbsent(pId, k -> new HashSet<>());
        if (hidden) {
            return hiddenFromPlayers.add(vId);
        } else {
            return hiddenFromPlayers.remove(vId);
        }
    }

    public Set<Player> getHiddenPlayerKeys() {
        Set<Player> result = new HashSet<>();
        for (UUID id : playerHiddenFromPlayersMap.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) result.add(p);
        }
        return result;
    }

    private void registerQuitListener() {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {

            @EventHandler(priority = EventPriority.MONITOR)
            public void onQuit(final PlayerQuitEvent e) {
                SuperVanish.getScheduler().runTaskLater(() -> {
                    UUID quitId = e.getPlayer().getUniqueId();
                    playerHiddenFromPlayersMap.remove(quitId);
                    for (UUID id : new java.util.ArrayList<>(playerHiddenFromPlayersMap.keySet())) {
                        Set<UUID> set = playerHiddenFromPlayersMap.get(id);
                        if (set != null) set.remove(quitId);
                    }
                }, 1L);
            }
        }, plugin);
    }

    protected void forEachHiddenPair(java.util.function.BiConsumer<Player, Player> consumer) {
        for (Map.Entry<UUID, Set<UUID>> entry : playerHiddenFromPlayersMap.entrySet()) {
            Player hidden = Bukkit.getPlayer(entry.getKey());
            if (hidden == null) continue;
            for (UUID viewerId : entry.getValue()) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null) consumer.accept(hidden, viewer);
            }
        }
    }

    protected void runLater(Runnable runnable, long delay) {
        SuperVanish.getScheduler().runTaskLater(runnable, delay);
    }

    public void clearAll() {
        playerHiddenFromPlayersMap.clear();
    }
}
