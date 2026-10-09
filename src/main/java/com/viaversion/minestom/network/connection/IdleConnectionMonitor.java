package com.viaversion.minestom.network.connection;

import com.viaversion.minestom.network.NetworkSettings;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public final class IdleConnectionMonitor {
    private static final Duration SWEEP_INTERVAL = Duration.ofSeconds(1);

    private final ConnectionRegistry registry;
    private final Deadlines deadlines;
    private @Nullable ScheduledExecutorService scheduler;

    public IdleConnectionMonitor(final ConnectionRegistry registry, final NetworkSettings settings) {
        this.registry = registry;
        this.deadlines = Deadlines.of(settings);
    }

    public synchronized void start() {
        if (scheduler != null) {
            return;
        }
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("Via-Monitor").factory());
        scheduler.scheduleWithFixedDelay(this::sweep, SWEEP_INTERVAL.toMillis(), SWEEP_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
        this.scheduler = scheduler;
    }

    public synchronized void stop() {
        final ScheduledExecutorService scheduler = this.scheduler;
        if (scheduler != null) {
            scheduler.shutdownNow();
            this.scheduler = null;
        }
    }

    private void sweep() {
        final long now = System.nanoTime();
        try {
            registry.forEach(connection -> connection.enforceDeadlines(now, deadlines));
        } catch (final RuntimeException e) {
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }
}
