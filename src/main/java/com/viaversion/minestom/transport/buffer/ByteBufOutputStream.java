package com.viaversion.minestom.transport.buffer;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class ByteBufOutputStream extends OutputStream implements DataOutput {
    private final ByteBuf buffer;
    private @Nullable DataOutputStream textEncoder;

    public ByteBufOutputStream(final ByteBuf buffer) {
        this.buffer = Objects.requireNonNull(buffer, "buffer");
    }

    @Override
    public void write(final int value) {
        buffer.writeByte(value);
    }

    @Override
    public void write(final byte[] source) {
        buffer.writeBytes(source);
    }

    @Override
    public void write(final byte[] source, final int offset, final int length) {
        buffer.writeBytes(source, offset, length);
    }

    @Override
    public void writeBoolean(final boolean value) {
        buffer.writeBoolean(value);
    }

    @Override
    public void writeByte(final int value) {
        buffer.writeByte(value);
    }

    @Override
    public void writeShort(final int value) {
        buffer.writeShort(value);
    }

    @Override
    public void writeChar(final int value) {
        buffer.writeChar(value);
    }

    @Override
    public void writeInt(final int value) {
        buffer.writeInt(value);
    }

    @Override
    public void writeLong(final long value) {
        buffer.writeLong(value);
    }

    @Override
    public void writeFloat(final float value) {
        buffer.writeFloat(value);
    }

    @Override
    public void writeDouble(final double value) {
        buffer.writeDouble(value);
    }

    @Override
    public void writeBytes(final String value) {
        final int length = value.length();
        for (int i = 0; i < length; i++) {
            buffer.writeByte(value.charAt(i));
        }
    }

    @Override
    public void writeChars(final String value) {
        final int length = value.length();
        for (int i = 0; i < length; i++) {
            buffer.writeChar(value.charAt(i));
        }
    }

    @Override
    public void writeUTF(final String value) throws IOException {
        DataOutputStream encoder = textEncoder;
        if (encoder == null) {
            encoder = new DataOutputStream(this);
            textEncoder = encoder;
        }
        encoder.writeUTF(value);
    }
}
