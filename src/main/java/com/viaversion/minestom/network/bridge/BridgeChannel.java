package com.viaversion.minestom.network.bridge;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicLong;
import net.minestom.server.registry.Registries;
import org.jetbrains.annotations.Nullable;

public final class BridgeChannel extends AbstractChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(false);
    private static final ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(false);
    private static final AtomicLong IDS = new AtomicLong();

    private final BridgeHost host;
    private final SocketAddress localAddress;
    private final SocketAddress remoteAddress;
    private final ChannelConfig config;
    private final UserConnection userConnection;
    private final ViaTranslator translator;
    private volatile boolean open = true;

    public BridgeChannel(final BridgeHost host, final SocketAddress localAddress, final SocketAddress remoteAddress, final @Nullable Registries registries) {
        super(null, new BridgeChannelId(IDS.incrementAndGet()));
        this.host = host;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.config = new DefaultChannelConfig(this).setAllocator(ALLOCATOR);
        this.userConnection = new UserConnectionImpl(this, false);
        new ProtocolPipelineImpl(userConnection);
        this.translator = new ViaTranslator(userConnection, ALLOCATOR, registries);
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

    public void register(final BridgeEventLoop loop) {
        loop.register(this);
    }

    @Override
    public ChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean isActive() {
        return open && isRegistered();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new BridgeUnsafe();
    }

    @Override
    protected boolean isCompatible(final EventLoop loop) {
        return loop instanceof BridgeEventLoop;
    }

    @Override
    protected SocketAddress localAddress0() {
        return localAddress;
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return remoteAddress;
    }

    @Override
    protected void doBind(final SocketAddress address) {
        throw new UnsupportedOperationException("A bridge channel is bound by its connection");
    }

    @Override
    protected void doDisconnect() {
        doClose();
    }

    @Override
    protected void doClose() {
        if (open) {
            open = false;
            host.closed();
        }
    }

    @Override
    protected void doBeginRead() {
    }

    @Override
    protected void doWrite(final ChannelOutboundBuffer in) {
        Object message;
        while ((message = in.current()) != null) {
            if (message instanceof ByteBuf packet) {
                host.outbound(packet);
            }
            in.remove();
        }
    }

    private final class BridgeUnsafe extends AbstractUnsafe {

        @Override
        public void connect(final SocketAddress remote, final SocketAddress local, final ChannelPromise promise) {
            safeSetFailure(promise, new UnsupportedOperationException("A bridge channel cannot connect"));
        }
    }
}
