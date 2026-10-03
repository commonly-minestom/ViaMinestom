package com.viaversion.minestom.network;

import com.viaversion.minestom.network.connection.ViaPlayerConnection;
import com.viaversion.minestom.network.intercept.PacketInterceptor;
import com.viaversion.minestom.network.intercept.PacketStage;
import com.viaversion.minestom.network.pipeline.FrameDecoder;
import com.viaversion.minestom.network.pipeline.FrameEncoder;
import com.viaversion.minestom.network.pipeline.HandlerNames;
import com.viaversion.minestom.network.pipeline.InboundPacketHandler;
import com.viaversion.minestom.network.pipeline.InterceptorHandler;
import com.viaversion.minestom.network.pipeline.ProxyProtocolHandler;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.platform.ViaChannelInitializer;
import com.viaversion.viaversion.platform.ViaDecodeHandler;
import com.viaversion.viaversion.platform.ViaEncodeHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minestom.server.ServerFlag;

final class ClientChannelInitializer extends ChannelInitializer<Channel> {
    private static final int READ_TIMEOUT_SECONDS = 30;

    @Override
    protected void initChannel(final Channel channel) {
        final UserConnection userConnection = ViaChannelInitializer.createUserConnection(channel, false);
        final ViaPlayerConnection connection = ViaPlayerConnection.open(channel, userConnection);

        final ChannelPipeline pipeline = channel.pipeline();
        if (ServerFlag.PROXY_PROTOCOL) {
            pipeline.addLast(HandlerNames.PROXY_PROTOCOL, new ProxyProtocolHandler(connection));
        }
        pipeline.addLast(HandlerNames.READ_TIMEOUT, new ReadTimeoutHandler(READ_TIMEOUT_SECONDS));
        pipeline.addLast(HandlerNames.FRAME_DECODER, new FrameDecoder(connection));
        pipeline.addLast(HandlerNames.FRAME_ENCODER, FrameEncoder.INSTANCE);
        final PacketInterceptor[] interceptors = connection.interceptors();
        if (interceptors.length > 0) {
            pipeline.addLast(HandlerNames.CLIENT_INTERCEPTOR, new InterceptorHandler(PacketStage.CLIENT, interceptors));
        }
        pipeline.addLast(HandlerNames.VIA_DECODER, new ViaDecodeHandler(userConnection));
        pipeline.addLast(HandlerNames.VIA_ENCODER, new ViaEncodeHandler(userConnection));
        if (interceptors.length > 0) {
            pipeline.addLast(HandlerNames.SERVER_INTERCEPTOR, new InterceptorHandler(PacketStage.SERVER, interceptors));
        }
        pipeline.addLast(HandlerNames.PACKET_HANDLER, new InboundPacketHandler(connection));

        connection.start();
    }
}
