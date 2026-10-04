package com.viaversion.minestom.network.bridge;

import io.netty.buffer.ByteBuf;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface BridgeHost {

    @Nullable Player player();

    void outbound(ByteBuf packet);

    void inbound(ByteBuf packet);

    void closed();
}
