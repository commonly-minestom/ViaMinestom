package com.viaversion.minestom.network.connection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.jetbrains.annotations.Nullable;

final class Mailbox<T> {
    private static final int CHUNK_SIZE = 256;

    private final MessagePassingQueue<T> queue = new MpscUnboundedArrayQueue<>(CHUNK_SIZE);
    private final AtomicBoolean signalled = new AtomicBoolean();
    private final ReentrantLock orphanLock = new ReentrantLock();
    private final Consumer<T> orphanHandler;
    private volatile @Nullable Thread consumer;
    private volatile boolean closed;
    private volatile boolean terminated;

    Mailbox(final Consumer<T> orphanHandler) {
        this.orphanHandler = orphanHandler;
    }

    void bind(final Thread thread) {
        this.consumer = thread;
    }

    void post(final T item) {
        queue.offer(item);
        if (terminated) {
            drainOrphans();
            return;
        }
        wake();
    }

    @Nullable T poll() {
        return queue.relaxedPoll();
    }

    boolean await() {
        signalled.set(false);
        if (!queue.isEmpty()) {
            return true;
        }
        if (closed) {
            return false;
        }
        LockSupport.park(this);
        return true;
    }

    void close() {
        closed = true;
        wake();
    }

    boolean isClosed() {
        return closed;
    }

    void terminate() {
        closed = true;
        terminated = true;
        drainOrphans();
    }

    boolean isTerminated() {
        return terminated;
    }

    private void drainOrphans() {
        orphanLock.lock();
        try {
            T item;
            while ((item = queue.poll()) != null) {
                orphanHandler.accept(item);
            }
        } finally {
            orphanLock.unlock();
        }
    }

    private void wake() {
        if (signalled.compareAndSet(false, true)) {
            LockSupport.unpark(consumer);
        }
    }
}
