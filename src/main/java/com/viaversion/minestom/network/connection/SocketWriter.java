package com.viaversion.minestom.network.connection;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.NetworkBuffer;

final class SocketWriter implements Runnable {
    private static final String BROKEN_PIPE = "Broken pipe";

    private final SocketChannel socket;
    private final BufferPool pool;
    private final Mailbox<NetworkBuffer> outbox;
    private ViaPlayerConnection connection;

    SocketWriter(final SocketChannel socket, final BufferPool pool) {
        this.socket = socket;
        this.pool = pool;
        this.outbox = new Mailbox<>(pool::release);
    }

    void attach(final ViaPlayerConnection connection) {
        this.connection = connection;
    }

    void send(final NetworkBuffer wire) {
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
                    try {
                        writeFully(wire);
                    } finally {
                        pool.release(wire);
                    }
                    continue;
                }
                if (!outbox.await()) {
                    break;
                }
            }
        } catch (final ClosedChannelException _) {
        } catch (final IOException e) {
            if (!BROKEN_PIPE.equals(e.getMessage())) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        } catch (final Throwable t) {
            MinecraftServer.getExceptionManager().handleException(t);
        } finally {
            outbox.terminate();
            closeSocket();
            connection.requestClose();
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
