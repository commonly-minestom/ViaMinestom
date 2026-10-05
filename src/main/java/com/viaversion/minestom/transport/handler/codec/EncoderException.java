package com.viaversion.minestom.transport.handler.codec;

public class EncoderException extends CodecException {

    public EncoderException() {
    }

    public EncoderException(final String message) {
        super(message);
    }

    public EncoderException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public EncoderException(final Throwable cause) {
        super(cause);
    }
}
