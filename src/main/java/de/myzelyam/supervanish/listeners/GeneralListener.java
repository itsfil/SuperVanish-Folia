/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.listeners;

import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.VanishPlayer;
import de.myzelyam.supervanish.features.Broadcast;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;


public class GeneralListener implements Listener {

    private final SuperVanish plugin;

    private final FileConfiguration config;
    private final boolean preventHittingEntities;
    private final boolean disableHunger;
    private final boolean disableDamage;
    private final boolean disableMobTarget;
    private final boolean preventModifyOwnInv;

    public GeneralListener(SuperVanish plugin) {
        this.plugin = plugin;
        config = plugin.getSettings();
        preventHittingEntities = config.getBoolean("RestrictiveOptions.PreventHittingEntities");
        disableHunger = config.getBoolean("InvisibilityFeatures.DisableHunger");
        disableDamage = config.getBoolean("InvisibilityFeatures.DisableDamage");
        disableMobTarget = config.getBoolean("InvisibilityFeatures.DisableMobTarget");
        preventModifyOwnInv = config.getBoolean("RestrictiveOptions.PreventModifyingOwnInventory");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent e) {
        try {
            if (!(e.getDamager() instanceof Player)) return;
            if (!preventHittingEntities) return;
            if (e.getEntity() == null) return;
            Player p = (Player) e.getDamager();
            if (plugin.getVanishStateMgr().isVanished(p.getUniqueId())) {
                if (!p.hasPermission("sv.damageentities") && !p.hasPermission("sv.damage")) {
                    plugin.sendMessage(p, "EntityHitDenied", p);
                    e.setCancelled(true);
                }
            }
        } catch (Exception er) {
            plugin.logException(er);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent e) {
        try {
            Player p = e.getEntity();
            if (plugin.getVanishStateMgr().isVanished(p.getUniqueId())) {
                String deathMessage = e.getDeathMessage();
                e.setDeathMessage(null);
                if (deathMessage != null)
                    Broadcast.announceSilentDeath(p, plugin, deathMessage);
            }
        } catch (Exception er) {
            plugin.logException(er);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        try {
            if (e.getEntity() instanceof Player && disableHunger) {
                Player p = (Player) e.getEntity();
                if (plugin.getVanishStateMgr().isVanished(p.getUniqueId())
                        && e.getFoodLevel() <= p.getFoodLevel())
                    e.setCancelled(true);
            }
        } catch (Exception er) {
            plugin.logException(er);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        try {
            if (!(e.getEntity() instanceof Player)) return;
            Player p = (Player) e.getEntity();
            if (!disableDamage) return;
            if (plugin.getVanishStateMgr().isVanished(p.getUniqueId())) {
                e.setCancelled(true);
            }
        } catch (Exception er) {
            plugin.logException(er);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTarget(EntityTargetEvent e) {
        try {
            if (!(e.getTarget() instanceof Player)) return;
            if (!disableMobTarget) return;
            Player p = (Player) e.getTarget();
            if (plugin.getVanishStateMgr().isVanished(p.getUniqueId())) {
                e.setCancelled(true);
            }
        } catch (Exception er) {
            plugin.logException(er);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickUp(EntityPickupItemEvent e) {
        try {
            if (!(e.getEntity() instanceof Player)) return;
            Player player = (Player) e.getEntity();
            VanishPlayer vanishPlayer = plugin.getVanishPlayer(player);
            if (vanishPlayer == null || !vanishPlayer.isOnlineVanished()) return;
            if (!vanishPlayer.hasItemPickUpsEnabled())
                e.setCancelled(true);
            if (preventModifyOwnInv && !player.hasPermission("sv.modifyowninv")) {
                e.setCancelled(true);
            }
        } catch (Exception er) {
            plugin.logException(er);
        }
    }

    @EventHandler
    public void onPlayerCropTrample(PlayerInteractEvent e) {
        try {
            if (!plugin.getVanishStateMgr().isVanished(e.getPlayer().getUniqueId())) return;
            if (e.getAction() != Action.PHYSICAL) return;
            if (e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.FARMLAND)
                e.setCancelled(true);
        } catch (Exception er) {
            plugin.logException(er);
        }
    }
}
