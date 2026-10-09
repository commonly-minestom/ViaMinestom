package com.viaversion.minestom.network;

import java.time.Duration;
import java.util.Objects;

public record NetworkSettings(
    Duration readTimeout,
    Duration closeTimeout,
    Duration loginTimeout,
    int backlog,
    long maxPendingWriteBytes,
    long maxPendingReadBytes,
    int maxConnections,
    int maxConnectionsPerAddress
) {
    private static final long MEBIBYTE = 1024L * 1024;
    private static final NetworkSettings DEFAULTS = new NetworkSettings(
        Duration.ofSeconds(30), Duration.ofSeconds(10), Duration.ofSeconds(60), 128,
        8 * MEBIBYTE, 8 * MEBIBYTE, Integer.MAX_VALUE, Integer.MAX_VALUE
    );

    public NetworkSettings {
        requirePositive(readTimeout, "readTimeout");
        requirePositive(closeTimeout, "closeTimeout");
        requirePositive(loginTimeout, "loginTimeout");
        if (backlog < 0) {
            throw new IllegalArgumentException("backlog must not be negative");
        }
        requirePositive(maxPendingWriteBytes, "maxPendingWriteBytes");
        requirePositive(maxPendingReadBytes, "maxPendingReadBytes");
        requirePositive(maxConnections, "maxConnections");
        requirePositive(maxConnectionsPerAddress, "maxConnectionsPerAddress");
    }

    public NetworkSettings(final Duration readTimeout, final Duration closeTimeout, final int backlog) {
        this(readTimeout, closeTimeout, DEFAULTS.loginTimeout, backlog, DEFAULTS.maxPendingWriteBytes,
            DEFAULTS.maxPendingReadBytes, DEFAULTS.maxConnections, DEFAULTS.maxConnectionsPerAddress);
    }

    public static NetworkSettings defaults() {
        return DEFAULTS;
    }

    public NetworkSettings withReadTimeout(final Duration readTimeout) {
        return new NetworkSettings(readTimeout, closeTimeout, loginTimeout, backlog, maxPendingWriteBytes, maxPendingReadBytes, maxConnections, maxConnectionsPerAddress);
    }

    public NetworkSettings withCloseTimeout(final Duration closeTimeout) {
        return new NetworkSettings(readTimeout, closeTimeout, loginTimeout, backlog, maxPendingWriteBytes, maxPendingReadBytes, maxConnections, maxConnectionsPerAddress);
    }

    public NetworkSettings withLoginTimeout(final Duration loginTimeout) {
        return new NetworkSettings(readTimeout, closeTimeout, loginTimeout, backlog, maxPendingWriteBytes, maxPendingReadBytes, maxConnections, maxConnectionsPerAddress);
    }

    public NetworkSettings withBacklog(final int backlog) {
        return new NetworkSettings(readTimeout, closeTimeout, loginTimeout, backlog, maxPendingWriteBytes, maxPendingReadBytes, maxConnections, maxConnectionsPerAddress);
    }

    public NetworkSettings withMaxPendingWriteBytes(final long maxPendingWriteBytes) {
        return new NetworkSettings(readTimeout, closeTimeout, loginTimeout, backlog, maxPendingWriteBytes, maxPendingReadBytes, maxConnections, maxConnectionsPerAddress);
    }

    public NetworkSettings withMaxPendingReadBytes(final long maxPendingReadBytes) {
        return new NetworkSettings(readTimeout, closeTimeout, loginTimeout, backlog, maxPendingWriteBytes, maxPendingReadBytes, maxConnections, maxConnectionsPerAddress);
    }

    public NetworkSettings withMaxConnections(final int maxConnections) {
        return new NetworkSettings(readTimeout, closeTimeout, loginTimeout, backlog, maxPendingWriteBytes, maxPendingReadBytes, maxConnections, maxConnectionsPerAddress);
    }

    public NetworkSettings withMaxConnectionsPerAddress(final int maxConnectionsPerAddress) {
        return new NetworkSettings(readTimeout, closeTimeout, loginTimeout, backlog, maxPendingWriteBytes, maxPendingReadBytes, maxConnections, maxConnectionsPerAddress);
    }

    private static void requirePositive(final Duration duration, final String name) {
        Objects.requireNonNull(duration, name);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requirePositive(final long value, final String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
