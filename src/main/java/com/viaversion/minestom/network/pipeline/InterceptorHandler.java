package com.viaversion.minestom.network.pipeline;

import com.viaversion.minestom.network.connection.PacketSerializer;
import com.viaversion.minestom.network.intercept.PacketInterceptor;
import com.viaversion.minestom.network.intercept.PacketSink;
import com.viaversion.minestom.network.intercept.PacketStage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import net.minestom.server.network.NetworkBuffer;

/**
 * Shows the packets passing one stage of the pipeline to the interceptors of the connection.
 */
public final class InterceptorHandler extends ChannelDuplexHandler {
    private final PacketStage stage;
    private final PacketInterceptor[] interceptors;

    public InterceptorHandler(final PacketStage stage, final PacketInterceptor[] interceptors) {
        this.stage = stage;
        this.interceptors = interceptors;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object message) {
        if (!(message instanceof ByteBuf data)) {
            ctx.fireChannelRead(message);
            return;
        }

        final NetworkBuffer original = PacketSerializer.wrap(data);
        NetworkBuffer packet = original;
        try {
            for (final PacketInterceptor interceptor : interceptors) {
                packet = interceptor.inbound(stage, packet);
                if (packet == null) {
                    data.release();
                    return;
                }
            }
        } catch (final Throwable t) {
            data.release();
            throw t;
        }

        if (packet == original) {
            ctx.fireChannelRead(data);
            return;
        }

        final ByteBuf replacement = PacketSerializer.copy(ctx.alloc(), packet);
        data.release();
        ctx.fireChannelRead(replacement);
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object message, final ChannelPromise promise) {
        final ByteBuf body;
        if (message instanceof PreframedPacket preframed) {
            body = preframed.content();
        } else if (message instanceof ByteBuf data) {
            body = data;
        } else {
            ctx.write(message, promise);
            return;
        }

        final OutboundChain chain = new OutboundChain(ctx, message, PacketSerializer.wrap(body), promise);
        try {
            chain.write(chain.original);
        } finally {
            chain.finish();
        }
    }

    /**
     * Walks one outgoing packet through the interceptors. A packet coming out unchanged keeps travelling as the
     * message it arrived as, so that a frame prepared ahead of time is not lost on the way.
     */
    private final class OutboundChain implements PacketSink {
        private final ChannelHandlerContext ctx;
        private final Object message;
        private final NetworkBuffer original;
        private final ChannelPromise promise;
        private int position;
        private boolean forwarded;

        private OutboundChain(final ChannelHandlerContext ctx, final Object message, final NetworkBuffer original, final ChannelPromise promise) {
            this.ctx = ctx;
            this.message = message;
            this.original = original;
            this.promise = promise;
        }

        @Override
        public void write(final NetworkBuffer packet) {
            if (position < interceptors.length) {
                interceptors[position++].outbound(stage, packet, this);
                return;
            }
            if (forwarded) {
                throw new IllegalStateException("Packet has already been written");
            }

            if (packet != original) {
                ctx.write(PacketSerializer.copy(ctx.alloc(), packet), promise);
            } else if (stage == PacketStage.CLIENT && message instanceof PreframedPacket preframed) {
                // Past the last interceptor nothing can change the packet anymore, its frame goes out as it is
                final ByteBuf frame = PacketSerializer.copy(ctx.alloc(), preframed.frames(), preframed.index(), preframed.length());
                ctx.pipeline().context(HandlerNames.FRAME_ENCODER).write(frame, promise);
            } else {
                forwarded = true;
                ctx.write(message, promise);
                return;
            }
            forwarded = true;
            ReferenceCountUtil.release(message);
        }

        private void finish() {
            if (!forwarded) {
                ReferenceCountUtil.release(message);
                promise.trySuccess();
            }
        }
    }
}
