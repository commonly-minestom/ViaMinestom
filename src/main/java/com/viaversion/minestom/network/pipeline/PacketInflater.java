package com.viaversion.minestom.network.pipeline;

import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import net.minestom.server.ServerFlag;

/**
 * Reads inbound packets in the compressed protocol format.
 */
public final class PacketInflater extends MessageToMessageDecoder<ByteBuf> {
    private final Inflater inflater = new Inflater();

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws DataFormatException {
        final int size = Types.VAR_INT.readPrimitive(in);
        if (size == 0) {
            out.add(in.retainedSlice());
            return;
        }
        if (size < 0 || size > ServerFlag.MAX_PACKET_SIZE) {
            throw new DecoderException("Invalid decompressed length: " + size);
        }

        final ByteBuf packet = ctx.alloc().buffer(size, size);
        try {
            inflater.setInput(in.nioBuffer());
            final int produced = inflater.inflate(packet.nioBuffer(0, size));
            if (produced != size) {
                throw new DecoderException("Decompressed length mismatch: expected " + size + ", got " + produced);
            }
            out.add(packet.writerIndex(size).retain());
        } finally {
            packet.release();
            inflater.reset();
        }
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
        inflater.end();
        super.handlerRemoved(ctx);
    }
}
