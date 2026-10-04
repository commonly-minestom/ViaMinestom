package com.viaversion.minestom.network.bridge;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

final class ViaEncoderHandler extends ChannelOutboundHandlerAdapter {
    private final ViaTranslator translator;

    ViaEncoderHandler(final ViaTranslator translator) {
        this.translator = translator;
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object message, final ChannelPromise promise) {
        if (!(message instanceof ByteBuf packet)) {
            ctx.write(message, promise);
            return;
        }
        final boolean forward;
        try {
            forward = translator.clientbound(packet);
        } catch (final RuntimeException e) {
            packet.release();
            promise.tryFailure(e);
            return;
        }
        if (forward) {
            ctx.write(packet, promise);
        } else {
            packet.release();
            promise.trySuccess();
        }
    }
}
