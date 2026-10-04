package com.viaversion.minestom.network.codec;

import net.minestom.server.network.NetworkBuffer;

public final class FrameSplitter {
    private static final int MAX_LENGTH_BYTES = 3;
    private static final int INCOMPLETE = -1;

    private FrameSplitter() {
    }

    @FunctionalInterface
    public interface FrameSink {

        void accept(NetworkBuffer source, long index, int length);
    }

    public static long split(final NetworkBuffer buffer, final int maxLength, final FrameSink sink) throws CorruptedFrameException {
        while (buffer.readableBytes() > 0) {
            final long start = buffer.readIndex();
            final int length = readLength(buffer);
            if (length == INCOMPLETE) {
                buffer.readIndex(start);
                return 0;
            }
            if (length > maxLength) {
                throw new CorruptedFrameException("Packet too large: " + length + " > " + maxLength);
            }
            if (buffer.readableBytes() < length) {
                final long required = buffer.readIndex() - start + length;
                buffer.readIndex(start);
                return required;
            }
            if (length > 0) {
                sink.accept(buffer, buffer.readIndex(), length);
            }
            buffer.advanceRead(length);
        }
        return 0;
    }

    private static int readLength(final NetworkBuffer buffer) throws CorruptedFrameException {
        int length = 0;
        for (int i = 0; i < MAX_LENGTH_BYTES; i++) {
            if (buffer.readableBytes() == 0) {
                return INCOMPLETE;
            }
            final byte part = buffer.read(NetworkBuffer.BYTE);
            length |= (part & 0x7F) << (i * 7);
            if (part >= 0) {
                return length;
            }
        }
        throw new CorruptedFrameException("Packet length wider than 21 bits");
    }
}
