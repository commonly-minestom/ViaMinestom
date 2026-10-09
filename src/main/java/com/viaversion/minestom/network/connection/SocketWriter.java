package com.viaversion.minestom.network.connection;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicLong;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.NetworkBuffer;

final class SocketWriter implements Runnable {
    private static final int SMALL_WIRE_BYTES = 4096;

    private final SocketChannel socket;
    private final BufferPool pool;
    private final long maxPendingBytes;
    private final Runnable closeRequest;
    private final Runnable onStopped;
    private final AtomicLong pendingBytes = new AtomicLong();
    private final Mailbox<NetworkBuffer> outbox;

    SocketWriter(final SocketChannel socket, final BufferPool pool, final long maxPendingBytes, final Runnable closeRequest, final Runnable onStopped) {
        this.socket = socket;
        this.pool = pool;
        this.maxPendingBytes = maxPendingBytes;
        this.closeRequest = closeRequest;
        this.onStopped = onStopped;
        this.outbox = new Mailbox<>(pool::release);
    }

    void send(final NetworkBuffer wire) {
        if (pendingBytes.addAndGet(wire.readableBytes()) > maxPendingBytes) {
            pool.release(wire);
            closeRequest.run();
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
                    transmit(wire);
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
            closeRequest.run();
            onStopped.run();
        }
    }

    private void transmit(final NetworkBuffer head) throws IOException {
        final NetworkBuffer first = isSmall(head) ? outbox.poll() : null;
        if (first == null) {
            writeAndRelease(head);
            return;
        }
        final NetworkBuffer staging = pool.acquire();
        NetworkBuffer held = first;
        try {
            stage(staging, head);
            while (held != null) {
                final NetworkBuffer next = held;
                if (!isSmall(next) || next.readableBytes() > staging.writableBytes()) {
                    held = null;
                    try {
                        writeFully(staging);
                    } catch (final IOException | RuntimeException e) {
                        pool.release(next);
                        throw e;
                    }
                    writeAndRelease(next);
                    return;
                }
                held = null;
                stage(staging, next);
                held = outbox.poll();
            }
            writeFully(staging);
        } finally {
            if (held != null) {
                pool.release(held);
            }
            pool.release(staging);
        }
    }

    private static boolean isSmall(final NetworkBuffer wire) {
        return wire.readableBytes() <= SMALL_WIRE_BYTES;
    }

    private void stage(final NetworkBuffer staging, final NetworkBuffer wire) {
        final long size = wire.readableBytes();
        try {
            NetworkBuffer.copy(wire, wire.readIndex(), staging, staging.writeIndex(), size);
            staging.advanceWrite(size);
        } finally {
            pendingBytes.addAndGet(-size);
            pool.release(wire);
        }
    }

    private void writeAndRelease(final NetworkBuffer wire) throws IOException {
        final long size = wire.readableBytes();
        try {
            writeFully(wire);
        } finally {
            pendingBytes.addAndGet(-size);
            pool.release(wire);
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
