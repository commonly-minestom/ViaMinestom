package com.viaversion.minestom.network.bridge;

import com.viaversion.minestom.transport.buffer.ByteBuf;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface BridgeHost {

    @Nullable Player player();

    void outbound(ByteBuf packet);

    void inbound(ByteBuf packet);

    void closed();
}
