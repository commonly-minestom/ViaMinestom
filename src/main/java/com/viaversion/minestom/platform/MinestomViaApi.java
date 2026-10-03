package com.viaversion.minestom.platform;

import com.viaversion.viaversion.ViaAPIBase;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import net.minestom.server.entity.Player;

public final class MinestomViaApi extends ViaAPIBase<Player> {

    @Override
    public ProtocolVersion getPlayerProtocolVersion(final Player player) {
        return getPlayerProtocolVersion(player.getUuid());
    }

    @Override
    public void sendRawPacket(final Player player, final ByteBuf packet) {
        sendRawPacket(player.getUuid(), packet);
    }
}
