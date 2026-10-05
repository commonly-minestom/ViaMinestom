package com.viaversion.minestom.network.bridge;

import com.viaversion.minestom.transport.buffer.ByteBuf;
import com.viaversion.minestom.transport.channel.ChannelHandlerContext;
import com.viaversion.minestom.transport.channel.ChannelInboundHandlerAdapter;
import com.viaversion.minestom.transport.util.ReferenceCountUtil;
import com.viaversion.viaversion.exception.CancelCodecException;
import java.nio.channels.ClosedChannelException;
import net.minestom.server.MinecraftServer;

final class PipelineTailHandler extends ChannelInboundHandlerAdapter {
    private final BridgeHost host;

    PipelineTailHandler(final BridgeHost host) {
        this.host = host;
    }

    @Override
    public void channelRead(final ChannelHandlerContext context, final Object message) {
        if (!(message instanceof ByteBuf packet)) {
            ReferenceCountUtil.release(message);
            return;
        }
        try {
            host.inbound(packet);
        } finally {
            packet.release();
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext context, final Throwable cause) {
        if (cause instanceof CancelCodecException || cause instanceof ClosedChannelException) {
            return;
        }
        MinecraftServer.getExceptionManager().handleException(cause);
    }
}
