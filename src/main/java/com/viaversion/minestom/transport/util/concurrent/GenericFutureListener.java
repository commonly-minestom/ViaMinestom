package com.viaversion.minestom.transport.util.concurrent;

import java.util.EventListener;

@FunctionalInterface
public interface GenericFutureListener<F extends Future<?>> extends EventListener {

    void operationComplete(F future) throws Exception;
}
