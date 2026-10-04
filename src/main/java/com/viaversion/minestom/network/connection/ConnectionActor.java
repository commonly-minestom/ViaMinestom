package com.viaversion.minestom.network.connection;

import com.viaversion.minestom.network.bridge.BridgeExecutor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.packet.server.SendablePacket;
import org.jetbrains.annotations.Nullable;

final class ConnectionActor implements Runnable, BridgeExecutor {
    private final Mailbox<Object> mailbox = new Mailbox<>(this::orphaned);
    private final CountDownLatch terminated = new CountDownLatch(1);
    private volatile @Nullable Thread thread;
    private ViaPlayerConnection connection;

    void attach(final ViaPlayerConnection connection) {
        this.connection = connection;
    }

    void post(final Object message) {
        mailbox.post(message);
    }

    boolean inThread() {
        return Thread.currentThread() == thread;
    }

    @Override
    public boolean inThread(final Thread candidate) {
        return candidate == thread;
    }

    @Override
    public void execute(final Runnable task) {
        mailbox.post(task);
    }

    @Override
    public void shutdown() {
        mailbox.close();
    }

    @Override
    public boolean isShutdown() {
        return mailbox.isClosed();
    }

    @Override
    public boolean isTerminated() {
        return mailbox.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return terminated.await(timeout, unit);
    }

    @Override
    public void run() {
        thread = Thread.currentThread();
        mailbox.bind(Thread.currentThread());
        try {
            connection.actorStarted();
            while (true) {
                drain();
                connection.outbound().flush();
                if (!mailbox.await()) {
                    break;
                }
            }
        } catch (final Throwable t) {
            MinecraftServer.getExceptionManager().handleException(t);
        } finally {
            stop();
        }
    }

    private void stop() {
        try {
            connection.actorStopping();
            drainTasks();
            connection.outbound().close();
        } catch (final Throwable t) {
            MinecraftServer.getExceptionManager().handleException(t);
        } finally {
            mailbox.terminate();
            terminated.countDown();
            connection.actorStopped();
        }
    }

    private void drain() {
        Object message;
        while ((message = mailbox.poll()) != null) {
            dispatch(message);
        }
    }

    private void drainTasks() {
        Object message;
        while ((message = mailbox.poll()) != null) {
            if (message instanceof Runnable task) {
                run(task);
            }
        }
    }

    private void dispatch(final Object message) {
        switch (message) {
            case SendablePacket packet -> connection.outbound().write(packet);
            case InboundFrame frame -> connection.inbound().accept(frame);
            case Runnable task -> run(task);
            default -> throw new IllegalArgumentException("Unsupported message type " + message.getClass().getName());
        }
    }

    private void run(final Runnable task) {
        try {
            task.run();
        } catch (final Throwable t) {
            MinecraftServer.getExceptionManager().handleException(t);
        }
    }

    private void orphaned(final Object message) {
        if (message instanceof Runnable task) {
            run(task);
        }
    }
}
