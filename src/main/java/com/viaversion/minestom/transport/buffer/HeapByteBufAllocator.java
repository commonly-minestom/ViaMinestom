package com.viaversion.minestom.transport.buffer;

final class HeapByteBufAllocator implements ByteBufAllocator {
    private static final int DEFAULT_INITIAL_CAPACITY = 256;
    private static final int DEFAULT_MAX_CAPACITY = Integer.MAX_VALUE;

    @Override
    public ByteBuf buffer() {
        return heapBuffer();
    }

    @Override
    public ByteBuf buffer(final int initialCapacity) {
        return heapBuffer(initialCapacity);
    }

    @Override
    public ByteBuf buffer(final int initialCapacity, final int maxCapacity) {
        return heapBuffer(initialCapacity, maxCapacity);
    }

    @Override
    public ByteBuf heapBuffer() {
        return heapBuffer(DEFAULT_INITIAL_CAPACITY, DEFAULT_MAX_CAPACITY);
    }

    @Override
    public ByteBuf heapBuffer(final int initialCapacity) {
        return heapBuffer(initialCapacity, DEFAULT_MAX_CAPACITY);
    }

    @Override
    public ByteBuf heapBuffer(final int initialCapacity, final int maxCapacity) {
        if (initialCapacity < 0 || initialCapacity > maxCapacity) {
            throw new IllegalArgumentException("initialCapacity " + initialCapacity + " is outside of [0, " + maxCapacity + "]");
        }
        return new HeapByteBuf(this, new byte[initialCapacity], 0, maxCapacity);
    }
}
