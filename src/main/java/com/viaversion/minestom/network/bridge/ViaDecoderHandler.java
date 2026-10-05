package com.viaversion.minestom.network.bridge;

import com.viaversion.minestom.transport.buffer.ByteBuf;
import com.viaversion.minestom.transport.channel.ChannelHandlerContext;
import com.viaversion.minestom.transport.channel.ChannelInboundHandlerAdapter;

final class ViaDecoderHandler extends ChannelInboundHandlerAdapter {
    private final ViaTranslator translator;

    ViaDecoderHandler(final ViaTranslator translator) {
        this.translator = translator;
    }

    @Override
    public void channelRead(final ChannelHandlerContext context, final Object message) {
        if (!(message instanceof ByteBuf packet)) {
            context.fireChannelRead(message);
            return;
        }
        boolean forwarded = false;
        try {
            if (translator.serverbound(packet)) {
                forwarded = true;
                context.fireChannelRead(packet);
            }
        } finally {
            if (!forwarded) {
                packet.release();
            }
        }
    }
}
