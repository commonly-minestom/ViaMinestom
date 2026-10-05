package com.viaversion.minestom.transport.buffer;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

final class HeapByteBuf extends AbstractByteBuf {
    private static final VarHandle SHORT = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
    private static final AtomicIntegerFieldUpdater<HeapByteBuf> REFERENCES = AtomicIntegerFieldUpdater.newUpdater(HeapByteBuf.class, "references");
    private static final int MINIMUM_EXPANSION = 64;

    private final ByteBufAllocator allocator;
    private final int maxCapacity;
    private byte[] array;
    private volatile int references = 1;

    HeapByteBuf(final ByteBufAllocator allocator, final byte[] array, final int writerIndex, final int maxCapacity) {
        super(writerIndex);
        this.allocator = allocator;
        this.array = array;
        this.maxCapacity = maxCapacity;
    }

    @Override
    public ByteBufAllocator alloc() {
        return allocator;
    }

    @Override
    public int capacity() {
        return array.length;
    }

    @Override
    public int maxCapacity() {
        return maxCapacity;
    }

    @Override
    public ByteBuf slice(final int index, final int length) {
        Objects.checkFromIndexSize(index, length, array.length);
        return new SlicedByteBuf(this, index, length);
    }

    @Override
    public byte[] array() {
        return array;
    }

    @Override
    public int arrayOffset() {
        return 0;
    }

    @Override
    public int refCnt() {
        return references;
    }

    @Override
    public ByteBuf retain() {
        if (REFERENCES.getAndIncrement(this) <= 0) {
            REFERENCES.getAndDecrement(this);
            throw new IllegalStateException("The buffer has already been released");
        }
        return this;
    }

    @Override
    public boolean release() {
        final int remaining = REFERENCES.decrementAndGet(this);
        if (remaining < 0) {
            REFERENCES.getAndIncrement(this);
            throw new IllegalStateException("The buffer has already been released");
        }
        return remaining == 0;
    }

    @Override
    protected byte loadByte(final int index) {
        return array[index];
    }

    @Override
    protected short loadShort(final int index) {
        return (short) SHORT.get(array, index);
    }

    @Override
    protected int loadInt(final int index) {
        return (int) INT.get(array, index);
    }

    @Override
    protected long loadLong(final int index) {
        return (long) LONG.get(array, index);
    }

    @Override
    protected void storeByte(final int index, final int value) {
        array[index] = (byte) value;
    }

    @Override
    protected void storeShort(final int index, final int value) {
        SHORT.set(array, index, (short) value);
    }

    @Override
    protected void storeInt(final int index, final int value) {
        INT.set(array, index, value);
    }

    @Override
    protected void storeLong(final int index, final long value) {
        LONG.set(array, index, value);
    }

    @Override
    protected void grow(final int minimumCapacity) {
        final long doubled = Math.max((long) array.length << 1, MINIMUM_EXPANSION);
        array = Arrays.copyOf(array, (int) Math.max(minimumCapacity, Math.min(doubled, maxCapacity)));
    }
}
