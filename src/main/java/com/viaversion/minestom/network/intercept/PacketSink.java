package com.viaversion.minestom.network.intercept;

import net.minestom.server.network.NetworkBuffer;

@FunctionalInterface
public interface PacketSink {
    void write(NetworkBuffer packet);
}
