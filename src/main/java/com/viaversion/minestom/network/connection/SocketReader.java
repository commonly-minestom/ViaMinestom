package com.viaversion.minestom.network.connection;

import com.viaversion.minestom.network.codec.CorruptedFrameException;
import com.viaversion.minestom.network.codec.FrameSplitter;
import com.viaversion.minestom.network.codec.ProxyProtocol;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.PacketReading;
import net.minestom.server.registry.Registries;
import org.jetbrains.annotations.Nullable;

final class SocketReader implements Runnable {
    private final SocketChannel socket;
    private static final long CREDIT_POLL_MILLIS = 100;
    private static final FrameSplitter.FrameSink MEASURE = (_, _, _) -> { };

    private final Registries registries;
    private final Semaphore credit;
    private final int creditLimit;
    private NetworkBuffer buffer;
    private volatile @Nullable Cipher decrypt;
    private boolean proxyHeaderPending = ServerFlag.PROXY_PROTOCOL;
    private final ViaPlayerConnection connection;

    SocketReader(final ViaPlayerConnection connection, final SocketChannel socket, final Registries registries, final long maxPendingBytes) {
        this.connection = connection;
        this.socket = socket;
        this.registries = registries;
        this.creditLimit = (int) Math.min(maxPendingBytes, Integer.MAX_VALUE);
        this.credit = new Semaphore(creditLimit);
        this.buffer = NetworkBuffer.staticBuffer(ServerFlag.POOLED_BUFFER_SIZE, registries);
    }

    void decryptWith(final Cipher cipher) {
        this.decrypt = cipher;
    }

    @Override
    public void run() {
        try {
            while (true) {
                read();
            }
        } catch (final IOException e) {
            if (!PeerDisconnects.isExpected(e)) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        } catch (final Throwable t) {
            MinecraftServer.getExceptionManager().handleException(t);
        } finally {
            connection.requestClose();
        }
    }

    private void read() throws IOException {
        final long start = buffer.writeIndex();
        final int count = buffer.readChannel(socket);
        connection.touch();
        if (proxyHeaderPending && !consumeProxyHeader()) {
            return;
        }
        final Cipher cipher = decrypt;
        if (cipher != null && count > 0) {
            buffer.cipher(cipher, start, count);
        }
        split();
    }

    private boolean consumeProxyHeader() throws IOException {
        final ProxyProtocol.Header header = ProxyProtocol.parse(buffer);
        switch (header.status()) {
            case NEED_MORE -> {
                if (buffer.writableBytes() == 0) {
                    throw new CorruptedFrameException("PROXY protocol header exceeds the read buffer");
                }
                return false;
            }
            case ABSENT -> {
                if (ServerFlag.PROXY_PROTOCOL_REQUIRED) {
                    throw new CorruptedFrameException("Missing required PROXY protocol header");
                }
            }
            case PRESENT -> {
                if (header.source() != null) {
                    connection.setRemoteAddress(header.source());
                }
            }
        }
        proxyHeaderPending = false;
        return true;
    }

    private void split() throws CorruptedFrameException {
        final int maxLength = PacketReading.maxPacketSize(connection.getClientState());
        final long start = buffer.readIndex();
        final long required = FrameSplitter.split(buffer, maxLength, MEASURE);
        final int complete = Math.toIntExact(buffer.readIndex() - start);
        if (complete > 0) {
            final byte[] chunk = new byte[complete];
            buffer.copyTo(start, chunk, 0, complete);
            FrameSplitter.split(NetworkBuffer.wrap(chunk, 0, complete, registries), maxLength, (_, index, length) -> frame(chunk, (int) index, length));
        }
        buffer.compact();
        if (required > buffer.capacity()) {
            buffer.resize(required);
        } else if (buffer.readableBytes() == 0 && buffer.capacity() > ServerFlag.POOLED_BUFFER_SIZE) {
            buffer = NetworkBuffer.staticBuffer(ServerFlag.POOLED_BUFFER_SIZE, registries);
        }
    }

    void release(final int frameLength) {
        credit.release(weight(frameLength));
    }

    private int weight(final int frameLength) {
        return Math.min(frameLength, creditLimit);
    }

    private boolean acquireCredit(final int frameLength) {
        final int weight = weight(frameLength);
        try {
            while (!credit.tryAcquire(weight, CREDIT_POLL_MILLIS, TimeUnit.MILLISECONDS)) {
                if (connection.isClosing()) {
                    return false;
                }
            }
            return true;
        } catch (final InterruptedException _) {
            Thread.currentThread().interrupt();
            connection.requestClose();
            return false;
        }
    }

    private void frame(final byte[] chunk, final int offset, final int length) {
        if (acquireCredit(length)) {
            connection.frame(new InboundFrame(chunk, offset, length));
        }
    }
}
