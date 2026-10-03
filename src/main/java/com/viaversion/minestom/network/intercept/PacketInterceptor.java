package com.viaversion.minestom.network.intercept;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;

/**
 * Observes the packets of a single connection on both sides of the translation.
 * <p>
 * An incoming packet passes the {@link PacketStage#CLIENT client stage} first and the
 * {@link PacketStage#SERVER server stage} once it is translated, an outgoing one takes the opposite way.
 * All methods are called on the event loop of the connection.
 * <p>
 * Every buffer starts with the packet id, followed by the payload. A buffer handed to an interceptor is only
 * valid for the duration of the call and has to be passed on as the very same instance, with its indices
 * untouched, unless the packet is replaced.
 */
public interface PacketInterceptor {

    /**
     * Called for every packet received from the client.
     *
     * @return the packet to pass on, or null to drop it
     */
    @Nullable NetworkBuffer inbound(PacketStage stage, NetworkBuffer packet);

    /**
     * Called for every packet sent by the server. The packet continues on its way once it is written to
     * the sink, which may happen at most once. Not doing so drops it.
     */
    void outbound(PacketStage stage, NetworkBuffer packet, PacketSink sink);

    /**
     * Called once the connection is gone.
     */
    default void close() {
    }
}
