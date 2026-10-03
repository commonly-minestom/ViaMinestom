package com.viaversion.minestom.network.connection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.lang.foreign.MemorySegment;
import java.util.zip.DataFormatException;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.PacketVanilla;
import net.minestom.server.network.packet.PacketWriting;
import net.minestom.server.network.packet.server.ServerPacket;

/**
 * Converts between the packet buffers of Minestom and the ones flowing through the Netty pipeline.
 */
public final class PacketSerializer {
    private static final int LENGTH_PREFIX_SIZE = 3;

    private PacketSerializer() {
    }

    /**
     * Writes the id and the payload of a packet, leaving framing and compression to the pipeline.
     */
    static ByteBuf serialize(final ByteBufAllocator allocator, final ConnectionState state, final ServerPacket packet) {
        final NetworkBuffer scratch = PacketVanilla.PACKET_POOL.get();
        try {
            writeUncompressed(scratch, state, packet);
            return copy(allocator, scratch, LENGTH_PREFIX_SIZE, scratch.writeIndex() - LENGTH_PREFIX_SIZE);
        } finally {
            PacketVanilla.PACKET_POOL.add(scratch);
        }
    }

    public static ByteBuf copy(final ByteBufAllocator allocator, final NetworkBuffer source, final long index, final long length) {
        final int size = Math.toIntExact(length);
        final ByteBuf target = allocator.buffer(size);
        source.copyTo(index, MemorySegment.ofBuffer(target.nioBuffer(0, size)), 0, size);
        return target.writerIndex(size);
    }

    public static NetworkBuffer wrap(final ByteBuf source) {
        return NetworkBuffer.wrap(MemorySegment.ofBuffer(source.nioBuffer()), 0, source.readableBytes(), MinecraftServer.getRegistries());
    }

    public static ByteBuf copy(final ByteBufAllocator allocator, final NetworkBuffer source) {
        return copy(allocator, source, source.readIndex(), source.readableBytes());
    }

    /**
     * Unpacks a run of packets that were framed ahead of time back into their individual bodies.
     */
    static void unframe(final ByteBufAllocator allocator, final NetworkBuffer source, final long index, final long length,
                        final boolean compressed, final FrameConsumer output) {
        final NetworkBuffer frames = source.slice(index, length, 0, length);
        while (frames.readableBytes() > 0) {
            final long frameStart = frames.readIndex();
            final int frameLength = frames.read(NetworkBuffer.VAR_INT);
            final long frameEnd = frames.readIndex() + frameLength;
            final int inflatedSize = compressed ? frames.read(NetworkBuffer.VAR_INT) : 0;
            final long bodyIndex = frames.readIndex();
            final long bodyLength = frameEnd - bodyIndex;
            final ByteBuf body = inflatedSize == 0
                ? copy(allocator, frames, bodyIndex, bodyLength)
                : inflate(allocator, frames, bodyIndex, bodyLength, inflatedSize);
            frames.readIndex(frameEnd);
            output.accept(body, index + frameStart, frameEnd - frameStart);
        }
    }

    @FunctionalInterface
    interface FrameConsumer {

        /**
         * Receives the body of a packet along with the position of its frame in the buffer it was read from.
         */
        void accept(ByteBuf body, long frameIndex, long frameLength);
    }

    private static void writeUncompressed(final NetworkBuffer buffer, final ConnectionState state, final ServerPacket packet) {
        while (true) {
            try {
                PacketWriting.writeFramedPacket(buffer, state, packet, 0);
                return;
            } catch (final IndexOutOfBoundsException e) {
                final long capacity = buffer.capacity();
                if (capacity > ServerFlag.MAX_PACKET_SIZE) {
                    throw new IllegalStateException("Packet too large: " + packet.getClass().getSimpleName(), e);
                }
                buffer.resize(capacity * 2);
                buffer.writeIndex(0);
            }
        }
    }

    private static ByteBuf inflate(final ByteBufAllocator allocator, final NetworkBuffer source, final long index, final long length, final int size) {
        final NetworkBuffer scratch = PacketVanilla.PACKET_POOL.get();
        try {
            if (scratch.capacity() < size) {
                scratch.resize(size);
            }

            final long written = source.decompress(index, length, scratch.slice(0, size, 0, 0));
            if (written != size) {
                throw new IllegalStateException("Decompressed length mismatch: expected " + size + ", got " + written);
            }
            return copy(allocator, scratch, 0, size);
        } catch (final DataFormatException e) {
            throw new IllegalStateException("Malformed pre-framed packet", e);
        } finally {
            PacketVanilla.PACKET_POOL.add(scratch);
        }
    }
}
