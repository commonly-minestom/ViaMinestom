package com.viaversion.minestom.network.bridge;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public interface BridgeExecutor extends Executor {

    boolean inThread(Thread thread);

    void shutdown();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
}
