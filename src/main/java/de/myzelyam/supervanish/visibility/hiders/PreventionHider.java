/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility.hiders;

import de.myzelyam.supervanish.SuperVanish;
import de.myzelyam.supervanish.scheduler.ScheduledTask;
import de.myzelyam.supervanish.utils.BukkitPlayerHidingUtil;
import org.bukkit.entity.Player;

public class PreventionHider extends PlayerHider implements Runnable {

    private ScheduledTask task;

    public PreventionHider(SuperVanish plugin) {
        super(plugin);
        if (!BukkitPlayerHidingUtil.isNewPlayerHidingAPISupported(plugin)) {
            task = SuperVanish.getScheduler().runTaskTimer(this, 2, 2);
        }
    }

    @Override
    public boolean setHidden(Player player, Player viewer, boolean hidden) {
        if (super.setHidden(player, viewer, hidden) || BukkitPlayerHidingUtil.isNewPlayerHidingAPISupported(plugin)) {
            if (hidden) BukkitPlayerHidingUtil.hidePlayer(player, viewer, plugin);
            else BukkitPlayerHidingUtil.showPlayer(player, viewer, plugin);
            return true;
        }
        return false;
    }

    @Override
    public String getName() {
        return "Prevention";
    }

    @Override
    public void run() {
        if (BukkitPlayerHidingUtil.isNewPlayerHidingAPISupported(plugin)) {
            if (task != null) {
                task.cancel();
                task = null;
            }
            return;
        }
        forEachHiddenPair((hidden, viewer) -> BukkitPlayerHidingUtil.hidePlayer(hidden, viewer, plugin));
    }
}