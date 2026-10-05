package com.viaversion.minestom.transport.channel;

@ChannelHandler.Sharable
public abstract class ChannelInitializer<C extends Channel> extends ChannelInboundHandlerAdapter {

    protected abstract void initChannel(C channel) throws Exception;

    @Override
    @SuppressWarnings("unchecked")
    public void handlerAdded(final ChannelHandlerContext context) throws Exception {
        initChannel((C) context.channel());
        context.pipeline().remove(this);
    }
}
