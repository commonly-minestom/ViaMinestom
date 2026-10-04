package com.viaversion.minestom.network.connection;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.PacketParser;
import net.minestom.server.network.packet.PacketRegistry;
import net.minestom.server.network.packet.PacketVanilla;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.common.ClientCookieResponsePacket;
import net.minestom.server.network.packet.client.common.ClientKeepAlivePacket;
import net.minestom.server.network.packet.client.common.ClientPingRequestPacket;
import net.minestom.server.network.packet.client.configuration.ClientFinishConfigurationPacket;
import net.minestom.server.network.packet.client.configuration.ClientSelectKnownPacksPacket;
import net.minestom.server.network.packet.client.handshake.ClientHandshakePacket;
import net.minestom.server.network.packet.client.login.ClientEncryptionResponsePacket;
import net.minestom.server.network.packet.client.login.ClientLoginAcknowledgedPacket;
import net.minestom.server.network.packet.client.login.ClientLoginPluginResponsePacket;
import net.minestom.server.network.packet.client.login.ClientLoginStartPacket;
import net.minestom.server.network.packet.client.play.ClientCreativeInventoryActionPacket;
import net.minestom.server.network.packet.client.status.StatusRequestPacket;

final class PacketDispatcher {
    private static final Set<Class<? extends ClientPacket>> IMMEDIATE_PACKETS = Set.of(
        ClientHandshakePacket.class,
        ClientCookieResponsePacket.class,
        StatusRequestPacket.class,
        ClientPingRequestPacket.class,
        ClientKeepAlivePacket.class,
        ClientLoginStartPacket.class,
        ClientEncryptionResponsePacket.class,
        ClientLoginPluginResponsePacket.class,
        ClientSelectKnownPacksPacket.class,
        ClientLoginAcknowledgedPacket.class,
        ClientFinishConfigurationPacket.class
    );
    private static final UUID NIL_PROFILE_ID = new UUID(0, 0);
    private static final String OFFLINE_PREFIX = "OfflinePlayer:";

    private final ViaPlayerConnection connection;
    private final PacketParser.Client parser;
    private ConnectionState state = ConnectionState.HANDSHAKE;

    PacketDispatcher(final ViaPlayerConnection connection, final PacketParser.Client parser) {
        this.connection = connection;
        this.parser = parser;
    }

    void dispatch(final NetworkBuffer body) {
        final int packetId = body.read(NetworkBuffer.VAR_INT);
        final PacketRegistry.PacketInfo<? extends ClientPacket> info = parser.stateRegistry(state).packetInfo(packetId);
        if (info.packetClass() == ClientCreativeInventoryActionPacket.class && !creative()) {
            return;
        }
        final ClientPacket packet = withOfflineProfileId(info.serializer().read(body));
        state = PacketVanilla.nextClientState(packet, state);
        try {
            if (IMMEDIATE_PACKETS.contains(packet.getClass())) {
                MinecraftServer.getPacketListenerManager().processClientPacket(packet, connection);
                return;
            }
            final Player player = connection.getPlayer();
            if (player != null) {
                player.addPacketToQueue(packet);
            }
        } catch (final Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    private boolean creative() {
        final Player player = connection.getPlayer();
        return player != null && player.getGameMode() == GameMode.CREATIVE;
    }

    private static ClientPacket withOfflineProfileId(final ClientPacket packet) {
        if (packet instanceof ClientLoginStartPacket(final String username, final UUID profileId) && profileId.equals(NIL_PROFILE_ID)) {
            final UUID offlineId = UUID.nameUUIDFromBytes((OFFLINE_PREFIX + username).getBytes(StandardCharsets.UTF_8));
            return new ClientLoginStartPacket(username, offlineId);
        }
        return packet;
    }
}
