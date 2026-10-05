package com.viaversion.minestom.transport.handler.codec;

import com.viaversion.minestom.transport.channel.ChannelHandlerContext;
import com.viaversion.minestom.transport.channel.ChannelOutboundHandlerAdapter;
import com.viaversion.minestom.transport.channel.ChannelPromise;
import com.viaversion.minestom.transport.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class MessageToMessageEncoder<I> extends ChannelOutboundHandlerAdapter {
    private final Class<?> messageType;

    protected MessageToMessageEncoder() {
        this.messageType = TypeArgumentResolver.resolve(getClass(), MessageToMessageEncoder.class, 0);
    }

    protected MessageToMessageEncoder(final Class<? extends I> messageType) {
        this.messageType = Objects.requireNonNull(messageType, "messageType");
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

        final List<Object> output = new ArrayList<>();
        try {
            encode(context, cast(message), output);
            if (output.isEmpty()) {
                throw new EncoderException(getClass().getSimpleName() + " must produce at least one message");
            }
        } catch (final Exception e) {
            for (final Object encoded : output) {
                ReferenceCountUtil.release(encoded);
            }
            throw e instanceof EncoderException ? e : new EncoderException(e);
        } finally {
            ReferenceCountUtil.release(message);
        }

        final int last = output.size() - 1;
        for (int i = 0; i < last; i++) {
            context.write(output.get(i), context.newPromise()).addListener(written -> {
                if (!written.isSuccess()) {
                    promise.tryFailure(written.cause());
                }
            });
        }
        context.write(output.get(last), promise);
    }

    protected abstract void encode(ChannelHandlerContext context, I message, List<Object> output) throws Exception;

    @SuppressWarnings("unchecked")
    private I cast(final Object message) {
        return (I) message;
    }
}
