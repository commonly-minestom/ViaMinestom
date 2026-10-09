package com.viaversion.minestom.network;

import com.viaversion.minestom.network.connection.ConnectionRegistry;
import com.viaversion.minestom.network.connection.IdleConnectionMonitor;
import com.viaversion.minestom.network.connection.ViaPlayerConnection;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import org.jetbrains.annotations.Nullable;

public final class NetworkServer {
    private static final Duration ACCEPT_FAILURE_BACKOFF = Duration.ofSeconds(1);

    private final NetworkSettings settings;
    private final ConnectionRegistry registry = new ConnectionRegistry();
    private final IdleConnectionMonitor monitor;
    private @Nullable ServerSocketChannel serverSocket;

    public NetworkServer(final NetworkSettings settings) {
        this.settings = settings;
        this.monitor = new IdleConnectionMonitor(registry, settings.readTimeout(), settings.closeTimeout());
    }

    public synchronized void bind(final InetSocketAddress address) {
        if (serverSocket != null) {
            throw new IllegalStateException("Already bound to " + address());
        }
        final ServerSocketChannel socket;
        try {
            socket = ServerSocketChannel.open();
            socket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            socket.bind(address, settings.backlog());
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to bind " + address, e);
        }
        this.serverSocket = socket;
        monitor.start();
        Thread.ofVirtual().name("Via-Acceptor").start(() -> accept(socket));
    }

    public synchronized @Nullable InetSocketAddress address() {
        final ServerSocketChannel socket = serverSocket;
        if (socket == null) {
            return null;
        }
        try {
            return (InetSocketAddress) socket.getLocalAddress();
        } catch (final IOException _) {
            return null;
        }
    }

    public int connectionCount() {
        return registry.size();
    }

    public synchronized void close() {
        final ServerSocketChannel socket = serverSocket;
        if (socket == null) {
            return;
        }
        serverSocket = null;
        closeQuietly(socket);
        monitor.stop();
        registry.closeAll(settings.closeTimeout());
    }

    private void accept(final ServerSocketChannel socket) {
        while (socket.isOpen()) {
            final SocketChannel client;
            try {
                client = socket.accept();
            } catch (final ClosedChannelException _) {
                return;
            } catch (final IOException e) {
                MinecraftServer.getExceptionManager().handleException(e);
                pause();
                continue;
            }
            open(client);
        }
    }

    private void open(final SocketChannel client) {
        ViaPlayerConnection connection = null;
        try {
            configure(client);
            connection = ViaPlayerConnection.open(client, settings, registry::remove);
            registry.add(connection);
            connection.start();
        } catch (final IOException | RuntimeException e) {
            if (connection != null) {
                registry.remove(connection);
                connection.disconnect();
            }
            closeQuietly(client);
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    private static void configure(final SocketChannel client) throws IOException {
        client.setOption(StandardSocketOptions.TCP_NODELAY, ServerFlag.SOCKET_NO_DELAY);
        client.setOption(StandardSocketOptions.SO_SNDBUF, ServerFlag.SOCKET_SEND_BUFFER_SIZE);
        client.setOption(StandardSocketOptions.SO_RCVBUF, ServerFlag.SOCKET_RECEIVE_BUFFER_SIZE);
    }

    private static void pause() {
        try {
            Thread.sleep(ACCEPT_FAILURE_BACKOFF);
        } catch (final InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    private static void closeQuietly(final Channel channel) {
        try {
            channel.close();
        } catch (final IOException _) {
        }
    }
}
