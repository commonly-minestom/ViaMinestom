package com.viaversion.minestom.network.connection;

import com.viaversion.minestom.network.codec.WireCodec;
import java.util.function.Consumer;
import net.minestom.server.ServerFlag;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;

final class WireBatch {
    private static final int FRAME_HEADER_BYTES = 8;
    private static final long MAX_CAPACITY = WireCodec.deflateBound(ServerFlag.MAX_PACKET_SIZE) + (long) FRAME_HEADER_BYTES;

    @FunctionalInterface
    interface FrameWriter {

        boolean write(NetworkBuffer out);
    }

    private final BufferPool pool;
    private final Consumer<NetworkBuffer> downstream;
    private @Nullable NetworkBuffer current;
    private boolean closed;

    WireBatch(final BufferPool pool, final Consumer<NetworkBuffer> downstream) {
        this.pool = pool;
        this.downstream = downstream;
    }

    void write(final FrameWriter writer) {
        if (closed) {
            return;
        }
        while (true) {
            final NetworkBuffer out = current();
            final long start = out.writeIndex();
            boolean written = false;
            try {
                written = writer.write(out);
            } catch (final IndexOutOfBoundsException _) {
            } finally {
                if (!written) {
                    out.writeIndex(start);
                }
            }
            if (written) {
                return;
            }
            if (start > 0) {
                flush();
            } else {
                grow(out);
            }
        }
    }

    void flush() {
        final NetworkBuffer out = current;
        if (out != null && out.readableBytes() > 0) {
            current = null;
            downstream.accept(out);
        }
    }

    void close() {
        closed = true;
        flush();
        final NetworkBuffer out = current;
        if (out != null) {
            current = null;
            pool.release(out);
        }
    }

    private NetworkBuffer current() {
        NetworkBuffer out = current;
        if (out == null) {
            out = pool.acquire();
            current = out;
        }
        return out;
    }

    private static void grow(final NetworkBuffer out) {
        final long capacity = out.capacity();
        if (capacity >= MAX_CAPACITY) {
            throw new IllegalStateException("Frame exceeds " + MAX_CAPACITY + " bytes");
        }
        out.resize(Math.min(capacity * 2, MAX_CAPACITY));
    }
}
