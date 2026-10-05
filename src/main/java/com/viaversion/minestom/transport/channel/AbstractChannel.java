package com.viaversion.minestom.transport.channel;

import com.viaversion.minestom.transport.buffer.ByteBufAllocator;
import com.viaversion.minestom.transport.util.ReferenceCountUtil;
import java.nio.channels.ClosedChannelException;
import java.util.Objects;

public abstract class AbstractChannel implements Channel {
    private final EventLoop eventLoop;
    private final ByteBufAllocator allocator;
    private final DefaultChannelPipeline pipeline;
    private final DefaultChannelPromise closeFuture;
    private final ChannelFuture succeededFuture;
    private volatile boolean open = true;

    protected AbstractChannel(final EventLoop eventLoop, final ByteBufAllocator allocator) {
        this.eventLoop = Objects.requireNonNull(eventLoop, "eventLoop");
        this.allocator = Objects.requireNonNull(allocator, "allocator");
        this.pipeline = new DefaultChannelPipeline(this);
        this.closeFuture = new DefaultChannelPromise(this);
        this.succeededFuture = new DefaultChannelPromise(this).setSuccess();
    }

    protected abstract void doWrite(Object message) throws Exception;

    protected abstract void doClose() throws Exception;

    protected abstract void exceptionCaught(Throwable cause);

    @Override
    public ByteBufAllocator alloc() {
        return allocator;
    }

    @Override
    public EventLoop eventLoop() {
        return eventLoop;
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public ChannelFuture close() {
        if (eventLoop.inEventLoop()) {
            shutdown();
        } else {
            eventLoop.execute(this::shutdown);
        }
        return closeFuture;
    }

    @Override
    public ChannelFuture closeFuture() {
        return closeFuture;
    }

    @Override
    public ChannelPromise newPromise() {
        return new DefaultChannelPromise(this);
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return succeededFuture;
    }

    @Override
    public ChannelFuture newFailedFuture(final Throwable cause) {
        return new DefaultChannelPromise(this).setFailure(cause);
    }

    void write(final Object message, final ChannelPromise promise) {
        try {
            if (!open) {
                throw new ClosedChannelException();
            }
            doWrite(message);
            promise.trySuccess();
        } catch (final Throwable cause) {
            promise.tryFailure(cause);
        } finally {
            ReferenceCountUtil.release(message);
        }
    }

    private void shutdown() {
        if (!open) {
            return;
        }
        open = false;
        try {
            doClose();
        } catch (final Throwable cause) {
            exceptionCaught(cause);
        } finally {
            closeFuture.trySuccess();
        }
    }
}
