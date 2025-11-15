/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

import de.myzelyam.supervanish.hooks.PlaceholderAPIHook;
import de.myzelyam.supervanish.utils.Validation;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.maximvdw.placeholderapi.PlaceholderAPI;

public class PlaceholderConverter {

    private final SuperVanish plugin;
    private final boolean placeholderAPIEnabled;
    private final boolean mvdwPlaceholderAPIEnabled;
    private final boolean essentialsEnabled;
    private final boolean vaultEnabled;
    private final Permission cachedPermAPI;
    private final Chat cachedChatAPI;
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("\\{?&?#[a-fA-F0-9]{6}\\}?");

    public PlaceholderConverter(SuperVanish plugin) {
        this.plugin = plugin;
        this.placeholderAPIEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                && plugin.getSettings().getBoolean("HookOptions.EnablePlaceholderAPIHook", true);
        this.mvdwPlaceholderAPIEnabled = Bukkit.getPluginManager().isPluginEnabled("MVdWPlaceholderAPI")
                && plugin.getSettings().getBoolean("HookOptions.EnableMVdWPlaceholderAPIHook", true);
        this.essentialsEnabled = Bukkit.getPluginManager().isPluginEnabled("Essentials");
        this.vaultEnabled = Bukkit.getPluginManager().isPluginEnabled("Vault");
        if (vaultEnabled) {
            RegisteredServiceProvider<Permission> permService = plugin.getServer()
                .getServicesManager().getRegistration(Permission.class);
            RegisteredServiceProvider<Chat> chatService = plugin.getServer()
                .getServicesManager().getRegistration(Chat.class);
            this.cachedPermAPI = permService != null ? permService.getProvider() : null;
            this.cachedChatAPI = chatService != null ? chatService.getProvider() : null;
        } else {
            this.cachedPermAPI = null;
            this.cachedChatAPI = null;
        }
    }

    public String replacePlaceholders(String msg, Object... additionalPlayerInfo) {
        Validation.checkIsTrue("Failed to replace variables (Illegal arguments)",
                msg != null, additionalPlayerInfo != null);
        //noinspection ConstantConditions
        Validation.checkIsTrue(additionalPlayerInfo.length > 0);
        // check vararg
        final List<Object> additionalPlayerInfoList = Arrays
                .asList(additionalPlayerInfo);
        Object unspecifiedPlayer = additionalPlayerInfoList.get(0);
        String unspecifiedOtherPlayersName = null;
        if (additionalPlayerInfoList.size() > 1
                && (additionalPlayerInfoList.get(1) instanceof String
                || additionalPlayerInfoList.get(1) instanceof Player)) {
            unspecifiedOtherPlayersName = (String) (additionalPlayerInfoList
                    .get(1) instanceof Player
                    ? ((Player) additionalPlayerInfoList.get(1)).getName()
                    : additionalPlayerInfoList.get(1));
        }
        //noinspection ConstantConditions
        msg = msg.replace("\\n", "\n");
        // replace sender specific variables
        replaceVariables:
        {
            if (unspecifiedPlayer instanceof OfflinePlayer
                    && !(unspecifiedPlayer instanceof Player)) {
                // offline player
                OfflinePlayer specifiedPlayer = (OfflinePlayer) unspecifiedPlayer;
                // MVdWPlaceholderAPI
                if (mvdwPlaceholderAPIEnabled) {
                    String replaced = PlaceholderAPI.replacePlaceholders(specifiedPlayer, msg);
                    msg = replaced == null ? msg : replaced;
                }
                // replace essentials nick names
                if (essentialsEnabled) {
                    msg = msg.replace("%nick%", specifiedPlayer.getName());
                }
                // replace general variables
                msg = msg.replace("%d%", specifiedPlayer.getName())
                        .replace("%p%", specifiedPlayer.getName())
                        .replace("%tab%", specifiedPlayer.getName());
                // replace other player's name if possible
                msg = msg.replace("%other%", unspecifiedOtherPlayersName != null
                        ? unspecifiedOtherPlayersName : "UNKNOWN");
                break replaceVariables;
            }
            if (unspecifiedPlayer instanceof Player) {
                // player
                Player specifiedPlayer = (Player) unspecifiedPlayer;
                // PlaceholderAPI
                if (placeholderAPIEnabled) {
                    String replaced = PlaceholderAPIHook.translatePlaceholders(msg, specifiedPlayer);
                    //noinspection ConstantConditions
                    msg = replaced == null ? msg : replaced;
                }
                // MVdWPlaceholderAPI
                if (mvdwPlaceholderAPIEnabled) {
                    String replaced = PlaceholderAPI.replacePlaceholders(specifiedPlayer, msg);
                    msg = replaced == null ? msg : replaced;
                }
                // replace essentials nick names
                if (essentialsEnabled) {
                    Essentials ess = (Essentials) Bukkit.getServer()
                            .getPluginManager().getPlugin("Essentials");
                    User u = ess.getUser(specifiedPlayer);
                    if (u != null)
                        if (u.getNickname() != null)
                            msg = msg.replace("%nick%", u.getNickname());
                }
                // replace vault info
                if (vaultEnabled) {
                    Permission permAPI = cachedPermAPI;
                    Chat chatAPI = cachedChatAPI;
                    try {
                        if (permAPI != null) {
                            String group = permAPI.getPrimaryGroup(specifiedPlayer);
                            if (group != null)
                                msg = msg.replace("%group%", group);
                        }
                        if (chatAPI != null) {
                            String prefix = chatAPI.getPlayerPrefix(specifiedPlayer);
                            String suffix = chatAPI.getPlayerSuffix(specifiedPlayer);
                            if (prefix != null) {
                                msg = msg.replace("%prefix%", prefix);
                            }
                            if (suffix != null) {
                                msg = msg.replace("%suffix%", suffix);
                            }
                        }
                    } catch (UnsupportedOperationException ignored) {
                    }
                }
                // replace general variables
                msg = msg.replace("%d%", "" + specifiedPlayer.getDisplayName())
                        .replace("%p%", "" + specifiedPlayer.getName())
                        .replace("%tab%",
                                "" + specifiedPlayer.getPlayerListName());
                // replace other player's name if possible
                msg = msg.replace("%other%", unspecifiedOtherPlayersName != null
                        ? unspecifiedOtherPlayersName : "UNKNOWN");

                break replaceVariables;
            }
            if (unspecifiedPlayer instanceof CommandSender) {
                // console
                // replace general variables
                msg = msg.replace("%d%", "Console").replace("%p%", "Console")
                        .replace("%tab%", "Console");
                // replace other player's name if possible
                msg = msg.replace("%other%", unspecifiedOtherPlayersName != null
                        ? unspecifiedOtherPlayersName : "UNKNOWN");
            }
        }
        // convert color codes
        if (plugin.getVersionUtil().isOneDotXOrHigher(16)) {
            Matcher matcher = HEX_COLOR_PATTERN.matcher(msg);
            StringBuffer sb = null;
            while (matcher.find()) {
                if (sb == null) sb = new StringBuffer();
                String color = matcher.group();
                String replacement = net.md_5.bungee.api.ChatColor
                        .of(color.replace("&", "").replace("{", "").replace("}", ""))
                        .toString();
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            if (sb != null) {
                matcher.appendTail(sb);
                msg = sb.toString();
            }
        }
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        return msg;
    }
}
