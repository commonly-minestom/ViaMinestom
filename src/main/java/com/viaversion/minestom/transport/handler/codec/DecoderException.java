package com.viaversion.minestom.transport.handler.codec;

public class DecoderException extends CodecException {

    public DecoderException() {
    }

    public DecoderException(final String message) {
        super(message);
    }

    public DecoderException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DecoderException(final Throwable cause) {
        super(cause);
    }
}
