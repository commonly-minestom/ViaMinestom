package com.viaversion.minestom.network.bridge;

import com.viaversion.viaversion.exception.CancelCodecException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import java.nio.channels.ClosedChannelException;
import net.minestom.server.MinecraftServer;

final class PipelineTailHandler extends ChannelInboundHandlerAdapter {
    private final BridgeHost host;

    PipelineTailHandler(final BridgeHost host) {
        this.host = host;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object message) {
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
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        if (cause instanceof CancelCodecException || cause instanceof ClosedChannelException) {
            return;
        }
        MinecraftServer.getExceptionManager().handleException(cause);
    }
}
