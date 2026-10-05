package com.viaversion.minestom.transport.channel;

public interface ChannelInboundHandler extends ChannelHandler {

    void channelRead(ChannelHandlerContext context, Object message) throws Exception;
}
