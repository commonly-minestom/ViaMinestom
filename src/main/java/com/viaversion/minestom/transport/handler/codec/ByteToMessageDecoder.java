package com.viaversion.minestom.transport.handler.codec;

import com.viaversion.minestom.transport.buffer.ByteBuf;
import com.viaversion.minestom.transport.channel.ChannelHandlerContext;
import com.viaversion.minestom.transport.channel.ChannelInboundHandlerAdapter;
import java.util.List;

public abstract class ByteToMessageDecoder extends ChannelInboundHandlerAdapter {

    @Override
    public void handlerAdded(final ChannelHandlerContext context) {
        throw new UnsupportedOperationException(getClass().getName() + " decodes a byte stream, while the channels of this transport carry whole packets");
    }

    protected abstract void decode(ChannelHandlerContext context, ByteBuf input, List<Object> output) throws Exception;
}
