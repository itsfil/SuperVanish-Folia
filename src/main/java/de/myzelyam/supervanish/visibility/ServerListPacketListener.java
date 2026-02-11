/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility;

import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;

import de.myzelyam.supervanish.SuperVanish;

public class ServerListPacketListener {

    private final SuperVanish plugin;

    public ServerListPacketListener(SuperVanish plugin) {
        this.plugin = plugin;
    }

    public static void register(SuperVanish plugin) {
        try {
            Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
            plugin.getLogger().log(Level.INFO, "Using Paper API for server list ping support");
            plugin.getServer().getPluginManager().registerEvents(new PaperServerPingListener(plugin), plugin);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.WARNING, "Paper API not available for server list ping");
        }
    }

    public static boolean isEnabled(SuperVanish plugin) {
        final FileConfiguration config = plugin.getSettings();
        return config.getBoolean(
                "ExternalInvisibility.ServerList.AdjustAmountOfOnlinePlayers")
                || config.getBoolean(
                "ExternalInvisibility.ServerList.AdjustListOfLoggedInPlayers");
    }
}