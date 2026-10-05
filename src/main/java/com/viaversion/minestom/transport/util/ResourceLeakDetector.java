package com.viaversion.minestom.transport.util;

import java.util.Objects;

public final class ResourceLeakDetector {
    private static volatile Level level = Level.DISABLED;

    private ResourceLeakDetector() {
    }

    public static Level getLevel() {
        return level;
    }

    public static void setLevel(final Level level) {
        ResourceLeakDetector.level = Objects.requireNonNull(level, "level");
    }

    public enum Level {
        DISABLED,
        SIMPLE,
        ADVANCED,
        PARANOID
    }
}
