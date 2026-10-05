package com.viaversion.minestom.transport.buffer;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class ByteBufInputStream extends InputStream implements DataInput {
    private final ByteBuf buffer;

    public ByteBufInputStream(final ByteBuf buffer) {
        this.buffer = Objects.requireNonNull(buffer, "buffer");
    }

    @Override
    public int available() {
        return buffer.readableBytes();
    }

    @Override
    public int read() {
        return buffer.isReadable() ? buffer.readUnsignedByte() : -1;
    }

    @Override
    public int read(final byte[] destination, final int offset, final int length) {
        Objects.checkFromIndexSize(offset, length, destination.length);
        if (length == 0) {
            return 0;
        }
        final int available = buffer.readableBytes();
        if (available == 0) {
            return -1;
        }
        final int count = Math.min(available, length);
        buffer.readBytes(destination, offset, count);
        return count;
    }

    @Override
    public int skipBytes(final int count) {
        final int skipped = Math.clamp(count, 0, buffer.readableBytes());
        buffer.skipBytes(skipped);
        return skipped;
    }

    @Override
    public void readFully(final byte[] destination) throws IOException {
        readFully(destination, 0, destination.length);
    }

    @Override
    public void readFully(final byte[] destination, final int offset, final int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, destination.length);
        require(length);
        buffer.readBytes(destination, offset, length);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    @Override
    public byte readByte() throws IOException {
        require(Byte.BYTES);
        return buffer.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return readByte() & 0xFF;
    }

    @Override
    public short readShort() throws IOException {
        require(Short.BYTES);
        return buffer.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return readShort() & 0xFFFF;
    }

    @Override
    public char readChar() throws IOException {
        return (char) readShort();
    }

    @Override
    public int readInt() throws IOException {
        require(Integer.BYTES);
        return buffer.readInt();
    }

    @Override
    public long readLong() throws IOException {
        require(Long.BYTES);
        return buffer.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public @Nullable String readLine() {
        if (!buffer.isReadable()) {
            return null;
        }
        final StringBuilder line = new StringBuilder();
        while (buffer.isReadable()) {
            final int value = buffer.readUnsignedByte();
            if (value == '\n') {
                break;
            }
            if (value == '\r') {
                if (buffer.isReadable() && buffer.getByte(buffer.readerIndex()) == '\n') {
                    buffer.skipBytes(1);
                }
                break;
            }
            line.append((char) value);
        }
        return line.toString();
    }

    @Override
    public String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    private void require(final int size) throws EOFException {
        final int available = buffer.readableBytes();
        if (size > available) {
            throw new EOFException("Expected " + size + " bytes, but only " + available + " are left");
        }
    }
}
