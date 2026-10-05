package com.viaversion.minestom.transport.buffer;

import java.util.Objects;

final class SlicedByteBuf extends AbstractByteBuf {
    private final HeapByteBuf root;
    private final int adjustment;
    private final int capacity;

    SlicedByteBuf(final HeapByteBuf root, final int adjustment, final int capacity) {
        super(capacity);
        this.root = root;
        this.adjustment = adjustment;
        this.capacity = capacity;
    }

    @Override
    public ByteBufAllocator alloc() {
        return root.alloc();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public int maxCapacity() {
        return capacity;
    }

    @Override
    public ByteBuf slice(final int index, final int length) {
        Objects.checkFromIndexSize(index, length, capacity);
        return new SlicedByteBuf(root, adjustment + index, length);
    }

    @Override
    public ByteBuf copy(final int index, final int length) {
        Objects.checkFromIndexSize(index, length, capacity);
        return root.copy(adjustment + index, length);
    }

    @Override
    public byte[] array() {
        return root.array();
    }

    @Override
    public int arrayOffset() {
        return adjustment;
    }

    @Override
    public int refCnt() {
        return root.refCnt();
    }

    @Override
    public ByteBuf retain() {
        root.retain();
        return this;
    }

    @Override
    public boolean release() {
        return root.release();
    }

    @Override
    protected byte loadByte(final int index) {
        return root.loadByte(adjustment + index);
    }

    @Override
    protected short loadShort(final int index) {
        return root.loadShort(adjustment + index);
    }

    @Override
    protected int loadInt(final int index) {
        return root.loadInt(adjustment + index);
    }

    @Override
    protected long loadLong(final int index) {
        return root.loadLong(adjustment + index);
    }

    @Override
    protected void storeByte(final int index, final int value) {
        root.storeByte(adjustment + index, value);
    }

    @Override
    protected void storeShort(final int index, final int value) {
        root.storeShort(adjustment + index, value);
    }

    @Override
    protected void storeInt(final int index, final int value) {
        root.storeInt(adjustment + index, value);
    }

    @Override
    protected void storeLong(final int index, final long value) {
        root.storeLong(adjustment + index, value);
    }

    @Override
    protected void grow(final int minimumCapacity) {
        throw new IndexOutOfBoundsException("A slice of " + capacity + " bytes cannot grow to " + minimumCapacity);
    }
}
