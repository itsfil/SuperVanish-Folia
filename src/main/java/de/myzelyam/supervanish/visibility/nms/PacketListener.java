package de.myzelyam.supervanish.visibility.nms;

import ca.spottedleaf.concurrentutil.map.SWMRHashTable;
import de.myzelyam.supervanish.SuperVanish;
import io.netty.channel.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PacketListener {
    private final SuperVanish plugin;
    private final ConcurrentHashMap<UUID, ChannelHandler> injectedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> vanishedEntityIds = new SWMRHashTable<>();
    private final Map<Integer, UUID> entityIdToUuid = new SWMRHashTable<>();
    private final Map<String, Boolean> filterCache = new SWMRHashTable<>();
    private static final int MAX_CACHE_SIZE = 10000;

    public PacketListener(SuperVanish plugin) {
        this.plugin = plugin;
    }

    public void trackVanishedPlayer(Player player) {
        ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        int entityId = nmsPlayer.getId();
        UUID uuid = player.getUniqueId();
        
        vanishedEntityIds.put(uuid, entityId);
        entityIdToUuid.put(entityId, uuid);
    }

    public void untrackVanishedPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Integer entityId = vanishedEntityIds.remove(uuid);
        if (entityId != null) {
            entityIdToUuid.remove(entityId);
        }
        filterCache.keySet().removeIf(key -> key.startsWith(uuid.toString()));
    }

    private boolean isVanishedEntityId(int entityId) {
        return entityIdToUuid.containsKey(entityId);
    }

    private UUID getVanishedPlayerByEntityId(int entityId) {
        return entityIdToUuid.get(entityId);
    }

    private boolean shouldFilterPacket(UUID receiverId, UUID vanishedId) {
        Player receiver = plugin.getServer().getPlayer(receiverId);
        Player vanished = plugin.getServer().getPlayer(vanishedId);
        
        if (receiver == null || vanished == null) return true;
        if (plugin.hasPermissionToSee(receiver, vanished)) return false;
        
        String key = receiverId.toString() + ":" + vanishedId.toString();
        Boolean cached = filterCache.get(key);
        
        if (cached != null) return cached;
        
        boolean result = true;
        if (filterCache.size() < MAX_CACHE_SIZE) {
            filterCache.put(key, result);
        }
        
        return result;
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
        } else if (packet instanceof ClientboundAddEntityPacket) {
            return handleEntitySpawn(receiver, (ClientboundAddEntityPacket) packet);
        } else if (packet instanceof ClientboundSetEntityDataPacket) {
            return handleEntityMetadata(receiver, (ClientboundSetEntityDataPacket) packet);
        } else if (packet instanceof ClientboundMoveEntityPacket) {
            return handleEntityMove(receiver, (ClientboundMoveEntityPacket) packet);
        } else if (packet instanceof ClientboundTeleportEntityPacket) {
            return handleEntityTeleport(receiver, (ClientboundTeleportEntityPacket) packet);
        } else if (packet instanceof ClientboundRotateHeadPacket) {
            return handleHeadRotation(receiver, (ClientboundRotateHeadPacket) packet);
        } else if (packet instanceof ClientboundSetEquipmentPacket) {
            return handleEntityEquipment(receiver, (ClientboundSetEquipmentPacket) packet);
        } else if (packet instanceof ClientboundSoundPacket) {
            return handleSound(receiver, (ClientboundSoundPacket) packet);
        } else if (packet instanceof ClientboundSoundEntityPacket) {
            return handleEntitySound(receiver, (ClientboundSoundEntityPacket) packet);
        } else if (packet instanceof ClientboundLevelParticlesPacket) {
            return handleParticles(receiver, (ClientboundLevelParticlesPacket) packet);
        } else if (packet instanceof ClientboundBlockDestructionPacket) {
            return handleBlockDestruction(receiver, (ClientboundBlockDestructionPacket) packet);
        } else if (packet instanceof ClientboundAnimatePacket) {
            return handleAnimation(receiver, (ClientboundAnimatePacket) packet);
        } else if (packet instanceof ClientboundHurtAnimationPacket) {
            return handleHurtAnimation(receiver, (ClientboundHurtAnimationPacket) packet);
        } else if (packet instanceof ClientboundUpdateMobEffectPacket) {
            return handleMobEffect(receiver, (ClientboundUpdateMobEffectPacket) packet);
        } else if (packet instanceof ClientboundRemoveMobEffectPacket) {
            return handleRemoveMobEffect(receiver, (ClientboundRemoveMobEffectPacket) packet);
        } else if (packet instanceof ClientboundSetEntityLinkPacket) {
            return handleEntityAttach(receiver, (ClientboundSetEntityLinkPacket) packet);
        } else if (packet instanceof ClientboundSetPassengersPacket) {
            return handleEntityPassengers(receiver, (ClientboundSetPassengersPacket) packet);
        } else if (packet instanceof ClientboundDamageEventPacket) {
            return handleDamageEvent(receiver, (ClientboundDamageEventPacket) packet);
        }
        return packet;
    }

    private boolean handleIncomingPacket(Player player, Packet<?> packet) {
        if (packet instanceof ServerboundInteractPacket) {
            return handleInteract(player, (ServerboundInteractPacket) packet);
        }
        return true;
    }

    private boolean handleInteract(Player player, ServerboundInteractPacket packet) {
        try {
            int entityId = packet.getEntityId();
            if (isVanishedEntityId(entityId)) {
                UUID vanishedUuid = getVanishedPlayerByEntityId(entityId);
                if (vanishedUuid != null && shouldFilterPacket(player.getUniqueId(), vanishedUuid)) {
                    return false;
                }
            }
        } catch (Exception e) {
        }
        return true;
    }

    private Packet<?> handleEntitySpawn(Player receiver, ClientboundAddEntityPacket packet) {
        if (isVanishedEntityId(packet.getId())) {
            UUID vanishedUuid = getVanishedPlayerByEntityId(packet.getId());
            if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                return null;
            }
        }
        return packet;
    }

    private Packet<?> handleEntityMetadata(Player receiver, ClientboundSetEntityDataPacket packet) {
        if (isVanishedEntityId(packet.id())) {
            UUID vanishedUuid = getVanishedPlayerByEntityId(packet.id());
            if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                return null;
            }
        }
        return packet;
    }

    private Packet<?> handleEntityMove(Player receiver, ClientboundMoveEntityPacket packet) {
        try {
            int entityId = packet.getEntity(null).getId();
            if (isVanishedEntityId(entityId)) {
                UUID vanishedUuid = getVanishedPlayerByEntityId(entityId);
                if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                    return null;
                }
            }
        } catch (Exception e) {
        }
        return packet;
    }

    private Packet<?> handleEntityTeleport(Player receiver, ClientboundTeleportEntityPacket packet) {
        if (isVanishedEntityId(packet.getId())) {
            UUID vanishedUuid = getVanishedPlayerByEntityId(packet.getId());
            if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                return null;
            }
        }
        return packet;
    }

    private Packet<?> handleHeadRotation(Player receiver, ClientboundRotateHeadPacket packet) {
        try {
            int entityId = packet.getEntity(null).getId();
            if (isVanishedEntityId(entityId)) {
                UUID vanishedUuid = getVanishedPlayerByEntityId(entityId);
                if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                    return null;
                }
            }
        } catch (Exception e) {
        }
        return packet;
    }

    private Packet<?> handleEntityEquipment(Player receiver, ClientboundSetEquipmentPacket packet) {
        if (isVanishedEntityId(packet.getEntity())) {
            UUID vanishedUuid = getVanishedPlayerByEntityId(packet.getEntity());
            if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                return null;
            }
        }
        return packet;
    }

    private Packet<?> handleSound(Player receiver, ClientboundSoundPacket packet) {
        if (plugin.getVanishStateMgr().getOnlineVanishedPlayers().isEmpty()) return packet;
        
        try {
            double x = packet.getX();
            double y = packet.getY();
            double z = packet.getZ();
            
            for (UUID vanishedUuid : plugin.getVanishStateMgr().getOnlineVanishedPlayers()) {
                Player vanished = plugin.getServer().getPlayer(vanishedUuid);
                if (vanished != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                    double distance = Math.sqrt(
                        Math.pow(vanished.getLocation().getX() - x, 2) +
                        Math.pow(vanished.getLocation().getY() - y, 2) +
                        Math.pow(vanished.getLocation().getZ() - z, 2)
                    );
                    
                    if (distance < 2.0) {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
        }
        return packet;
    }

    private Packet<?> handleEntitySound(Player receiver, ClientboundSoundEntityPacket packet) {
        try {
            int entityId = packet.getId();
            if (isVanishedEntityId(entityId)) {
                UUID vanishedUuid = getVanishedPlayerByEntityId(entityId);
                if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                    return null;
                }
            }
        } catch (Exception e) {
        }
        return packet;
    }

    private Packet<?> handleParticles(Player receiver, ClientboundLevelParticlesPacket packet) {
        if (plugin.getVanishStateMgr().getOnlineVanishedPlayers().isEmpty()) return packet;
        
        try {
            double x = packet.getX();
            double y = packet.getY();
            double z = packet.getZ();
            
            for (UUID vanishedUuid : plugin.getVanishStateMgr().getOnlineVanishedPlayers()) {
                Player vanished = plugin.getServer().getPlayer(vanishedUuid);
                if (vanished != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                    double distance = Math.sqrt(
                        Math.pow(vanished.getLocation().getX() - x, 2) +
                        Math.pow(vanished.getLocation().getY() - y, 2) +
                        Math.pow(vanished.getLocation().getZ() - z, 2)
                    );
                    
                    if (distance < 3.0) {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
        }
        return packet;
    }

    private Packet<?> handleBlockDestruction(Player receiver, ClientboundBlockDestructionPacket packet) {
        if (isVanishedEntityId(packet.getId())) {
            UUID vanishedUuid = getVanishedPlayerByEntityId(packet.getId());
            if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                return null;
            }
        }
        return packet;
    }

    private Packet<?> handleAnimation(Player receiver, ClientboundAnimatePacket packet) {
        if (isVanishedEntityId(packet.getId())) {
            UUID vanishedUuid = getVanishedPlayerByEntityId(packet.getId());
            if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                return null;
            }
        }
        return packet;
    }

    private Packet<?> handleHurtAnimation(Player receiver, ClientboundHurtAnimationPacket packet) {
        if (isVanishedEntityId(packet.id())) {
            UUID vanishedUuid = getVanishedPlayerByEntityId(packet.id());
            if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                return null;
            }
        }
        return packet;
    }

    private Packet<?> handleMobEffect(Player receiver, ClientboundUpdateMobEffectPacket packet) {
        if (isVanishedEntityId(packet.getEntityId())) {
            UUID vanishedUuid = getVanishedPlayerByEntityId(packet.getEntityId());
            if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                return null;
            }
        }
        return packet;
    }

    private Packet<?> handleRemoveMobEffect(Player receiver, ClientboundRemoveMobEffectPacket packet) {
        if (isVanishedEntityId(packet.getEntityId())) {
            UUID vanishedUuid = getVanishedPlayerByEntityId(packet.getEntityId());
            if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                return null;
            }
        }
        return packet;
    }

    private Packet<?> handleEntityAttach(Player receiver, ClientboundSetEntityLinkPacket packet) {
        try {
            if (isVanishedEntityId(packet.getSourceId()) || isVanishedEntityId(packet.getDestId())) {
                UUID vanishedUuid = getVanishedPlayerByEntityId(packet.getSourceId());
                if (vanishedUuid == null) {
                    vanishedUuid = getVanishedPlayerByEntityId(packet.getDestId());
                }
                if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                    return null;
                }
            }
        } catch (Exception e) {
        }
        return packet;
    }

    private Packet<?> handleEntityPassengers(Player receiver, ClientboundSetPassengersPacket packet) {
        try {
            if (isVanishedEntityId(packet.getVehicle())) {
                UUID vanishedUuid = getVanishedPlayerByEntityId(packet.getVehicle());
                if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                    return null;
                }
            }
            
            for (int passengerId : packet.getPassengers()) {
                if (isVanishedEntityId(passengerId)) {
                    UUID vanishedUuid = getVanishedPlayerByEntityId(passengerId);
                    if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
        }
        return packet;
    }

    private Packet<?> handleDamageEvent(Player receiver, ClientboundDamageEventPacket packet) {
        try {
            if (isVanishedEntityId(packet.entityId())) {
                UUID vanishedUuid = getVanishedPlayerByEntityId(packet.entityId());
                if (vanishedUuid != null && shouldFilterPacket(receiver.getUniqueId(), vanishedUuid)) {
                    return null;
                }
            }
        } catch (Exception e) {
        }
        return packet;
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
        vanishedEntityIds.clear();
        entityIdToUuid.clear();
        filterCache.clear();
    }
}
