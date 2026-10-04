package com.viaversion.minestom.network.bridge;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.AbstractEventExecutor;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.TimeUnit;

public final class BridgeEventLoop extends AbstractEventExecutor implements EventLoop {
    private final BridgeExecutor executor;
    private final Promise<Void> termination = new DefaultPromise<>(ImmediateEventExecutor.INSTANCE);

    public BridgeEventLoop(final BridgeExecutor executor) {
        this.executor = executor;
    }

    public void terminated() {
        termination.trySuccess(null);
    }

    @Override
    public EventLoopGroup parent() {
        return this;
    }

    @Override
    public EventLoop next() {
        return this;
    }

    @Override
    public ChannelFuture register(final Channel channel) {
        return register(new DefaultChannelPromise(channel, this));
    }

    @Override
    public ChannelFuture register(final ChannelPromise promise) {
        promise.channel().unsafe().register(this, promise);
        return promise;
    }

    @Deprecated
    @Override
    public ChannelFuture register(final Channel channel, final ChannelPromise promise) {
        channel.unsafe().register(this, promise);
        return promise;
    }

    @Override
    public boolean inEventLoop(final Thread thread) {
        return executor.inThread(thread);
    }

    @Override
    public void execute(final Runnable task) {
        executor.execute(task);
    }

    @Override
    public boolean isShuttingDown() {
        return executor.isShutdown();
    }

    @Override
    public Future<?> shutdownGracefully(final long quietPeriod, final long timeout, final TimeUnit unit) {
        executor.shutdown();
        return termination;
    }

    @Override
    public Future<?> terminationFuture() {
        return termination;
    }

    @Deprecated
    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }
}
