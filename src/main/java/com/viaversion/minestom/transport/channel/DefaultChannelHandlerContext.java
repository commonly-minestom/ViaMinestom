package com.viaversion.minestom.transport.channel;

import com.viaversion.minestom.transport.buffer.ByteBufAllocator;
import java.util.Objects;

final class DefaultChannelHandlerContext implements ChannelHandlerContext {
    private final DefaultChannelPipeline pipeline;
    private final String name;
    private final ChannelHandler handler;
    private final boolean inbound;
    private final boolean outbound;
    private volatile DefaultChannelHandlerContext previous;
    private volatile DefaultChannelHandlerContext next;
    private volatile boolean removed;

    DefaultChannelHandlerContext(final DefaultChannelPipeline pipeline, final String name, final ChannelHandler handler) {
        this.pipeline = pipeline;
        this.name = name;
        this.handler = handler;
        this.inbound = handler instanceof ChannelInboundHandler;
        this.outbound = handler instanceof ChannelOutboundHandler;
    }

    @Override
    public Channel channel() {
        return pipeline.channel();
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ChannelHandler handler() {
        return handler;
    }

    @Override
    public ByteBufAllocator alloc() {
        return channel().alloc();
    }

    @Override
    public ChannelPromise newPromise() {
        return channel().newPromise();
    }

    @Override
    public ChannelHandlerContext fireChannelRead(final Object message) {
        Objects.requireNonNull(message, "message");
        final DefaultChannelHandlerContext target = nextInbound();
        final EventLoop loop = channel().eventLoop();
        if (loop.inEventLoop()) {
            target.invokeChannelRead(message);
        } else {
            loop.execute(() -> target.invokeChannelRead(message));
        }
        return this;
    }

    @Override
    public ChannelHandlerContext fireExceptionCaught(final Throwable cause) {
        Objects.requireNonNull(cause, "cause");
        final DefaultChannelHandlerContext target = next;
        final EventLoop loop = channel().eventLoop();
        if (loop.inEventLoop()) {
            target.invokeExceptionCaught(cause);
        } else {
            loop.execute(() -> target.invokeExceptionCaught(cause));
        }
        return this;
    }

    @Override
    public ChannelFuture write(final Object message, final ChannelPromise promise) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(promise, "promise");
        final DefaultChannelHandlerContext target = previousOutbound();
        final EventLoop loop = channel().eventLoop();
        if (loop.inEventLoop()) {
            target.invokeWrite(message, promise);
        } else {
            loop.execute(() -> target.invokeWrite(message, promise));
        }
        return promise;
    }

    @Override
    public ChannelFuture writeAndFlush(final Object message) {
        return write(message, newPromise());
    }

    DefaultChannelHandlerContext previous() {
        return previous;
    }

    DefaultChannelHandlerContext next() {
        return next;
    }

    void linkAfter(final DefaultChannelHandlerContext predecessor) {
        final DefaultChannelHandlerContext successor = predecessor.next;
        previous = predecessor;
        next = successor;
        predecessor.next = this;
        if (successor != null) {
            successor.previous = this;
        }
    }

    void unlink() {
        previous.next = next;
        next.previous = previous;
        removed = true;
    }

    private DefaultChannelHandlerContext nextInbound() {
        DefaultChannelHandlerContext context = next;
        while (!context.inbound) {
            context = context.next;
        }
        return context;
    }

    private DefaultChannelHandlerContext previousOutbound() {
        DefaultChannelHandlerContext context = previous;
        while (!context.outbound) {
            context = context.previous;
        }
        return context;
    }

    private void invokeChannelRead(final Object message) {
        if (removed) {
            fireChannelRead(message);
            return;
        }
        try {
            ((ChannelInboundHandler) handler).channelRead(this, message);
        } catch (final Throwable cause) {
            invokeExceptionCaught(cause);
        }
    }

    private void invokeExceptionCaught(final Throwable cause) {
        if (removed) {
            fireExceptionCaught(cause);
            return;
        }
        try {
            handler.exceptionCaught(this, cause);
        } catch (final Throwable failure) {
            if (failure != cause) {
                failure.addSuppressed(cause);
            }
            pipeline.uncaught(failure);
        }
    }

    private void invokeWrite(final Object message, final ChannelPromise promise) {
        if (removed) {
            write(message, promise);
            return;
        }
        try {
            ((ChannelOutboundHandler) handler).write(this, message, promise);
        } catch (final Throwable cause) {
            promise.tryFailure(cause);
        }
    }
}
