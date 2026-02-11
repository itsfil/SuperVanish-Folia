/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.features;

import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class SilentOpenChestPacketAdapter {

    private final SilentOpenChest silentOpenChest;

    public SilentOpenChestPacketAdapter(SilentOpenChest silentOpenChest) {
        this.silentOpenChest = silentOpenChest;
    }

    public ClientboundPlayerInfoUpdatePacket handlePlayerInfo(Player receiver, ClientboundPlayerInfoUpdatePacket packet) {
        if (silentOpenChest.plugin.getVanishStateMgr().getOnlineVanishedPlayers().isEmpty()) return packet;
        
        try {
            var entries = packet.entries();
            var modifiedEntries = entries.stream()
                    .map(entry -> {
                        if (!silentOpenChest.plugin.getVisibilityChanger().getHider()
                                .isHidden(entry.profileId(), receiver)
                                && silentOpenChest.plugin.getVanishStateMgr()
                                .isVanished(entry.profileId())) {
                            Player vanishedPlayer = silentOpenChest.plugin.getServer().getPlayer(entry.profileId());
                            if (vanishedPlayer != null) {
                                ServerPlayer nmsPlayer = ((CraftPlayer) vanishedPlayer).getHandle();
                                if (nmsPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR
                                        && silentOpenChest.hasSilentlyOpenedChest(vanishedPlayer)) {
                                    return new ClientboundPlayerInfoUpdatePacket.Entry(
                                            entry.profileId(),
                                            entry.profile(),
                                            entry.listed(),
                                            entry.latency(),
                                            GameType.SURVIVAL,
                                            entry.displayName(),
                                            entry.listed(),
                                            0,
                                            entry.chatSession()
                                    );
                                }
                            }
                        }
                        return entry;
                    })
                    .toList();
            
            return new ClientboundPlayerInfoUpdatePacket(packet.actions(), modifiedEntries);
        } catch (Exception e) {
            silentOpenChest.plugin.logException(e);
        }
        return packet;
    }

    public ClientboundPlayerAbilitiesPacket handleAbilities(Player receiver, ClientboundPlayerAbilitiesPacket packet) {
        if (silentOpenChest.plugin.getVanishStateMgr().isVanished(receiver.getUniqueId())) {
            if (silentOpenChest.hasSilentlyOpenedChest(receiver)) {
                return null;
            }
        }
        return packet;
    }

    public ClientboundSetEntityDataPacket handleEntityMetadata(Player receiver, ClientboundSetEntityDataPacket packet) {
        try {
            int entityId = packet.id();
            if (entityId == receiver.getEntityId()) {
                if (silentOpenChest.plugin.getVanishStateMgr().isVanished(receiver.getUniqueId())) {
                    if (silentOpenChest.hasSilentlyOpenedChest(receiver)) {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            silentOpenChest.plugin.logException(e);
        }
        return packet;
    }
}
