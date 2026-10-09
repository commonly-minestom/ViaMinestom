package com.viaversion.minestom.network;

import java.time.Duration;
import java.util.Objects;

public record NetworkSettings(Duration readTimeout, Duration closeTimeout, int backlog, long maxPendingWriteBytes) {
    private static final long DEFAULT_MAX_PENDING_WRITE_BYTES = 8L * 1024 * 1024;
    private static final NetworkSettings DEFAULTS = new NetworkSettings(Duration.ofSeconds(30), Duration.ofSeconds(10), 128, DEFAULT_MAX_PENDING_WRITE_BYTES);

    public NetworkSettings {
        requirePositive(readTimeout, "readTimeout");
        requirePositive(closeTimeout, "closeTimeout");
        if (backlog < 0) {
            throw new IllegalArgumentException("backlog must not be negative");
        }
        if (maxPendingWriteBytes <= 0) {
            throw new IllegalArgumentException("maxPendingWriteBytes must be positive");
        }
    }

    public NetworkSettings(final Duration readTimeout, final Duration closeTimeout, final int backlog) {
        this(readTimeout, closeTimeout, backlog, DEFAULT_MAX_PENDING_WRITE_BYTES);
    }

    public static NetworkSettings defaults() {
        return DEFAULTS;
    }

    public NetworkSettings withReadTimeout(final Duration readTimeout) {
        return new NetworkSettings(readTimeout, closeTimeout, backlog, maxPendingWriteBytes);
    }

    public NetworkSettings withCloseTimeout(final Duration closeTimeout) {
        return new NetworkSettings(readTimeout, closeTimeout, backlog, maxPendingWriteBytes);
    }

    public NetworkSettings withBacklog(final int backlog) {
        return new NetworkSettings(readTimeout, closeTimeout, backlog, maxPendingWriteBytes);
    }

    public NetworkSettings withMaxPendingWriteBytes(final long maxPendingWriteBytes) {
        return new NetworkSettings(readTimeout, closeTimeout, backlog, maxPendingWriteBytes);
    }

    private static void requirePositive(final Duration duration, final String name) {
        Objects.requireNonNull(duration, name);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
