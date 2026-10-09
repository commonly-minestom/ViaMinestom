package com.viaversion.minestom.network.connection;

import com.viaversion.minestom.network.codec.CorruptedFrameException;
import com.viaversion.minestom.network.codec.FrameSplitter;
import com.viaversion.minestom.network.codec.ProxyProtocol;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import javax.crypto.Cipher;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.PacketReading;
import net.minestom.server.registry.Registries;
import org.jetbrains.annotations.Nullable;

final class SocketReader implements Runnable {
    private final SocketChannel socket;
    private final NetworkBuffer buffer;
    private volatile @Nullable Cipher decrypt;
    private boolean proxyHeaderPending = ServerFlag.PROXY_PROTOCOL;
    private ViaPlayerConnection connection;

    SocketReader(final SocketChannel socket, final Registries registries) {
        this.socket = socket;
        this.buffer = NetworkBuffer.staticBuffer(ServerFlag.POOLED_BUFFER_SIZE, registries);
    }

    void attach(final ViaPlayerConnection connection) {
        this.connection = connection;
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
        final long required = FrameSplitter.split(buffer, maxLength, this::frame);
        buffer.compact();
        if (required > buffer.capacity()) {
            buffer.resize(required);
        }
    }

    private void frame(final NetworkBuffer source, final long index, final int length) {
        final byte[] bytes = new byte[length];
        source.copyTo(index, bytes, 0, length);
        connection.frame(new InboundFrame(bytes));
    }
}
