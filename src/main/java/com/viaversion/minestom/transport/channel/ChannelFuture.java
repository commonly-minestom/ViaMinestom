package com.viaversion.minestom.transport.channel;

import com.viaversion.minestom.transport.util.concurrent.Future;
import com.viaversion.minestom.transport.util.concurrent.GenericFutureListener;

public interface ChannelFuture extends Future<Void> {

    Channel channel();

    @Override
    ChannelFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener);
}
