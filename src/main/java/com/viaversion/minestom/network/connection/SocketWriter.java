package com.viaversion.minestom.network.connection;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicLong;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.NetworkBuffer;

final class SocketWriter implements Runnable {
    private final SocketChannel socket;
    private final BufferPool pool;
    private final long maxPendingBytes;
    private final AtomicLong pendingBytes = new AtomicLong();
    private final Mailbox<NetworkBuffer> outbox;
    private ViaPlayerConnection connection;

    SocketWriter(final SocketChannel socket, final BufferPool pool, final long maxPendingBytes) {
        this.socket = socket;
        this.pool = pool;
        this.maxPendingBytes = maxPendingBytes;
        this.outbox = new Mailbox<>(pool::release);
    }

    void attach(final ViaPlayerConnection connection) {
        this.connection = connection;
    }

    void send(final NetworkBuffer wire) {
        final long size = wire.readableBytes();
        if (pendingBytes.addAndGet(size) > maxPendingBytes) {
            pool.release(wire);
            connection.requestClose();
            return;
        }
        outbox.post(wire);
    }

    void shutdown() {
        outbox.close();
    }

    @Override
    public void run() {
        outbox.bind(Thread.currentThread());
        try {
            while (true) {
                final NetworkBuffer wire = outbox.poll();
                if (wire != null) {
                    final long size = wire.readableBytes();
                    try {
                        writeFully(wire);
                    } finally {
                        pendingBytes.addAndGet(-size);
                        pool.release(wire);
                    }
                    continue;
                }
                if (!outbox.await()) {
                    break;
                }
            }
        } catch (final IOException e) {
            if (!PeerDisconnects.isExpected(e)) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        } catch (final Throwable t) {
            MinecraftServer.getExceptionManager().handleException(t);
        } finally {
            outbox.terminate();
            closeSocket();
            connection.requestClose();
            connection.writerStopped();
        }
    }

    private void writeFully(final NetworkBuffer wire) throws IOException {
        while (!wire.writeChannel(socket)) {
        }
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (final IOException _) {
        }
    }
}
