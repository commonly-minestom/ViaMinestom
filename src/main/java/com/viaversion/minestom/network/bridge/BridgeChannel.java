package com.viaversion.minestom.network.bridge;

import com.viaversion.minestom.transport.buffer.ByteBuf;
import com.viaversion.minestom.transport.buffer.ByteBufAllocator;
import com.viaversion.minestom.transport.channel.AbstractChannel;
import com.viaversion.minestom.transport.channel.EventLoop;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import java.net.SocketAddress;
import net.minestom.server.MinecraftServer;
import net.minestom.server.registry.Registries;
import org.jetbrains.annotations.Nullable;

public final class BridgeChannel extends AbstractChannel {
    private final BridgeHost host;
    private final SocketAddress remoteAddress;
    private final UserConnection userConnection;
    private final ViaTranslator translator;

    public BridgeChannel(final BridgeHost host, final EventLoop eventLoop, final SocketAddress remoteAddress, final @Nullable Registries registries) {
        super(eventLoop, ByteBufAllocator.DEFAULT);
        this.host = host;
        this.remoteAddress = remoteAddress;
        this.userConnection = new UserConnectionImpl(this, false);
        new ProtocolPipelineImpl(userConnection);
        this.translator = new ViaTranslator(userConnection, alloc(), registries);
        pipeline()
            .addLast(HandlerNames.PACKET_INLET, new PacketInletHandler())
            .addLast(HandlerNames.VIA_DECODER, new ViaDecoderHandler(translator))
            .addLast(HandlerNames.VIA_ENCODER, new ViaEncoderHandler(translator))
            .addLast(HandlerNames.PACKET_HANDLER, new PipelineTailHandler(host));
    }

    public BridgeHost host() {
        return host;
    }

    public UserConnection userConnection() {
        return userConnection;
    }

    public ViaTranslator translator() {
        return translator;
    }

    @Override
    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    @Override
    protected void doWrite(final Object message) {
        if (!(message instanceof ByteBuf packet)) {
            return;
        }
        try {
            host.outbound(packet);
        } catch (final Throwable cause) {
            exceptionCaught(cause);
            throw cause;
        }
    }

    @Override
    protected void doClose() {
        host.closed();
    }

    @Override
    protected void exceptionCaught(final Throwable cause) {
        MinecraftServer.getExceptionManager().handleException(cause);
    }
}
