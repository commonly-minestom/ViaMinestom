package com.viaversion.minestom.network.intercept;

import com.viaversion.minestom.network.connection.ViaPlayerConnection;

@FunctionalInterface
public interface PacketInterceptorFactory {
    PacketInterceptor create(ViaPlayerConnection connection);
}
