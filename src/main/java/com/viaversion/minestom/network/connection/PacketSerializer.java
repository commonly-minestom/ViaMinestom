package com.viaversion.minestom.network.connection;

import java.util.zip.DataFormatException;
import net.minestom.server.ServerFlag;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.PacketWriting;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.registry.Registries;
import org.jetbrains.annotations.Nullable;

final class PacketSerializer {
    private static final int LENGTH_PREFIX_BYTES = 3;
    private static final long MAX_CAPACITY = ServerFlag.MAX_PACKET_SIZE + (long) LENGTH_PREFIX_BYTES;

    private PacketSerializer() {
    }

    @FunctionalInterface
    interface FrameConsumer {

        void accept(NetworkBuffer body, long frameIndex, long frameLength);
    }

    static NetworkBuffer serialize(final NetworkBuffer scratch, final ConnectionState state, final ServerPacket packet) {
        while (true) {
            scratch.clear();
            try {
                PacketWriting.writeFramedPacket(scratch, state, packet, 0);
                final long length = scratch.writeIndex() - LENGTH_PREFIX_BYTES;
                return scratch.slice(LENGTH_PREFIX_BYTES, length, 0, length);
            } catch (final IndexOutOfBoundsException e) {
                grow(scratch, packet, e);
            }
        }
    }

    static void unframe(final NetworkBuffer source, final long index, final long length, final boolean compressed, final FrameConsumer output) throws DataFormatException {
        final NetworkBuffer frames = source.slice(index, length, 0, length);
        while (frames.readableBytes() > 0) {
            final long frameStart = frames.readIndex();
            final int frameLength = frames.read(NetworkBuffer.VAR_INT);
            final long frameEnd = frames.readIndex() + frameLength;
            final int inflatedSize = compressed ? frames.read(NetworkBuffer.VAR_INT) : 0;
            final long bodyIndex = frames.readIndex();
            final long bodyLength = frameEnd - bodyIndex;
            final NetworkBuffer body = inflatedSize == 0
                ? frames.slice(bodyIndex, bodyLength, 0, bodyLength)
                : inflate(frames, bodyIndex, bodyLength, inflatedSize);
            frames.readIndex(frameEnd);
            output.accept(body, index + frameStart, frameEnd - frameStart);
        }
    }

    static byte[] snapshot(final NetworkBuffer packet) {
        final int size = Math.toIntExact(packet.readableBytes());
        final byte[] bytes = new byte[size];
        packet.copyTo(packet.readIndex(), bytes, 0, size);
        return bytes;
    }

    static NetworkBuffer wrap(final byte[] bytes, final @Nullable Registries registries) {
        return NetworkBuffer.wrap(bytes, 0, bytes.length, registries);
    }

    private static NetworkBuffer inflate(final NetworkBuffer frames, final long index, final long length, final int size) throws DataFormatException {
        final NetworkBuffer body = NetworkBuffer.wrap(new byte[size], 0, 0, frames.registries());
        final long produced = frames.decompress(index, length, body);
        if (produced != size) {
            throw new DataFormatException("Decompressed length mismatch: expected " + size + ", got " + produced);
        }
        return body;
    }

    private static void grow(final NetworkBuffer scratch, final ServerPacket packet, final IndexOutOfBoundsException cause) {
        final long capacity = scratch.capacity();
        if (capacity >= MAX_CAPACITY) {
            throw new IllegalStateException("Packet too large: " + packet.getClass().getSimpleName(), cause);
        }
        scratch.resize(Math.min(capacity * 2, MAX_CAPACITY));
    }
}
