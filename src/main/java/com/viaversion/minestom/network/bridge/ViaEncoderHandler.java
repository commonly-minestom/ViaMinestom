package com.viaversion.minestom.network.bridge;

import com.viaversion.minestom.transport.buffer.ByteBuf;
import com.viaversion.minestom.transport.channel.ChannelHandlerContext;
import com.viaversion.minestom.transport.channel.ChannelOutboundHandlerAdapter;
import com.viaversion.minestom.transport.channel.ChannelPromise;

final class ViaEncoderHandler extends ChannelOutboundHandlerAdapter {
    private final ViaTranslator translator;

    ViaEncoderHandler(final ViaTranslator translator) {
        this.translator = translator;
    }

    @Override
    public void write(final ChannelHandlerContext context, final Object message, final ChannelPromise promise) {
        if (!(message instanceof ByteBuf packet)) {
            context.write(message, promise);
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
            context.write(packet, promise);
        } else {
            packet.release();
            promise.trySuccess();
        }
    }
}
