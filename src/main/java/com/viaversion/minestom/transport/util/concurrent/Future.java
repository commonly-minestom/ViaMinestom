package com.viaversion.minestom.transport.util.concurrent;

import org.jetbrains.annotations.Nullable;

public interface Future<V> {

    boolean isDone();

    boolean isSuccess();

    @Nullable Throwable cause();

    Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);
}
