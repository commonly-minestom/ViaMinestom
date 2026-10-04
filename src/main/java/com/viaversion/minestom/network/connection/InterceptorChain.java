package com.viaversion.minestom.network.connection;

import com.viaversion.minestom.network.intercept.PacketInterceptor;
import com.viaversion.minestom.network.intercept.PacketSink;
import com.viaversion.minestom.network.intercept.PacketStage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;

final class InterceptorChain {
    private final PacketInterceptor[] interceptors;

    InterceptorChain(final PacketInterceptor[] interceptors) {
        this.interceptors = interceptors;
    }

    boolean isEmpty() {
        return interceptors.length == 0;
    }

    PacketInterceptor[] snapshot() {
        return interceptors.clone();
    }

    <T extends PacketInterceptor> @Nullable T find(final Class<T> type) {
        for (final PacketInterceptor interceptor : interceptors) {
            if (type.isInstance(interceptor)) {
                return type.cast(interceptor);
            }
        }
        return null;
    }

    @Nullable NetworkBuffer inbound(final PacketStage stage, final NetworkBuffer packet) {
        NetworkBuffer current = packet;
        for (final PacketInterceptor interceptor : interceptors) {
            current = interceptor.inbound(stage, current);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    boolean outbound(final PacketStage stage, final NetworkBuffer packet, final Stage next) {
        final Walk walk = new Walk(stage, next);
        walk.write(packet);
        return walk.delivered;
    }

    void close() {
        for (final PacketInterceptor interceptor : interceptors) {
            try {
                interceptor.close();
            } catch (final RuntimeException e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        }
    }

    private final class Walk implements PacketSink {
        private final PacketStage stage;
        private final Stage next;
        private int position;
        private boolean finished;
        private boolean delivered;

        private Walk(final PacketStage stage, final Stage next) {
            this.stage = stage;
            this.next = next;
        }

        @Override
        public void write(final NetworkBuffer packet) {
            if (position < interceptors.length) {
                interceptors[position++].outbound(stage, packet, this);
                return;
            }
            if (finished) {
                throw new IllegalStateException("Packet has already been written");
            }
            finished = true;
            delivered = next.proceed(packet);
        }
    }
}
