package com.viaversion.minestom.transport.channel;

public class ChannelInboundHandlerAdapter extends ChannelHandlerAdapter implements ChannelInboundHandler {

    @Override
    public void channelRead(final ChannelHandlerContext context, final Object message) throws Exception {
        context.fireChannelRead(message);
    }
}
