package com.viaversion.minestom.transport.handler.codec;

import com.viaversion.minestom.transport.buffer.ByteBuf;
import com.viaversion.minestom.transport.channel.ChannelHandlerContext;
import com.viaversion.minestom.transport.channel.ChannelOutboundHandlerAdapter;
import com.viaversion.minestom.transport.channel.ChannelPromise;
import com.viaversion.minestom.transport.util.ReferenceCountUtil;

public abstract class MessageToByteEncoder<I> extends ChannelOutboundHandlerAdapter {
    private final Class<?> messageType;

    protected MessageToByteEncoder() {
        this.messageType = TypeArgumentResolver.resolve(getClass(), MessageToByteEncoder.class, 0);
    }

    public boolean acceptOutboundMessage(final Object message) {
        return messageType.isInstance(message);
    }

    @Override
    public void write(final ChannelHandlerContext context, final Object message, final ChannelPromise promise) throws Exception {
        if (!acceptOutboundMessage(message)) {
            context.write(message, promise);
            return;
        }

        final ByteBuf output = context.alloc().buffer();
        try {
            encode(context, cast(message), output);
        } catch (final EncoderException e) {
            output.release();
            throw e;
        } catch (final Exception e) {
            output.release();
            throw new EncoderException(e);
        } finally {
            ReferenceCountUtil.release(message);
        }
        context.write(output, promise);
    }

    protected abstract void encode(ChannelHandlerContext context, I message, ByteBuf output) throws Exception;

    @SuppressWarnings("unchecked")
    private I cast(final Object message) {
        return (I) message;
    }
}
