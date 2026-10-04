package com.viaversion.minestom.network.bridge;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

final class ViaDecoderHandler extends ChannelInboundHandlerAdapter {
    private final ViaTranslator translator;

    ViaDecoderHandler(final ViaTranslator translator) {
        this.translator = translator;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object message) {
        if (!(message instanceof ByteBuf packet)) {
            ctx.fireChannelRead(message);
            return;
        }
        boolean forwarded = false;
        try {
            if (translator.serverbound(packet)) {
                forwarded = true;
                ctx.fireChannelRead(packet);
            }
        } finally {
            if (!forwarded) {
                packet.release();
            }
        }
    }
}
