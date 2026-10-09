package com.viaversion.minestom.network.connection;

import com.viaversion.minestom.transport.channel.EventLoop;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.packet.server.SendablePacket;
import org.jetbrains.annotations.Nullable;

final class ConnectionActor implements Runnable, EventLoop {
    private final Mailbox<Object> mailbox = new Mailbox<>(_ -> { });
    private volatile @Nullable Thread thread;
    private ViaPlayerConnection connection;

    void attach(final ViaPlayerConnection connection) {
        this.connection = connection;
    }

    void post(final Object message) {
        mailbox.post(message);
    }

    @Override
    public boolean inEventLoop() {
        return Thread.currentThread() == thread;
    }

    @Override
    public void execute(final Runnable task) {
        mailbox.post(task);
    }

    void shutdown() {
        mailbox.close();
    }

    boolean isShutdown() {
        return mailbox.isClosed();
    }

    @Override
    public void run() {
        thread = Thread.currentThread();
        mailbox.bind(Thread.currentThread());
        try {
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
            case InboundFrame frame -> {
                try {
                    connection.inbound().accept(frame);
                } finally {
                    connection.frameProcessed(frame);
                }
            }
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
}
