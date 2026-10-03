package com.viaversion.minestom.network.pipeline;

import com.viaversion.minestom.network.connection.ViaPlayerConnection;
import com.viaversion.viaversion.exception.CancelCodecException;
import com.viaversion.viaversion.util.PipelineUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.timeout.ReadTimeoutException;
import java.io.IOException;
import net.minestom.server.MinecraftServer;

/**
 * Terminates the pipeline by handing translated packets over to the Minestom connection.
 */
public final class InboundPacketHandler extends ChannelInboundHandlerAdapter {
    private final ViaPlayerConnection connection;

    public InboundPacketHandler(final ViaPlayerConnection connection) {
        this.connection = connection;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object message) {
        connection.receive((ByteBuf) message);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        connection.channelClosed();
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        if (PipelineUtil.containsCause(cause, CancelCodecException.class)) {
            return;
        }
        if (cause instanceof EncoderException) {
            // A single packet failed to translate, the connection itself is still healthy
            MinecraftServer.getExceptionManager().handleException(cause);
            return;
        }

        if (!(cause instanceof IOException) && !(cause instanceof ReadTimeoutException)) {
            MinecraftServer.getExceptionManager().handleException(cause);
        }
        ctx.close();
    }
}
