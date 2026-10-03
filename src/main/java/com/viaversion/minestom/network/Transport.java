package com.viaversion.minestom.network;

import io.netty.channel.IoHandlerFactory;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.util.function.Supplier;

/**
 * The socket implementations the server can run on, native ones being preferred where available.
 */
public enum Transport {
    EPOLL(EpollServerSocketChannel.class, EpollIoHandler::newFactory),
    KQUEUE(KQueueServerSocketChannel.class, KQueueIoHandler::newFactory),
    NIO(NioServerSocketChannel.class, NioIoHandler::newFactory);

    private final Class<? extends ServerChannel> serverChannelType;
    private final Supplier<IoHandlerFactory> ioHandlerFactory;

    Transport(final Class<? extends ServerChannel> serverChannelType, final Supplier<IoHandlerFactory> ioHandlerFactory) {
        this.serverChannelType = serverChannelType;
        this.ioHandlerFactory = ioHandlerFactory;
    }

    public static Transport detect() {
        if (Epoll.isAvailable()) {
            return EPOLL;
        }
        if (KQueue.isAvailable()) {
            return KQUEUE;
        }
        return NIO;
    }

    public Class<? extends ServerChannel> serverChannelType() {
        return serverChannelType;
    }

    public IoHandlerFactory newIoHandlerFactory() {
        return ioHandlerFactory.get();
    }
}
