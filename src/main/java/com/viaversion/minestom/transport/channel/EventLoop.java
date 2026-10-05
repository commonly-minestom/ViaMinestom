package com.viaversion.minestom.transport.channel;

import java.util.concurrent.Executor;

public interface EventLoop extends Executor {

    boolean inEventLoop();
}
