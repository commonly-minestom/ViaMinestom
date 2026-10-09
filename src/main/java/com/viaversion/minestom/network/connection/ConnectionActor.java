package com.viaversion.minestom.network.connection;

import com.viaversion.minestom.transport.channel.EventLoop;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.packet.server.SendablePacket;
import org.jetbrains.annotations.Nullable;

final class ConnectionActor implements Runnable, EventLoop {
    private static final int MESSAGES_PER_FLUSH = 256;

    private final Mailbox<ActorMessage> mailbox = new Mailbox<>(_ -> { });
    private volatile @Nullable Thread thread;
    private final ViaPlayerConnection connection;

    ConnectionActor(final ViaPlayerConnection connection) {
        this.connection = connection;
    }

    void post(final SendablePacket packet) {
        mailbox.post(new ActorMessage.OutboundPacket(packet));
    }

    void post(final InboundFrame frame) {
        mailbox.post(frame);
    }

    @Override
    public boolean inEventLoop() {
        return Thread.currentThread() == thread;
    }

    @Override
    public void execute(final Runnable task) {
        mailbox.post(new ActorMessage.Task(task));
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
        ActorMessage message;
        for (int processed = 0; processed < MESSAGES_PER_FLUSH && (message = mailbox.poll()) != null; processed++) {
            dispatch(message);
        }
    }

    private void drainTasks() {
        ActorMessage message;
        while ((message = mailbox.poll()) != null) {
            if (message instanceof ActorMessage.Task(final Runnable task)) {
                run(task);
            }
        }
    }

    private void dispatch(final ActorMessage message) {
        switch (message) {
            case ActorMessage.OutboundPacket(final SendablePacket packet) -> connection.outbound().write(packet);
            case InboundFrame frame -> {
                try {
                    connection.inbound().accept(frame);
                } finally {
                    connection.frameProcessed(frame);
                }
            }
            case ActorMessage.Task(final Runnable task) -> run(task);
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
