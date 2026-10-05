package com.viaversion.minestom.transport.util;

public final class ReferenceCountUtil {

    private ReferenceCountUtil() {
    }

    public static boolean release(final Object message) {
        return message instanceof ReferenceCounted counted && counted.release();
    }
}
