package com.viaversion.minestom.network;

import java.time.Duration;
import java.util.Objects;

public record NetworkSettings(Duration readTimeout, Duration closeTimeout, int backlog) {
    private static final NetworkSettings DEFAULTS = new NetworkSettings(Duration.ofSeconds(30), Duration.ofSeconds(10), 128);

    public NetworkSettings {
        requirePositive(readTimeout, "readTimeout");
        requirePositive(closeTimeout, "closeTimeout");
        if (backlog < 0) {
            throw new IllegalArgumentException("backlog must not be negative");
        }
    }

    public static NetworkSettings defaults() {
        return DEFAULTS;
    }

    public NetworkSettings withReadTimeout(final Duration readTimeout) {
        return new NetworkSettings(readTimeout, closeTimeout, backlog);
    }

    public NetworkSettings withCloseTimeout(final Duration closeTimeout) {
        return new NetworkSettings(readTimeout, closeTimeout, backlog);
    }

    public NetworkSettings withBacklog(final int backlog) {
        return new NetworkSettings(readTimeout, closeTimeout, backlog);
    }

    private static void requirePositive(final Duration duration, final String name) {
        Objects.requireNonNull(duration, name);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
