package com.viaversion.minestom.transport.buffer;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Objects;

final class SwappedByteBuf extends ByteBuf {
    private final ByteBuf buffer;

    SwappedByteBuf(final ByteBuf buffer) {
        this.buffer = buffer;
    }

    @Override
    public ByteBufAllocator alloc() {
        return buffer.alloc();
    }

    @Override
    public int capacity() {
        return buffer.capacity();
    }

    @Override
    public int maxCapacity() {
        return buffer.maxCapacity();
    }

    @Override
    public ByteBuf order(final ByteOrder endianness) {
        return Objects.requireNonNull(endianness, "endianness") == ByteOrder.LITTLE_ENDIAN ? this : buffer;
    }

    @Override
    public int readerIndex() {
        return buffer.readerIndex();
    }

    @Override
    public ByteBuf readerIndex(final int readerIndex) {
        buffer.readerIndex(readerIndex);
        return this;
    }

    @Override
    public int writerIndex() {
        return buffer.writerIndex();
    }

    @Override
    public ByteBuf writerIndex(final int writerIndex) {
        buffer.writerIndex(writerIndex);
        return this;
    }

    @Override
    public ByteBuf setIndex(final int readerIndex, final int writerIndex) {
        buffer.setIndex(readerIndex, writerIndex);
        return this;
    }

    @Override
    public int readableBytes() {
        return buffer.readableBytes();
    }

    @Override
    public int writableBytes() {
        return buffer.writableBytes();
    }

    @Override
    public boolean isReadable() {
        return buffer.isReadable();
    }

    @Override
    public boolean isReadable(final int size) {
        return buffer.isReadable(size);
    }

    @Override
    public ByteBuf clear() {
        buffer.clear();
        return this;
    }

    @Override
    public ByteBuf markReaderIndex() {
        buffer.markReaderIndex();
        return this;
    }

    @Override
    public ByteBuf resetReaderIndex() {
        buffer.resetReaderIndex();
        return this;
    }

    @Override
    public ByteBuf ensureWritable(final int minWritableBytes) {
        buffer.ensureWritable(minWritableBytes);
        return this;
    }

    @Override
    public byte getByte(final int index) {
        return buffer.getByte(index);
    }

    @Override
    public short getUnsignedByte(final int index) {
        return buffer.getUnsignedByte(index);
    }

    @Override
    public ByteBuf getBytes(final int index, final byte[] destination, final int destinationIndex, final int length) {
        buffer.getBytes(index, destination, destinationIndex, length);
        return this;
    }

    @Override
    public ByteBuf setByte(final int index, final int value) {
        buffer.setByte(index, value);
        return this;
    }

    @Override
    public ByteBuf setShort(final int index, final int value) {
        buffer.setShort(index, Short.reverseBytes((short) value));
        return this;
    }

    @Override
    public boolean readBoolean() {
        return buffer.readBoolean();
    }

    @Override
    public byte readByte() {
        return buffer.readByte();
    }

    @Override
    public short readUnsignedByte() {
        return buffer.readUnsignedByte();
    }

    @Override
    public short readShort() {
        return Short.reverseBytes(buffer.readShort());
    }

    @Override
    public int readUnsignedShort() {
        return readShort() & 0xFFFF;
    }

    @Override
    public int readInt() {
        return Integer.reverseBytes(buffer.readInt());
    }

    @Override
    public long readUnsignedInt() {
        return readInt() & 0xFFFFFFFFL;
    }

    @Override
    public long readLong() {
        return Long.reverseBytes(buffer.readLong());
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
        return buffer.readBytes(length).order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public ByteBuf readBytes(final byte[] destination) {
        buffer.readBytes(destination);
        return this;
    }

    @Override
    public ByteBuf readBytes(final byte[] destination, final int destinationIndex, final int length) {
        buffer.readBytes(destination, destinationIndex, length);
        return this;
    }

    @Override
    public ByteBuf readSlice(final int length) {
        return buffer.readSlice(length).order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public ByteBuf skipBytes(final int length) {
        buffer.skipBytes(length);
        return this;
    }

    @Override
    public ByteBuf writeBoolean(final boolean value) {
        buffer.writeBoolean(value);
        return this;
    }

    @Override
    public ByteBuf writeByte(final int value) {
        buffer.writeByte(value);
        return this;
    }

    @Override
    public ByteBuf writeShort(final int value) {
        buffer.writeShort(Short.reverseBytes((short) value));
        return this;
    }

    @Override
    public ByteBuf writeChar(final int value) {
        return writeShort(value);
    }

    @Override
    public ByteBuf writeInt(final int value) {
        buffer.writeInt(Integer.reverseBytes(value));
        return this;
    }

    @Override
    public ByteBuf writeLong(final long value) {
        buffer.writeLong(Long.reverseBytes(value));
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
        buffer.writeBytes(source);
        return this;
    }

    @Override
    public ByteBuf writeBytes(final byte[] source, final int sourceIndex, final int length) {
        buffer.writeBytes(source, sourceIndex, length);
        return this;
    }

    @Override
    public ByteBuf writeBytes(final ByteBuf source) {
        buffer.writeBytes(source);
        return this;
    }

    @Override
    public ByteBuf writeBytes(final ByteBuf source, final int length) {
        buffer.writeBytes(source, length);
        return this;
    }

    @Override
    public ByteBuf writeZero(final int length) {
        buffer.writeZero(length);
        return this;
    }

    @Override
    public ByteBuf copy() {
        return buffer.copy().order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public ByteBuf copy(final int index, final int length) {
        return buffer.copy(index, length).order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public ByteBuf slice(final int index, final int length) {
        return buffer.slice(index, length).order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public boolean hasArray() {
        return buffer.hasArray();
    }

    @Override
    public byte[] array() {
        return buffer.array();
    }

    @Override
    public int arrayOffset() {
        return buffer.arrayOffset();
    }

    @Override
    public String toString(final int index, final int length, final Charset charset) {
        return buffer.toString(index, length, charset);
    }

    @Override
    public int refCnt() {
        return buffer.refCnt();
    }

    @Override
    public ByteBuf retain() {
        buffer.retain();
        return this;
    }

    @Override
    public boolean release() {
        return buffer.release();
    }

    @Override
    public String toString() {
        return "Swapped(" + buffer + ')';
    }
}
