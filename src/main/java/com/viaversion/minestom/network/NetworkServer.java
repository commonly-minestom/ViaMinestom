package com.viaversion.minestom.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.net.InetSocketAddress;
import net.minestom.server.ServerFlag;
import org.jetbrains.annotations.Nullable;

/**
 * Accepts the client connections in place of the socket server built into Minestom.
 */
public final class NetworkServer {
    private final Transport transport;

    private @Nullable EventLoopGroup acceptorGroup;
    private @Nullable EventLoopGroup workerGroup;
    private @Nullable Channel serverChannel;

    public NetworkServer(final Transport transport) {
        this.transport = transport;
    }

    public synchronized void bind(final InetSocketAddress address) {
        if (serverChannel != null) {
            throw new IllegalStateException("Already bound to " + serverChannel.localAddress());
        }

        final EventLoopGroup acceptorGroup = new MultiThreadIoEventLoopGroup(1, new DefaultThreadFactory("Via-Acceptor", true), transport.newIoHandlerFactory());
        final EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(new DefaultThreadFactory("Via-Worker", true), transport.newIoHandlerFactory());
        this.acceptorGroup = acceptorGroup;
        this.workerGroup = workerGroup;
        try {
            this.serverChannel = new ServerBootstrap()
                .group(acceptorGroup, workerGroup)
                .channel(transport.serverChannelType())
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, ServerFlag.SOCKET_NO_DELAY)
                .childOption(ChannelOption.SO_SNDBUF, ServerFlag.SOCKET_SEND_BUFFER_SIZE)
                .childOption(ChannelOption.SO_RCVBUF, ServerFlag.SOCKET_RECEIVE_BUFFER_SIZE)
                .childHandler(new ClientChannelInitializer())
                .bind(address)
                .syncUninterruptibly()
                .channel();
        } catch (final RuntimeException e) {
            close();
            throw e;
        }
    }

    public synchronized @Nullable InetSocketAddress address() {
        return serverChannel != null ? (InetSocketAddress) serverChannel.localAddress() : null;
    }

    public synchronized void close() {
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
            serverChannel = null;
        }
        if (acceptorGroup != null) {
            acceptorGroup.shutdownGracefully();
            acceptorGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
    }
}
