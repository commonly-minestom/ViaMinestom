package com.viaversion.minestom.network.pipeline;

import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Prefixes outbound packets with their length.
 */
@ChannelHandler.Sharable
public final class FrameEncoder extends MessageToByteEncoder<ByteBuf> {
    public static final FrameEncoder INSTANCE = new FrameEncoder();
    private static final int MAX_LENGTH_BYTES = 3;

    private FrameEncoder() {
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final ByteBuf packet, final ByteBuf out) {
        Types.VAR_INT.writePrimitive(out, packet.readableBytes());
        out.writeBytes(packet);
    }

    @Override
    protected ByteBuf allocateBuffer(final ChannelHandlerContext ctx, final ByteBuf packet, final boolean preferDirect) {
        return ctx.alloc().ioBuffer(packet.readableBytes() + MAX_LENGTH_BYTES);
    }
}
