package com.viaversion.minestom.network.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

/**
 * Applies the stream cipher negotiated during an authenticated login to everything on the wire.
 */
public final class CipherHandler extends ChannelDuplexHandler {
    private final Cipher decrypt;
    private final Cipher encrypt;

    public CipherHandler(final Cipher decrypt, final Cipher encrypt) {
        this.decrypt = decrypt;
        this.encrypt = encrypt;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object message) throws ShortBufferException {
        if (!(message instanceof ByteBuf data)) {
            ctx.fireChannelRead(message);
            return;
        }

        try {
            ctx.fireChannelRead(process(decrypt, ctx.alloc(), data));
        } finally {
            data.release();
        }
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object message, final ChannelPromise promise) throws ShortBufferException {
        if (!(message instanceof ByteBuf data)) {
            ctx.write(message, promise);
            return;
        }

        try {
            ctx.write(process(encrypt, ctx.alloc(), data), promise);
        } finally {
            data.release();
        }
    }

    private static ByteBuf process(final Cipher cipher, final ByteBufAllocator allocator, final ByteBuf data) throws ShortBufferException {
        final int length = data.readableBytes();
        final ByteBuf result = allocator.ioBuffer(length);
        try {
            final int produced = cipher.update(data.nioBuffer(), result.nioBuffer(0, length));
            return result.writerIndex(produced);
        } catch (final ShortBufferException | RuntimeException e) {
            result.release();
            throw e;
        }
    }
}
