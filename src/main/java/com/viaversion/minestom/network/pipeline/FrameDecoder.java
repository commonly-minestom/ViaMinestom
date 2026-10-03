package com.viaversion.minestom.network.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;
import net.minestom.server.network.packet.PacketReading;
import net.minestom.server.network.player.PlayerConnection;

/**
 * Splits the inbound byte stream into length-prefixed packets.
 */
public final class FrameDecoder extends ByteToMessageDecoder {
    private static final int MAX_LENGTH_BYTES = 3;

    private final PlayerConnection connection;

    public FrameDecoder(final PlayerConnection connection) {
        this.connection = connection;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
        if (!ctx.channel().isActive()) {
            in.skipBytes(in.readableBytes());
            return;
        }

        final int start = in.readerIndex();
        int length = 0;
        for (int i = 0; i < MAX_LENGTH_BYTES; i++) {
            if (!in.isReadable()) {
                in.readerIndex(start);
                return;
            }

            final byte part = in.readByte();
            length |= (part & 0x7F) << (i * 7);
            if (part < 0) {
                continue;
            }

            if (length > PacketReading.maxPacketSize(connection.getClientState())) {
                throw new CorruptedFrameException("Packet too large: " + length);
            }
            if (in.readableBytes() < length) {
                in.readerIndex(start);
                return;
            }
            if (length != 0) {
                out.add(in.readRetainedSlice(length));
            }
            return;
        }
        throw new CorruptedFrameException("Packet length wider than 21 bits");
    }
}
