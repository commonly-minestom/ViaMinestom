package com.viaversion.minestom.transport.channel;

public class ChannelOutboundHandlerAdapter extends ChannelHandlerAdapter implements ChannelOutboundHandler {

    @Override
    public void write(final ChannelHandlerContext context, final Object message, final ChannelPromise promise) throws Exception {
        context.write(message, promise);
    }
}
