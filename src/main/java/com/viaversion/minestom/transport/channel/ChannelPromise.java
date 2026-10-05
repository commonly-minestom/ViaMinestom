package com.viaversion.minestom.transport.channel;

import com.viaversion.minestom.transport.util.concurrent.Future;
import com.viaversion.minestom.transport.util.concurrent.GenericFutureListener;

public interface ChannelPromise extends ChannelFuture {

    ChannelPromise setSuccess();

    ChannelPromise setFailure(Throwable cause);

    boolean trySuccess();

    boolean tryFailure(Throwable cause);

    @Override
    ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener);
}
