package com.viaversion.minestom.network.codec;

import java.io.IOException;

public final class CorruptedFrameException extends IOException {

    public CorruptedFrameException(final String message) {
        super(message);
    }

    public CorruptedFrameException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
