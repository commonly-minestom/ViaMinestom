package com.viaversion.minestom.transport.channel;

public interface ChannelOutboundHandler extends ChannelHandler {

    void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception;
}
