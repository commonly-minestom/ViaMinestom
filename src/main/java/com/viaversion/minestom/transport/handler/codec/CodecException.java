package com.viaversion.minestom.transport.handler.codec;

public class CodecException extends RuntimeException {

    public CodecException() {
    }

    public CodecException(final String message) {
        super(message);
    }

    public CodecException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public CodecException(final Throwable cause) {
        super(cause);
    }
}
