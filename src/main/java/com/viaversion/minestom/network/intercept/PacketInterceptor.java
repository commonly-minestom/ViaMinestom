package com.viaversion.minestom.network.intercept;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;

public interface PacketInterceptor {
    @Nullable NetworkBuffer inbound(PacketStage stage, NetworkBuffer packet);

    void outbound(PacketStage stage, NetworkBuffer packet, PacketSink sink);

    default void close() {
    }
}
