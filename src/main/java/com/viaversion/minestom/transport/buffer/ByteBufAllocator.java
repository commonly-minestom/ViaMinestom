package com.viaversion.minestom.transport.buffer;

public interface ByteBufAllocator {
    ByteBufAllocator DEFAULT = new HeapByteBufAllocator();

    ByteBuf buffer();

    ByteBuf buffer(int initialCapacity);

    ByteBuf buffer(int initialCapacity, int maxCapacity);

    ByteBuf heapBuffer();

    ByteBuf heapBuffer(int initialCapacity);

    ByteBuf heapBuffer(int initialCapacity, int maxCapacity);
}
