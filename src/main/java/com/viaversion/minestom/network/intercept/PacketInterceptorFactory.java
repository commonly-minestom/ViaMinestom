package com.viaversion.minestom.network.intercept;

import com.viaversion.minestom.network.connection.ViaPlayerConnection;

@FunctionalInterface
public interface PacketInterceptorFactory {

    /**
     * Creates the interceptor of a connection that has just been accepted.
     */
    PacketInterceptor create(ViaPlayerConnection connection);
}
