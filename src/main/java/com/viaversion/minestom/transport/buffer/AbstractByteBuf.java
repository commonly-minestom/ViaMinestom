package com.viaversion.minestom.transport.buffer;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

abstract class AbstractByteBuf extends ByteBuf {
    private int readerIndex;
    private int writerIndex;
    private int markedReaderIndex;

    protected AbstractByteBuf(final int writerIndex) {
        this.writerIndex = writerIndex;
    }

    protected abstract byte loadByte(int index);

    protected abstract short loadShort(int index);

    protected abstract int loadInt(int index);

    protected abstract long loadLong(int index);

    protected abstract void storeByte(int index, int value);

    protected abstract void storeShort(int index, int value);

    protected abstract void storeInt(int index, int value);

    protected abstract void storeLong(int index, long value);

    protected abstract void grow(int minimumCapacity);

    @Override
    public ByteBuf order(final ByteOrder endianness) {
        return Objects.requireNonNull(endianness, "endianness") == ByteOrder.BIG_ENDIAN ? this : new SwappedByteBuf(this);
    }

    @Override
    public int readerIndex() {
        return readerIndex;
    }

    @Override
    public ByteBuf readerIndex(final int readerIndex) {
        if (readerIndex < 0 || readerIndex > writerIndex) {
            throw new IndexOutOfBoundsException("readerIndex " + readerIndex + " is outside of [0, " + writerIndex + "]");
        }
        this.readerIndex = readerIndex;
        return this;
    }

    @Override
    public int writerIndex() {
        return writerIndex;
    }

    @Override
    public ByteBuf writerIndex(final int writerIndex) {
        if (writerIndex < readerIndex || writerIndex > capacity()) {
            throw new IndexOutOfBoundsException("writerIndex " + writerIndex + " is outside of [" + readerIndex + ", " + capacity() + "]");
        }
        this.writerIndex = writerIndex;
        return this;
    }

    @Override
    public ByteBuf setIndex(final int readerIndex, final int writerIndex) {
        if (readerIndex < 0 || readerIndex > writerIndex || writerIndex > capacity()) {
            throw new IndexOutOfBoundsException("Indices " + readerIndex + " and " + writerIndex + " do not fit a capacity of " + capacity());
        }
        this.readerIndex = readerIndex;
        this.writerIndex = writerIndex;
        return this;
    }

    @Override
    public int readableBytes() {
        return writerIndex - readerIndex;
    }

    @Override
    public int writableBytes() {
        return capacity() - writerIndex;
    }

    @Override
    public boolean isReadable() {
        return writerIndex > readerIndex;
    }

    @Override
    public boolean isReadable(final int size) {
        return writerIndex - readerIndex >= size;
    }

    @Override
    public ByteBuf clear() {
        readerIndex = 0;
        writerIndex = 0;
        return this;
    }

    @Override
    public ByteBuf markReaderIndex() {
        markedReaderIndex = readerIndex;
        return this;
    }

    @Override
    public ByteBuf resetReaderIndex() {
        return readerIndex(markedReaderIndex);
    }

    @Override
    public ByteBuf ensureWritable(final int minWritableBytes) {
        if (minWritableBytes < 0) {
            throw new IllegalArgumentException("minWritableBytes " + minWritableBytes + " is negative");
        }
        if (minWritableBytes > capacity() - writerIndex) {
            expand(minWritableBytes);
        }
        return this;
    }

    @Override
    public byte getByte(final int index) {
        Objects.checkIndex(index, capacity());
        return loadByte(index);
    }

    @Override
    public short getUnsignedByte(final int index) {
        return (short) (getByte(index) & 0xFF);
    }

    @Override
    public ByteBuf getBytes(final int index, final byte[] destination, final int destinationIndex, final int length) {
        Objects.checkFromIndexSize(index, length, capacity());
        System.arraycopy(array(), arrayOffset() + index, destination, destinationIndex, length);
        return this;
    }

    @Override
    public ByteBuf setByte(final int index, final int value) {
        Objects.checkIndex(index, capacity());
        storeByte(index, value);
        return this;
    }

    @Override
    public ByteBuf setShort(final int index, final int value) {
        Objects.checkFromIndexSize(index, Short.BYTES, capacity());
        storeShort(index, value);
        return this;
    }

    @Override
    public boolean readBoolean() {
        return readByte() != 0;
    }

    @Override
    public byte readByte() {
        return loadByte(consume(Byte.BYTES));
    }

    @Override
    public short readUnsignedByte() {
        return (short) (readByte() & 0xFF);
    }

    @Override
    public short readShort() {
        return loadShort(consume(Short.BYTES));
    }

    @Override
    public int readUnsignedShort() {
        return readShort() & 0xFFFF;
    }

    @Override
    public int readInt() {
        return loadInt(consume(Integer.BYTES));
    }

    @Override
    public long readUnsignedInt() {
        return readInt() & 0xFFFFFFFFL;
    }

    @Override
    public long readLong() {
        return loadLong(consume(Long.BYTES));
    }

    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public ByteBuf readBytes(final int length) {
        requireReadable(length);
        return alloc().heapBuffer(length, maxCapacity()).writeBytes(this, length);
    }

    @Override
    public ByteBuf readBytes(final byte[] destination) {
        return readBytes(destination, 0, destination.length);
    }

    @Override
    public ByteBuf readBytes(final byte[] destination, final int destinationIndex, final int length) {
        requireReadable(length);
        System.arraycopy(array(), arrayOffset() + readerIndex, destination, destinationIndex, length);
        readerIndex += length;
        return this;
    }

    @Override
    public ByteBuf readSlice(final int length) {
        requireReadable(length);
        final ByteBuf slice = slice(readerIndex, length);
        readerIndex += length;
        return slice;
    }

    @Override
    public ByteBuf skipBytes(final int length) {
        requireReadable(length);
        readerIndex += length;
        return this;
    }

    @Override
    public ByteBuf writeBoolean(final boolean value) {
        return writeByte(value ? 1 : 0);
    }

    @Override
    public ByteBuf writeByte(final int value) {
        storeByte(append(Byte.BYTES), value);
        return this;
    }

    @Override
    public ByteBuf writeShort(final int value) {
        storeShort(append(Short.BYTES), value);
        return this;
    }

    @Override
    public ByteBuf writeChar(final int value) {
        return writeShort(value);
    }

    @Override
    public ByteBuf writeInt(final int value) {
        storeInt(append(Integer.BYTES), value);
        return this;
    }

    @Override
    public ByteBuf writeLong(final long value) {
        storeLong(append(Long.BYTES), value);
        return this;
    }

    @Override
    public ByteBuf writeFloat(final float value) {
        return writeInt(Float.floatToRawIntBits(value));
    }

    @Override
    public ByteBuf writeDouble(final double value) {
        return writeLong(Double.doubleToRawLongBits(value));
    }

    @Override
    public ByteBuf writeBytes(final byte[] source) {
        return writeBytes(source, 0, source.length);
    }

    @Override
    public ByteBuf writeBytes(final byte[] source, final int sourceIndex, final int length) {
        ensureWritable(length);
        System.arraycopy(source, sourceIndex, array(), arrayOffset() + writerIndex, length);
        writerIndex += length;
        return this;
    }

    @Override
    public ByteBuf writeBytes(final ByteBuf source) {
        return writeBytes(source, source.readableBytes());
    }

    @Override
    public ByteBuf writeBytes(final ByteBuf source, final int length) {
        if (length > source.readableBytes()) {
            throw new IndexOutOfBoundsException("Transferring " + length + " bytes exceeds the " + source.readableBytes() + " readable bytes of the source");
        }
        ensureWritable(length);
        final int sourceIndex = source.readerIndex();
        source.getBytes(sourceIndex, array(), arrayOffset() + writerIndex, length);
        writerIndex += length;
        source.readerIndex(sourceIndex + length);
        return this;
    }

    @Override
    public ByteBuf writeZero(final int length) {
        ensureWritable(length);
        final int start = arrayOffset() + writerIndex;
        Arrays.fill(array(), start, start + length, (byte) 0);
        writerIndex += length;
        return this;
    }

    @Override
    public ByteBuf copy() {
        return copy(readerIndex, writerIndex - readerIndex);
    }

    @Override
    public ByteBuf copy(final int index, final int length) {
        Objects.checkFromIndexSize(index, length, capacity());
        final ByteBuf copy = alloc().heapBuffer(length, maxCapacity());
        System.arraycopy(array(), arrayOffset() + index, copy.array(), copy.arrayOffset(), length);
        return copy.writerIndex(length);
    }

    @Override
    public boolean hasArray() {
        return true;
    }

    @Override
    public String toString(final int index, final int length, final Charset charset) {
        Objects.checkFromIndexSize(index, length, capacity());
        return new String(array(), arrayOffset() + index, length, charset);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(readerIndex=" + readerIndex + ", writerIndex=" + writerIndex + ", capacity=" + capacity() + ')';
    }

    private int consume(final int size) {
        final int index = readerIndex;
        if (size > writerIndex - index) {
            throw exhausted(size);
        }
        readerIndex = index + size;
        return index;
    }

    private int append(final int size) {
        final int index = writerIndex;
        if (size > capacity() - index) {
            expand(size);
        }
        writerIndex = index + size;
        return index;
    }

    private void requireReadable(final int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length " + length + " is negative");
        }
        if (length > writerIndex - readerIndex) {
            throw exhausted(length);
        }
    }

    private void expand(final int size) {
        if (size > maxCapacity() - writerIndex) {
            throw new IndexOutOfBoundsException("Writing " + size + " bytes at " + writerIndex + " exceeds the maximum capacity of " + maxCapacity());
        }
        grow(writerIndex + size);
    }

    private IndexOutOfBoundsException exhausted(final int size) {
        return new IndexOutOfBoundsException("Reading " + size + " bytes at " + readerIndex + " exceeds the writerIndex of " + writerIndex);
    }
}
