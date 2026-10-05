package com.viaversion.minestom.network.bridge;

import com.viaversion.minestom.transport.buffer.ByteBuf;
import com.viaversion.minestom.transport.buffer.ByteBufAllocator;
import java.lang.foreign.MemorySegment;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.Registries;
import org.jetbrains.annotations.Nullable;

public final class ByteBufs {

    private ByteBufs() {
    }

    public static ByteBuf copy(final ByteBufAllocator allocator, final NetworkBuffer packet) {
        final int size = Math.toIntExact(packet.readableBytes());
        final ByteBuf buf = allocator.heapBuffer(size);
        packet.copyTo(packet.readIndex(), buf.array(), buf.arrayOffset(), size);
        return buf.writerIndex(size);
    }

    public static NetworkBuffer view(final ByteBuf buf, final @Nullable Registries registries) {
        final int size = buf.readableBytes();
        final MemorySegment segment = MemorySegment.ofArray(buf.array()).asSlice(buf.arrayOffset() + buf.readerIndex(), size);
        return NetworkBuffer.wrap(segment, 0, size, registries);
    }
}
