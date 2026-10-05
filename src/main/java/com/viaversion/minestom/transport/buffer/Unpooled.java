package com.viaversion.minestom.transport.buffer;

public final class Unpooled {

    private Unpooled() {
    }

    public static ByteBuf buffer() {
        return ByteBufAllocator.DEFAULT.heapBuffer();
    }

    public static ByteBuf wrappedBuffer(final byte[] array) {
        return new HeapByteBuf(ByteBufAllocator.DEFAULT, array, array.length, array.length);
    }
}
