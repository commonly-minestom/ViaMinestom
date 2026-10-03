package com.viaversion.minestom.network.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ProtocolDetectionResult;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import java.net.InetSocketAddress;
import java.util.List;
import net.minestom.server.ServerFlag;
import net.minestom.server.network.player.PlayerSocketConnection;

/**
 * Detects an optional PROXY protocol header in front of the handshake and applies the forwarded address.
 */
public final class ProxyProtocolHandler extends ByteToMessageDecoder {
    private static final String HEADER_DECODER = "proxy-protocol-decoder";
    private static final String ADDRESS_HANDLER = "proxy-protocol-address";

    private final PlayerSocketConnection connection;

    public ProxyProtocolHandler(final PlayerSocketConnection connection) {
        this.connection = connection;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
        final ProtocolDetectionResult<HAProxyProtocolVersion> detection = HAProxyMessageDecoder.detectProtocol(in);
        switch (detection.state()) {
            case NEEDS_MORE_DATA -> {
            }
            case INVALID -> {
                if (ServerFlag.PROXY_PROTOCOL_REQUIRED) {
                    throw new DecoderException("Missing required PROXY protocol header");
                }
                ctx.pipeline().remove(this);
            }
            case DETECTED -> {
                final ChannelPipeline pipeline = ctx.pipeline();
                pipeline.addAfter(ctx.name(), HEADER_DECODER, new HAProxyMessageDecoder());
                pipeline.addAfter(HEADER_DECODER, ADDRESS_HANDLER, new AddressHandler(connection));
                pipeline.remove(this);
            }
        }
    }

    private static final class AddressHandler extends SimpleChannelInboundHandler<HAProxyMessage> {
        private final PlayerSocketConnection connection;

        private AddressHandler(final PlayerSocketConnection connection) {
            this.connection = connection;
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, final HAProxyMessage header) {
            if (header.sourceAddress() != null) {
                connection.setRemoteAddress(new InetSocketAddress(header.sourceAddress(), header.sourcePort()));
            }
            ctx.pipeline().remove(this);
        }
    }
}
