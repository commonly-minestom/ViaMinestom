package com.viaversion.minestom.network.connection;

import net.minestom.server.network.NetworkBuffer;

@FunctionalInterface
interface Stage {

    boolean proceed(NetworkBuffer packet);
}
