package com.viaversion.minestom.transport.channel;

import com.viaversion.minestom.transport.buffer.ByteBufAllocator;

public interface ChannelHandlerContext {

    Channel channel();

    ChannelPipeline pipeline();

    String name();

    ChannelHandler handler();

    ByteBufAllocator alloc();

    ChannelPromise newPromise();

    ChannelHandlerContext fireChannelRead(Object message);

    ChannelHandlerContext fireExceptionCaught(Throwable cause);

    ChannelFuture write(Object message, ChannelPromise promise);

    ChannelFuture writeAndFlush(Object message);
}
