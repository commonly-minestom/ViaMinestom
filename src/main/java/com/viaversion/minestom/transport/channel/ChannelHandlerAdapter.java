package com.viaversion.minestom.transport.channel;

public abstract class ChannelHandlerAdapter implements ChannelHandler {
    boolean added;

    public boolean isSharable() {
        return getClass().isAnnotationPresent(Sharable.class);
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext context) throws Exception {
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext context) throws Exception {
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext context, final Throwable cause) throws Exception {
        context.fireExceptionCaught(cause);
    }
}
