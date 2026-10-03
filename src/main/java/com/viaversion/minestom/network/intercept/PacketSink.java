package com.viaversion.minestom.network.intercept;

import net.minestom.server.network.NetworkBuffer;

/**
 * The next step an outgoing packet takes on its way to the client.
 */
@FunctionalInterface
public interface PacketSink {

    /**
     * Passes a packet on. The buffer starts with the packet id and is consumed before this method returns.
     */
    void write(NetworkBuffer packet);
}
