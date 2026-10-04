package com.viaversion.minestom.network.connection;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ConnectionRegistry {
    private final Set<ViaPlayerConnection> connections = ConcurrentHashMap.newKeySet();

    public void add(final ViaPlayerConnection connection) {
        connections.add(connection);
    }

    public void remove(final ViaPlayerConnection connection) {
        connections.remove(connection);
    }

    public int size() {
        return connections.size();
    }

    public void forEach(final Consumer<ViaPlayerConnection> action) {
        connections.forEach(action);
    }

    public void closeAll(final Duration timeout) {
        final long deadline = System.nanoTime() + timeout.toNanos();
        connections.forEach(ViaPlayerConnection::disconnect);
        for (final ViaPlayerConnection connection : connections) {
            final long remaining = deadline - System.nanoTime();
            if (remaining <= 0 || !connection.awaitTermination(Duration.ofNanos(remaining))) {
                connection.closeSocket();
            }
        }
    }
}
