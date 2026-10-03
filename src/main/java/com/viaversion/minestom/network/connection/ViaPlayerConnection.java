package com.viaversion.minestom.network.connection;

import com.viaversion.minestom.network.intercept.PacketInterceptor;
import com.viaversion.minestom.network.intercept.PacketInterceptors;
import com.viaversion.minestom.network.intercept.PacketStage;
import com.viaversion.minestom.network.pipeline.CipherHandler;
import com.viaversion.minestom.network.pipeline.HandlerNames;
import com.viaversion.minestom.network.pipeline.PacketDeflater;
import com.viaversion.minestom.network.pipeline.PacketInflater;
import com.viaversion.minestom.network.pipeline.PreframedPacket;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.util.AttributeKey;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.ListenerHandle;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.extras.mojangAuth.MojangCrypt;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.PacketParser;
import net.minestom.server.network.packet.PacketRegistry;
import net.minestom.server.network.packet.PacketVanilla;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.common.ClientCookieResponsePacket;
import net.minestom.server.network.packet.client.common.ClientKeepAlivePacket;
import net.minestom.server.network.packet.client.common.ClientPingRequestPacket;
import net.minestom.server.network.packet.client.configuration.ClientFinishConfigurationPacket;
import net.minestom.server.network.packet.client.configuration.ClientSelectKnownPacksPacket;
import net.minestom.server.network.packet.client.handshake.ClientHandshakePacket;
import net.minestom.server.network.packet.client.login.ClientEncryptionResponsePacket;
import net.minestom.server.network.packet.client.login.ClientLoginAcknowledgedPacket;
import net.minestom.server.network.packet.client.login.ClientLoginPluginResponsePacket;
import net.minestom.server.network.packet.client.login.ClientLoginStartPacket;
import net.minestom.server.network.packet.client.play.ClientCreativeInventoryActionPacket;
import net.minestom.server.network.packet.client.status.StatusRequestPacket;
import net.minestom.server.network.packet.server.BufferedPacket;
import net.minestom.server.network.packet.server.CachedPacket;
import net.minestom.server.network.packet.server.FramedPacket;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.login.SetCompressionPacket;
import net.minestom.server.network.player.PlayerSocketConnection;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;

/**
 * A player connection whose transport is a Netty channel carrying the Via handlers.
 * <p>
 * Minestom keeps talking to it like to any other socket connection. Packets are still parsed, dispatched and
 * serialized on a pair of virtual threads, only the bytes travel through the channel pipeline instead of
 * being read from and written to the socket directly.
 */
public final class ViaPlayerConnection extends PlayerSocketConnection {
    public static final AttributeKey<ViaPlayerConnection> ATTRIBUTE = AttributeKey.valueOf("viaminestom-connection");

    private static final ThreadFactory READER_THREADS = Thread.ofVirtual().name("Via-Socket-Reader-", 0).factory();
    private static final ThreadFactory WRITER_THREADS = Thread.ofVirtual().name("Via-Socket-Writer-", 0).factory();

    // Has to mirror the packets PlayerSocketConnection handles outside of the player tick
    private static final Set<Class<? extends ClientPacket>> IMMEDIATE_PACKETS = Set.of(
        ClientHandshakePacket.class,
        ClientCookieResponsePacket.class,
        StatusRequestPacket.class,
        ClientPingRequestPacket.class,
        ClientKeepAlivePacket.class,
        ClientLoginStartPacket.class,
        ClientEncryptionResponsePacket.class,
        ClientLoginPluginResponsePacket.class,
        ClientSelectKnownPacksPacket.class,
        ClientLoginAcknowledgedPacket.class,
        ClientFinishConfigurationPacket.class
    );

    private static final UUID NIL_PROFILE_ID = new UUID(0, 0);
    private static final PacketInterceptor[] NO_INTERCEPTORS = new PacketInterceptor[0];

    private final Channel channel;
    private final UserConnection userConnection;
    private final PacketParser.Client packetParser = MinecraftServer.getServer().packetParser();
    private final ListenerHandle<PlayerPacketOutEvent> outgoing = EventDispatcher.getHandle(PlayerPacketOutEvent.class);
    private final PacketMailbox<ByteBuf> inbound;
    private final PacketMailbox<SendablePacket> outbound;
    private PacketInterceptor[] interceptors = NO_INTERCEPTORS;

    // Confined to the reader thread
    private ConnectionState parserState = ConnectionState.HANDSHAKE;

    // Confined to the writer thread
    private @Nullable ChannelHandlerContext frameEncoderContext;
    private boolean compressed;
    private boolean pendingFlush;

    private volatile boolean encrypted;

    private ViaPlayerConnection(final Channel channel, final UserConnection userConnection, final Thread readThread, final Thread writeThread) {
        super(null, channel.remoteAddress(), readThread, writeThread);
        this.channel = channel;
        this.userConnection = userConnection;
        this.inbound = new PacketMailbox<>(readThread);
        this.outbound = new PacketMailbox<>(writeThread);
    }

    public static ViaPlayerConnection open(final Channel channel, final UserConnection userConnection) {
        final AtomicReference<ViaPlayerConnection> reference = new AtomicReference<>();
        final Thread readThread = READER_THREADS.newThread(() -> reference.get().readLoop());
        final Thread writeThread = WRITER_THREADS.newThread(() -> reference.get().writeLoop());

        final ViaPlayerConnection connection = new ViaPlayerConnection(channel, userConnection, readThread, writeThread);
        reference.set(connection);
        channel.attr(ATTRIBUTE).set(connection);
        connection.interceptors = PacketInterceptors.create(connection);
        return connection;
    }

    /**
     * Returns the player behind a Via connection, if it has got past the login.
     */
    public static @Nullable Player player(final UserConnection userConnection) {
        final Channel channel = userConnection.getChannel();
        if (channel == null) {
            return null;
        }

        final ViaPlayerConnection connection = channel.attr(ATTRIBUTE).get();
        return connection != null ? connection.getPlayer() : null;
    }

    public void start() {
        readThread().start();
        writeThread().start();
    }

    public Channel channel() {
        return channel;
    }

    public UserConnection userConnection() {
        return userConnection;
    }

    public SocketAddress getLocalAddress() {
        return channel.localAddress();
    }

    /**
     * Returns the version the client is actually running, as opposed to {@link #getProtocolVersion()}
     * which reports the version the handshake has been translated to.
     */
    public ProtocolVersion clientVersion() {
        return userConnection.getProtocolInfo().protocolVersion();
    }

    /**
     * Returns the interceptors installed on this connection, in the order packets from the client pass them.
     */
    public PacketInterceptor[] interceptors() {
        return interceptors.clone();
    }

    /**
     * Returns the interceptor of the given type installed on this connection, if any.
     */
    public <T extends PacketInterceptor> @Nullable T interceptor(final Class<T> type) {
        for (final PacketInterceptor interceptor : interceptors) {
            if (type.isInstance(interceptor)) {
                return type.cast(interceptor);
            }
        }
        return null;
    }

    /**
     * Runs a task on the event loop of the connection.
     */
    public void execute(final Runnable task) {
        channel.eventLoop().execute(task);
    }

    /**
     * Sends everything written so far to the client.
     */
    public void flush() {
        channel.flush();
    }

    /**
     * Writes an already serialized packet in the version of the server, id included, as if Minestom had sent it.
     */
    public void write(final NetworkBuffer packet) {
        channel.writeAndFlush(PacketSerializer.copy(channel.alloc(), packet), channel.voidPromise());
    }

    /**
     * Writes an already serialized packet, id included, from past the interceptors of the given stage on.
     * The packet has to be in the protocol version of that stage.
     */
    public void write(final PacketStage after, final NetworkBuffer packet) {
        final ChannelHandlerContext context = interceptorContext(after);
        context.writeAndFlush(PacketSerializer.copy(channel.alloc(), packet), channel.voidPromise());
    }

    /**
     * Hands over an already serialized packet in the version of the client, id included, as if it had just
     * been received.
     */
    public void read(final NetworkBuffer packet) {
        final ChannelHandlerContext context = interceptorContext(PacketStage.CLIENT);
        final ByteBuf data = PacketSerializer.copy(channel.alloc(), packet);
        context.executor().execute(() -> {
            try {
                ((ChannelInboundHandler) context.handler()).channelRead(context, data);
            } catch (final Exception e) {
                context.fireExceptionCaught(e);
            }
        });
    }

    /**
     * Hands over an already serialized packet, id included, from past the interceptors of the given stage on.
     * The packet has to be in the protocol version of that stage.
     */
    public void read(final PacketStage after, final NetworkBuffer packet) {
        interceptorContext(after).fireChannelRead(PacketSerializer.copy(channel.alloc(), packet));
    }

    private ChannelHandlerContext interceptorContext(final PacketStage stage) {
        final String name = stage == PacketStage.CLIENT ? HandlerNames.CLIENT_INTERCEPTOR : HandlerNames.SERVER_INTERCEPTOR;
        final ChannelHandlerContext context = channel.pipeline().context(name);
        if (context == null) {
            throw new IllegalStateException("No packet interceptor is installed on this connection");
        }
        return context;
    }

    /**
     * Takes ownership of a packet that went through the pipeline. Called from the event loop.
     */
    public void receive(final ByteBuf packet) {
        if (!isOnline()) {
            packet.release();
            return;
        }
        inbound.offer(packet);
    }

    /**
     * Called from the event loop once the channel is gone.
     */
    public void channelClosed() {
        inbound.close();
    }

    @Override
    public void sendPacket(final SendablePacket packet) {
        outbound.offer(packet);
    }

    @Override
    public void sendPackets(final Collection<SendablePacket> packets) {
        for (final SendablePacket packet : packets) {
            outbound.offer(packet);
        }
    }

    @Override
    public void setEncryptionKey(final SecretKey secretKey) {
        Check.stateCondition(encrypted, "Encryption is already enabled!");
        this.encrypted = true;

        final CipherHandler handler = new CipherHandler(
            MojangCrypt.getCipher(Cipher.DECRYPT_MODE, secretKey),
            MojangCrypt.getCipher(Cipher.ENCRYPT_MODE, secretKey)
        );
        channel.eventLoop().execute(() -> channel.pipeline().addBefore(HandlerNames.FRAME_DECODER, HandlerNames.CIPHER, handler));
    }

    @Override
    public void startCompression() {
        final int threshold = MinecraftServer.getCompressionThreshold();
        Check.stateCondition(threshold == 0, "Compression cannot be enabled because the threshold is equal to 0");
        sendPacket(new SetCompressionPacket(threshold));
    }

    @Override
    public void disconnect() {
        super.disconnect();
        inbound.close();
        outbound.close();
    }

    @Override
    public void read(final PacketParser<ClientPacket> packetParser) {
        throw new UnsupportedOperationException("Packets are delivered by the channel pipeline");
    }

    @Override
    public void flushSync() {
        throw new UnsupportedOperationException("Packets are flushed by the channel pipeline");
    }

    private void readLoop() {
        try {
            while (isOnline()) {
                final ByteBuf packet = inbound.poll();
                if (packet == null) {
                    if (!inbound.await()) {
                        break;
                    }
                    continue;
                }

                try {
                    handle(packet);
                } finally {
                    packet.release();
                }
            }
        } catch (final Throwable t) {
            MinecraftServer.getExceptionManager().handleException(t);
        } finally {
            disconnect();
            releaseInbound();
            for (final PacketInterceptor interceptor : interceptors) {
                interceptor.close();
            }
        }
    }

    private void handle(final ByteBuf data) {
        final NetworkBuffer buffer = PacketSerializer.wrap(data);
        final int packetId = buffer.read(NetworkBuffer.VAR_INT);
        final PacketRegistry.PacketInfo<? extends ClientPacket> packetInfo = packetParser.stateRegistry(parserState).packetInfo(packetId);
        if (packetInfo.packetClass() == ClientCreativeInventoryActionPacket.class && !isCreative()) {
            return;
        }

        final ClientPacket packet = withProfileId(packetInfo.serializer().read(buffer));
        parserState = PacketVanilla.nextClientState(packet, parserState);
        try {
            if (IMMEDIATE_PACKETS.contains(packet.getClass())) {
                MinecraftServer.getPacketListenerManager().processClientPacket(packet, this);
                return;
            }

            final Player player = getPlayer();
            if (player != null) {
                player.addPacketToQueue(packet);
            }
        } catch (final Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    /**
     * Clients older than 1.20.2 do not announce a profile id when logging in, which Via fills in with a nil id.
     * Minestom takes the announced id as is in offline mode, so those clients get the one vanilla would derive.
     */
    private static ClientPacket withProfileId(final ClientPacket packet) {
        if (packet instanceof ClientLoginStartPacket(String username, UUID profileId) && profileId.equals(NIL_PROFILE_ID)) {
            final UUID offlineId = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
            return new ClientLoginStartPacket(username, offlineId);
        }
        return packet;
    }

    private boolean isCreative() {
        final Player player = getPlayer();
        return player != null && player.getGameMode() == GameMode.CREATIVE;
    }

    private void releaseInbound() {
        try {
            // Runs on the thread filling the mailbox, so that nothing can slip in afterwards
            channel.eventLoop().execute(() -> {
                ByteBuf packet;
                while ((packet = inbound.poll()) != null) {
                    packet.release();
                }
            });
        } catch (final RejectedExecutionException ignored) {
            // The event loop has been shut down along with the server
        }
    }

    private void writeLoop() {
        try {
            while (true) {
                final SendablePacket packet = outbound.poll();
                if (packet != null) {
                    write(packet);
                    continue;
                }

                flushPending();
                if (!outbound.await()) {
                    break;
                }
            }
        } catch (final Throwable t) {
            MinecraftServer.getExceptionManager().handleException(t);
            disconnect();
        } finally {
            channel.flush();
            channel.close();
        }
    }

    private void flushPending() {
        if (pendingFlush) {
            pendingFlush = false;
            channel.flush();
        }
    }

    private void write(SendablePacket sendable) {
        final Player player = getPlayer();
        final ConnectionState state = getServerState();
        if (player != null) {
            if (outgoing.hasListener()) {
                final ServerPacket packet = SendablePacket.extractServerPacket(state, sendable);
                if (packet != null) {
                    final PlayerPacketOutEvent event = new PlayerPacketOutEvent(player, packet);
                    outgoing.call(event);
                    if (event.isCancelled()) {
                        return;
                    }
                }
            }
            if (ServerFlag.AUTOMATIC_COMPONENT_TRANSLATION && sendable instanceof ServerPacket.ComponentHolding holder) {
                final Locale playerLocale = player.getLocale();
                final Locale locale = playerLocale != null ? playerLocale : MinestomAdventure.getDefaultLocale();
                sendable = holder.copyWithOperator(component -> MinestomAdventure.COMPONENT_TRANSLATOR.apply(component, locale));
            }
        }

        switch (sendable) {
            case ServerPacket packet -> writePacket(state, packet, null);
            case FramedPacket framed -> writePacket(state, framed.packet(), framed.body());
            case CachedPacket cached -> writePacket(state, cached.packet(state), cached.body(state));
            case BufferedPacket buffered -> writeBuffered(buffered.buffer(), buffered.index(), buffered.length());
        }
    }

    /**
     * Packets framed ahead of time can be forwarded untouched as long as nothing has to be translated
     * and they have been framed the same way this connection frames its own packets.
     */
    private boolean canPassThrough() {
        return !userConnection.isActive() && compressed == MinecraftServer.getCompressionThreshold() > 0;
    }

    private void writePacket(final ConnectionState state, final ServerPacket packet, final @Nullable NetworkBuffer frame) {
        if (frame != null && interceptors.length == 0 && canPassThrough()) {
            writeFramed(frame, 0, frame.capacity());
            return;
        }

        final ConnectionState nextState = PacketVanilla.nextServerState(packet, state);
        if (nextState != state) {
            setServerState(nextState);
        }

        final ByteBuf body = PacketSerializer.serialize(channel.alloc(), state, packet);
        writeBody(body, frame, 0, frame != null ? frame.capacity() : 0);
        if (packet instanceof SetCompressionPacket(int threshold)) {
            enableCompression(threshold);
        }
    }

    private void writeBuffered(final NetworkBuffer frames, final long index, final long length) {
        if (interceptors.length == 0 && canPassThrough()) {
            writeFramed(frames, index, length);
            return;
        }

        final boolean compressedFrames = MinecraftServer.getCompressionThreshold() > 0;
        PacketSerializer.unframe(channel.alloc(), frames, index, length, compressedFrames,
            (body, frameIndex, frameLength) -> writeBody(body, frames, frameIndex, frameLength));
    }

    /**
     * Hands a packet body to the pipeline. With interceptors around, a frame that would otherwise have been
     * forwarded directly travels along with the body, to be used if they leave the packet alone.
     */
    private void writeBody(final ByteBuf body, final @Nullable NetworkBuffer frame, final long frameIndex, final long frameLength) {
        final Object message = frame != null && canPassThrough() ? new PreframedPacket(body, frame, frameIndex, frameLength) : body;
        channel.write(message, channel.voidPromise());
        pendingFlush = true;
    }

    private void writeFramed(final NetworkBuffer frames, final long index, final long length) {
        ChannelHandlerContext context = frameEncoderContext;
        if (context == null) {
            context = channel.pipeline().context(HandlerNames.FRAME_ENCODER);
            frameEncoderContext = context;
        }

        // Starts past the frame encoder, skipping translation, compression and framing
        context.write(PacketSerializer.copy(channel.alloc(), frames, index, length), channel.voidPromise());
        pendingFlush = true;
    }

    private void enableCompression(final int threshold) {
        this.compressed = true;
        channel.eventLoop().execute(() -> {
            final ChannelPipeline pipeline = channel.pipeline();
            pipeline.addAfter(HandlerNames.FRAME_ENCODER, HandlerNames.DECOMPRESS, new PacketInflater());
            pipeline.addAfter(HandlerNames.DECOMPRESS, HandlerNames.COMPRESS, new PacketDeflater(threshold));
        });
    }
}
