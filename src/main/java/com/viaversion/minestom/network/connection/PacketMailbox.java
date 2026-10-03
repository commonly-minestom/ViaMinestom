package com.viaversion.minestom.network.connection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import org.jctools.queues.MessagePassingQueue;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.jetbrains.annotations.Nullable;

/**
 * Hands items over to a single consumer thread which parks while there is nothing to do.
 */
final class PacketMailbox<T> {
    private static final int CHUNK_SIZE = 256;

    private final MessagePassingQueue<T> queue = new MpscUnboundedArrayQueue<>(CHUNK_SIZE);
    private final AtomicBoolean signalled = new AtomicBoolean();
    private final Thread consumer;
    private volatile boolean closed;

    PacketMailbox(final Thread consumer) {
        this.consumer = consumer;
    }

    void offer(final T item) {
        queue.offer(item);
        wake();
    }

    @Nullable T poll() {
        return queue.relaxedPoll();
    }

    /**
     * Parks the consumer until an item is offered.
     *
     * @return false once the mailbox is closed and drained
     */
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

    private void wake() {
        if (signalled.compareAndSet(false, true)) {
            LockSupport.unpark(consumer);
        }
    }
}
