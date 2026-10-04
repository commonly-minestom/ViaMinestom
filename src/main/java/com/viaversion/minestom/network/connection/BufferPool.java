package com.viaversion.minestom.network.connection;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.PacketVanilla;

interface BufferPool {

    NetworkBuffer acquire();

    void release(NetworkBuffer buffer);

    static BufferPool minestom() {
        return Minestom.INSTANCE;
    }

    enum Minestom implements BufferPool {
        INSTANCE;

        @Override
        public NetworkBuffer acquire() {
            return PacketVanilla.PACKET_POOL.get();
        }

        @Override
        public void release(final NetworkBuffer buffer) {
            PacketVanilla.PACKET_POOL.add(buffer);
        }
    }
}
