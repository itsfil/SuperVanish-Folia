package de.myzelyam.supervanish.visibility.nms;

import de.myzelyam.supervanish.SuperVanish;
import io.netty.channel.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PacketListener {
    private final SuperVanish plugin;
    private final ConcurrentHashMap<UUID, ChannelHandler> injectedPlayers = new ConcurrentHashMap<>();

    public PacketListener(SuperVanish plugin) {
        this.plugin = plugin;
    }

    public void injectPlayer(Player player) {
        if (injectedPlayers.containsKey(player.getUniqueId())) return;

        ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        Connection connection = nmsPlayer.connection.connection;
        Channel channel = connection.channel;

        ChannelHandler handler = new ChannelDuplexHandler() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof Packet<?>) {
                    Packet<?> packet = (Packet<?>) msg;
                    Packet<?> modified = handleOutgoingPacket(player, packet);
                    if (modified == null) return;
                    msg = modified;
                }
                super.write(ctx, msg, promise);
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof Packet<?>) {
                    Packet<?> packet = (Packet<?>) msg;
                    if (!handleIncomingPacket(player, packet)) return;
                }
                super.channelRead(ctx, msg);
            }
        };

        try {
            channel.pipeline().addBefore("packet_handler", "supervanish_handler", handler);
            injectedPlayers.put(player.getUniqueId(), handler);
        } catch (Exception e) {
            plugin.logException(e);
        }
    }

    public void uninjectPlayer(Player player) {
        ChannelHandler handler = injectedPlayers.remove(player.getUniqueId());
        if (handler == null) return;

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
            Connection connection = nmsPlayer.connection.connection;
            Channel channel = connection.channel;
            
            if (channel.pipeline().get("supervanish_handler") != null) {
                channel.pipeline().remove("supervanish_handler");
            }
        } catch (Exception e) {
            plugin.logException(e);
        }
    }

    private Packet<?> handleOutgoingPacket(Player receiver, Packet<?> packet) {
        if (packet instanceof ClientboundCommandSuggestionsPacket) {
            return handleTabComplete(receiver, (ClientboundCommandSuggestionsPacket) packet);
        } else if (packet instanceof ClientboundPlayerInfoUpdatePacket) {
            return handlePlayerInfo(receiver, (ClientboundPlayerInfoUpdatePacket) packet);
        }
        return packet;
    }

    private boolean handleIncomingPacket(Player player, Packet<?> packet) {
        return true;
    }

    private Packet<?> handleTabComplete(Player receiver, ClientboundCommandSuggestionsPacket packet) {
        if (plugin.getVanishStateMgr().getOnlineVanishedPlayers().isEmpty()) return packet;

        try {
            var suggestions = packet.suggestions();
            var filteredList = suggestions.getList().stream()
                    .filter(suggestion -> {
                        String text = suggestion.getText();
                        if (text.contains("/")) return true;
                        
                        for (UUID vanishedUUID : plugin.getVanishStateMgr().getOnlineVanishedPlayers()) {
                            Player vanished = plugin.getServer().getPlayer(vanishedUUID);
                            if (vanished != null && text.equalsIgnoreCase(vanished.getName())) {
                                if (!plugin.hasPermissionToSee(receiver, vanished)) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    })
                    .toList();

            if (filteredList.size() < suggestions.getList().size()) {
                return new ClientboundCommandSuggestionsPacket(
                        packet.id(),
                        new com.mojang.brigadier.suggestion.Suggestions(
                                suggestions.getRange(),
                                filteredList
                        )
                );
            }
        } catch (Exception e) {
            plugin.logException(e);
        }

        return packet;
    }

    private Packet<?> handlePlayerInfo(Player receiver, ClientboundPlayerInfoUpdatePacket packet) {
        if (plugin.getVanishStateMgr().getOnlineVanishedPlayers().isEmpty()) return packet;

        try {
            var entries = packet.entries();
            var filteredEntries = entries.stream()
                    .filter(entry -> {
                        if (plugin.getVanishStateMgr().isVanished(entry.profileId())) {
                            Player vanished = plugin.getServer().getPlayer(entry.profileId());
                            return vanished != null && plugin.hasPermissionToSee(receiver, vanished);
                        }
                        return true;
                    })
                    .toList();

            if (filteredEntries.size() < entries.size()) {
                return new ClientboundPlayerInfoUpdatePacket(packet.actions(), filteredEntries);
            }
        } catch (Exception e) {
            plugin.logException(e);
        }

        return packet;
    }

    public void uninjectAll() {
        for (UUID uuid : injectedPlayers.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                uninjectPlayer(player);
            }
        }
        injectedPlayers.clear();
    }
}
