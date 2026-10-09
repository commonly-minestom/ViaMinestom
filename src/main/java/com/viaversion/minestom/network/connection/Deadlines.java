package com.viaversion.minestom.network.connection;

import com.viaversion.minestom.network.NetworkSettings;

record Deadlines(long readNanos, long closeNanos, long loginNanos) {

    static Deadlines of(final NetworkSettings settings) {
        return new Deadlines(settings.readTimeout().toNanos(), settings.closeTimeout().toNanos(), settings.loginTimeout().toNanos());
    }
}
