package com.viaversion.minestom.network.bridge;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.lang.foreign.MemorySegment;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.Registries;
import org.jetbrains.annotations.Nullable;

public final class ByteBufs {

    private ByteBufs() {
    }

    public static ByteBuf copy(final ByteBufAllocator allocator, final NetworkBuffer packet) {
        final int size = Math.toIntExact(packet.readableBytes());
        final ByteBuf buf = allocator.buffer(size);
        packet.copyTo(packet.readIndex(), MemorySegment.ofBuffer(buf.nioBuffer(0, size)), 0, size);
        return buf.writerIndex(size);
    }

    public static NetworkBuffer view(final ByteBuf buf, final @Nullable Registries registries) {
        final int size = buf.readableBytes();
        if (size == 0) {
            return NetworkBuffer.wrap(new byte[0], 0, 0, registries);
        }
        return NetworkBuffer.wrap(MemorySegment.ofBuffer(buf.nioBuffer()), 0, size, registries);
    }
}
