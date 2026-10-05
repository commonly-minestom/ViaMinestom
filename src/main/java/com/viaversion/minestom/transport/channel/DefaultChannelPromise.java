package com.viaversion.minestom.transport.channel;

import com.viaversion.minestom.transport.util.concurrent.Future;
import com.viaversion.minestom.transport.util.concurrent.GenericFutureListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

final class DefaultChannelPromise implements ChannelPromise {
    private static final Object SUCCESS = new Object();

    private final AbstractChannel channel;
    private @Nullable List<GenericFutureListener<? extends Future<? super Void>>> listeners;
    private boolean notifying;
    private volatile @Nullable Object outcome;

    DefaultChannelPromise(final AbstractChannel channel) {
        this.channel = channel;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public boolean isDone() {
        return outcome != null;
    }

    @Override
    public boolean isSuccess() {
        return outcome == SUCCESS;
    }

    @Override
    public @Nullable Throwable cause() {
        return outcome instanceof Throwable cause ? cause : null;
    }

    @Override
    public ChannelPromise setSuccess() {
        if (!trySuccess()) {
            throw new IllegalStateException("The promise has already been completed");
        }
        return this;
    }

    @Override
    public ChannelPromise setFailure(final Throwable cause) {
        if (!tryFailure(cause)) {
            throw new IllegalStateException("The promise has already been completed", cause);
        }
        return this;
    }

    @Override
    public boolean trySuccess() {
        return complete(SUCCESS);
    }

    @Override
    public boolean tryFailure(final Throwable cause) {
        return complete(Objects.requireNonNull(cause, "cause"));
    }

    @Override
    public ChannelPromise addListener(final GenericFutureListener<? extends Future<? super Void>> listener) {
        Objects.requireNonNull(listener, "listener");
        final boolean done;
        synchronized (this) {
            if (listeners == null) {
                listeners = new ArrayList<>(1);
            }
            listeners.add(listener);
            done = outcome != null;
        }
        if (done) {
            notifyListeners();
        }
        return this;
    }

    private boolean complete(final Object result) {
        final boolean awaited;
        synchronized (this) {
            if (outcome != null) {
                return false;
            }
            outcome = result;
            awaited = listeners != null;
        }
        if (awaited) {
            notifyListeners();
        }
        return true;
    }

    private void notifyListeners() {
        final EventLoop loop = channel.eventLoop();
        if (loop.inEventLoop()) {
            drainListeners();
        } else {
            loop.execute(this::drainListeners);
        }
    }

    private void drainListeners() {
        while (true) {
            final List<GenericFutureListener<? extends Future<? super Void>>> batch;
            synchronized (this) {
                if (notifying || listeners == null) {
                    return;
                }
                notifying = true;
                batch = listeners;
                listeners = null;
            }
            try {
                for (final GenericFutureListener<? extends Future<? super Void>> listener : batch) {
                    invoke(listener);
                }
            } finally {
                synchronized (this) {
                    notifying = false;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void invoke(final GenericFutureListener<? extends Future<? super Void>> listener) {
        try {
            ((GenericFutureListener<ChannelFuture>) listener).operationComplete(this);
        } catch (final Throwable cause) {
            channel.exceptionCaught(cause);
        }
    }
}
