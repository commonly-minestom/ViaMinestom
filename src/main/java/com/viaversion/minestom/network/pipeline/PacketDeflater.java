package com.viaversion.minestom.network.pipeline;

import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.zip.Deflater;

/**
 * Writes outbound packets in the compressed protocol format, deflating those above the threshold.
 */
public final class PacketDeflater extends MessageToByteEncoder<ByteBuf> {
    private static final int CHUNK_SIZE = 8192;

    private final Deflater deflater = new Deflater();
    private final int threshold;

    public PacketDeflater(final int threshold) {
        this.threshold = threshold;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final ByteBuf packet, final ByteBuf out) {
        final int size = packet.readableBytes();
        if (size < threshold) {
            Types.VAR_INT.writePrimitive(out, 0);
            out.writeBytes(packet);
            return;
        }

        Types.VAR_INT.writePrimitive(out, size);
        deflater.setInput(packet.nioBuffer());
        deflater.finish();
        while (!deflater.finished()) {
            out.ensureWritable(CHUNK_SIZE);
            final int written = deflater.deflate(out.nioBuffer(out.writerIndex(), out.writableBytes()));
            out.writerIndex(out.writerIndex() + written);
        }
        deflater.reset();
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
        deflater.end();
        super.handlerRemoved(ctx);
    }
}
