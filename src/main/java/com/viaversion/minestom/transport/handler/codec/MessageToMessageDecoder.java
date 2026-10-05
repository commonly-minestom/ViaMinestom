package com.viaversion.minestom.transport.handler.codec;

import com.viaversion.minestom.transport.channel.ChannelHandlerContext;
import com.viaversion.minestom.transport.channel.ChannelInboundHandlerAdapter;
import com.viaversion.minestom.transport.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class MessageToMessageDecoder<I> extends ChannelInboundHandlerAdapter {
    private final Class<?> messageType;

    protected MessageToMessageDecoder() {
        this.messageType = TypeArgumentResolver.resolve(getClass(), MessageToMessageDecoder.class, 0);
    }

    protected MessageToMessageDecoder(final Class<? extends I> messageType) {
        this.messageType = Objects.requireNonNull(messageType, "messageType");
    }

    public boolean acceptInboundMessage(final Object message) {
        return messageType.isInstance(message);
    }

    @Override
    public void channelRead(final ChannelHandlerContext context, final Object message) throws Exception {
        if (!acceptInboundMessage(message)) {
            context.fireChannelRead(message);
            return;
        }

        final List<Object> output = new ArrayList<>();
        try {
            decode(context, cast(message), output);
        } catch (final DecoderException e) {
            throw e;
        } catch (final Exception e) {
            throw new DecoderException(e);
        } finally {
            ReferenceCountUtil.release(message);
            for (final Object decoded : output) {
                context.fireChannelRead(decoded);
            }
        }
    }

    protected abstract void decode(ChannelHandlerContext context, I message, List<Object> output) throws Exception;

    @SuppressWarnings("unchecked")
    private I cast(final Object message) {
        return (I) message;
    }
}
