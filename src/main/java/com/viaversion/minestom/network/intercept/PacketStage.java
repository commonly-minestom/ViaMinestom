package com.viaversion.minestom.network.intercept;

/**
 * The two places of a connection where packets can be observed, on either side of the translation.
 */
public enum PacketStage {
    /**
     * Between the socket and ViaVersion, where packets are in the protocol version of the client.
     */
    CLIENT,
    /**
     * Between ViaVersion and Minestom, where packets are in the protocol version of the server.
     */
    SERVER
}
