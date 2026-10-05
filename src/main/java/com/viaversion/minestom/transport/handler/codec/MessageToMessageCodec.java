package com.viaversion.minestom.transport.handler.codec;

import com.viaversion.minestom.transport.channel.ChannelDuplexHandler;
import com.viaversion.minestom.transport.channel.ChannelHandlerContext;
import com.viaversion.minestom.transport.channel.ChannelPromise;
import java.util.List;

public abstract class MessageToMessageCodec<I, O> extends ChannelDuplexHandler {
    private final Class<?> inboundType;
    private final Class<?> outboundType;
    private final MessageToMessageDecoder<Object> decoder = new MessageToMessageDecoder<>(Object.class) {

        @Override
        public boolean acceptInboundMessage(final Object message) {
            return MessageToMessageCodec.this.acceptInboundMessage(message);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void decode(final ChannelHandlerContext context, final Object message, final List<Object> output) throws Exception {
            MessageToMessageCodec.this.decode(context, (I) message, output);
        }
    };
    private final MessageToMessageEncoder<Object> encoder = new MessageToMessageEncoder<>(Object.class) {

        @Override
        public boolean acceptOutboundMessage(final Object message) {
            return MessageToMessageCodec.this.acceptOutboundMessage(message);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void encode(final ChannelHandlerContext context, final Object message, final List<Object> output) throws Exception {
            MessageToMessageCodec.this.encode(context, (O) message, output);
        }
    };

    protected MessageToMessageCodec() {
        this.inboundType = TypeArgumentResolver.resolve(getClass(), MessageToMessageCodec.class, 0);
        this.outboundType = TypeArgumentResolver.resolve(getClass(), MessageToMessageCodec.class, 1);
    }

    public boolean acceptInboundMessage(final Object message) {
        return inboundType.isInstance(message);
    }

    public boolean acceptOutboundMessage(final Object message) {
        return outboundType.isInstance(message);
    }

    @Override
    public void channelRead(final ChannelHandlerContext context, final Object message) throws Exception {
        decoder.channelRead(context, message);
    }

    @Override
    public void write(final ChannelHandlerContext context, final Object message, final ChannelPromise promise) throws Exception {
        encoder.write(context, message, promise);
    }

    protected abstract void decode(ChannelHandlerContext context, I message, List<Object> output) throws Exception;

    protected abstract void encode(ChannelHandlerContext context, O message, List<Object> output) throws Exception;
}
