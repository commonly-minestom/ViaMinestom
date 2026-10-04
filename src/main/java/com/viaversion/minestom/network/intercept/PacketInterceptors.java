package com.viaversion.minestom.network.intercept;

import com.viaversion.minestom.network.connection.ViaPlayerConnection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PacketInterceptors {
    private static final PacketInterceptor[] NONE = new PacketInterceptor[0];
    private static final List<PacketInterceptorFactory> FACTORIES = new CopyOnWriteArrayList<>();

    private PacketInterceptors() {
    }

    public static void register(final PacketInterceptorFactory factory) {
        FACTORIES.add(factory);
    }

    public static void unregister(final PacketInterceptorFactory factory) {
        FACTORIES.remove(factory);
    }

    public static PacketInterceptor[] create(final ViaPlayerConnection connection) {
        if (FACTORIES.isEmpty()) {
            return NONE;
        }

        final Object[] factories = FACTORIES.toArray();
        final PacketInterceptor[] interceptors = new PacketInterceptor[factories.length];
        for (int i = 0; i < factories.length; i++) {
            interceptors[i] = ((PacketInterceptorFactory) factories[i]).create(connection);
        }
        return interceptors;
    }
}
