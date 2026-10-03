package com.viaversion.minestom.network.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import net.minestom.server.network.NetworkBuffer;

/**
 * An outgoing packet body together with the frame Minestom has prepared for it ahead of time.
 * <p>
 * It is only written to connections that need no translation. Unless an interceptor changes the packet,
 * the frame is sent in place of the body, which saves compressing it again.
 */
public final class PreframedPacket extends DefaultByteBufHolder {
    private final NetworkBuffer frames;
    private final long index;
    private final long length;

    public PreframedPacket(final ByteBuf body, final NetworkBuffer frames, final long index, final long length) {
        super(body);
        this.frames = frames;
        this.index = index;
        this.length = length;
    }

    public NetworkBuffer frames() {
        return frames;
    }

    public long index() {
        return index;
    }

    public long length() {
        return length;
    }
}
