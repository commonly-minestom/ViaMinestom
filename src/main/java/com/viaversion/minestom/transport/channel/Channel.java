package com.viaversion.minestom.transport.channel;

import com.viaversion.minestom.transport.buffer.ByteBufAllocator;
import java.net.SocketAddress;

public interface Channel {

    ByteBufAllocator alloc();

    EventLoop eventLoop();

    ChannelPipeline pipeline();

    boolean isOpen();

    SocketAddress remoteAddress();

    ChannelFuture close();

    ChannelFuture closeFuture();

    ChannelPromise newPromise();

    ChannelFuture newSucceededFuture();

    ChannelFuture newFailedFuture(Throwable cause);
}
