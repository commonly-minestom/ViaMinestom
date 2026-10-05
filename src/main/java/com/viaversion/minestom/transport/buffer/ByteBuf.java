package com.viaversion.minestom.transport.buffer;

import com.viaversion.minestom.transport.util.ReferenceCounted;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public abstract class ByteBuf implements ReferenceCounted {

    public abstract ByteBufAllocator alloc();

    public abstract int capacity();

    public abstract int maxCapacity();

    public abstract ByteBuf order(ByteOrder endianness);

    public abstract int readerIndex();

    public abstract ByteBuf readerIndex(int readerIndex);

    public abstract int writerIndex();

    public abstract ByteBuf writerIndex(int writerIndex);

    public abstract ByteBuf setIndex(int readerIndex, int writerIndex);

    public abstract int readableBytes();

    public abstract int writableBytes();

    public abstract boolean isReadable();

    public abstract boolean isReadable(int size);

    public abstract ByteBuf clear();

    public abstract ByteBuf markReaderIndex();

    public abstract ByteBuf resetReaderIndex();

    public abstract ByteBuf ensureWritable(int minWritableBytes);

    public abstract byte getByte(int index);

    public abstract short getUnsignedByte(int index);

    public abstract ByteBuf getBytes(int index, byte[] destination, int destinationIndex, int length);

    public abstract ByteBuf setByte(int index, int value);

    public abstract ByteBuf setShort(int index, int value);

    public abstract boolean readBoolean();

    public abstract byte readByte();

    public abstract short readUnsignedByte();

    public abstract short readShort();

    public abstract int readUnsignedShort();

    public abstract int readInt();

    public abstract long readUnsignedInt();

    public abstract long readLong();

    public abstract float readFloat();

    public abstract double readDouble();

    public abstract ByteBuf readBytes(int length);

    public abstract ByteBuf readBytes(byte[] destination);

    public abstract ByteBuf readBytes(byte[] destination, int destinationIndex, int length);

    public abstract ByteBuf readSlice(int length);

    public abstract ByteBuf skipBytes(int length);

    public abstract ByteBuf writeBoolean(boolean value);

    public abstract ByteBuf writeByte(int value);

    public abstract ByteBuf writeShort(int value);

    public abstract ByteBuf writeChar(int value);

    public abstract ByteBuf writeInt(int value);

    public abstract ByteBuf writeLong(long value);

    public abstract ByteBuf writeFloat(float value);

    public abstract ByteBuf writeDouble(double value);

    public abstract ByteBuf writeBytes(byte[] source);

    public abstract ByteBuf writeBytes(byte[] source, int sourceIndex, int length);

    public abstract ByteBuf writeBytes(ByteBuf source);

    public abstract ByteBuf writeBytes(ByteBuf source, int length);

    public abstract ByteBuf writeZero(int length);

    public abstract ByteBuf copy();

    public abstract ByteBuf copy(int index, int length);

    public abstract ByteBuf slice(int index, int length);

    public abstract boolean hasArray();

    public abstract byte[] array();

    public abstract int arrayOffset();

    public abstract String toString(int index, int length, Charset charset);

    @Override
    public abstract ByteBuf retain();
}
