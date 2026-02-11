/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import de.myzelyam.supervanish.SuperVanish;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ActionBarMgr {

    private final SuperVanish plugin;
    private final List<Player> actionBars = new ArrayList<>();

    public ActionBarMgr(SuperVanish plugin) {
        this.plugin = plugin;
        startTask();
    }

    private void startTask() {
        SuperVanish.getScheduler().runTaskTimer(() -> {
          for (Player p : actionBars) {
            try {
              sendActionBar(p, plugin.replacePlaceholders(plugin.getMessage("ActionBarMessage"), p));
            } catch (Exception | NoSuchMethodError | NoClassDefFoundError e) {
              plugin.logException(e);
            }
          }
        }, 1L, 2 * 20);
    }

    private void sendActionBar(Player p, String bar) {
        try {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(bar));
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            plugin.logException(e);
        }
    }

    public void addActionBar(Player p) {
        actionBars.add(p);
    }

    public void removeActionBar(Player p) {
        actionBars.remove(p);
    }
}
