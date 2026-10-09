package com.viaversion.minestom.network.connection;

import com.viaversion.minestom.network.bridge.BridgeChannel;
import com.viaversion.minestom.network.bridge.BridgeHost;
import com.viaversion.minestom.network.NetworkSettings;
import com.viaversion.minestom.network.bridge.ByteBufs;
import com.viaversion.minestom.network.codec.WireCodec;
import com.viaversion.minestom.network.intercept.PacketInterceptor;
import com.viaversion.minestom.network.intercept.PacketInterceptors;
import com.viaversion.minestom.network.intercept.PacketStage;
import com.viaversion.minestom.transport.buffer.ByteBuf;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.crypto.SecretKey;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.PacketParser;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.login.SetCompressionPacket;
import net.minestom.server.network.player.PlayerSocketConnection;
import net.minestom.server.registry.Registries;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;

public final class ViaPlayerConnection extends PlayerSocketConnection {
    private static final ThreadFactory ACTORS = Thread.ofVirtual().name("Via-Actor-", 0).factory();
    private static final ThreadFactory READERS = Thread.ofVirtual().name("Via-Reader-", 0).factory();
    private static final ThreadFactory WRITERS = Thread.ofVirtual().name("Via-Writer-", 0).factory();
    private static final Thread UNUSED = Thread.ofVirtual().unstarted(() -> { });

    private final SocketChannel socket;
    private final SocketAddress localAddress;
    private final Registries registries;
    private final ConnectionActor actor;
    private final Thread actorThread;
    private final SocketReader reader;
    private final Thread readerThread;
    private final SocketWriter writer;
    private final Thread writerThread;
    private final Consumer<ViaPlayerConnection> onTerminated;
    private final BridgeChannel bridge;
    private final InterceptorChain interceptors;
    private final WireCodec codec = new WireCodec();
    private final InboundPipeline inbound;
    private final OutboundPipeline outbound;
    private final AtomicBoolean encrypted = new AtomicBoolean();
    private final AtomicBoolean compressionRequested = new AtomicBoolean();
    private final AtomicBoolean closeRequested = new AtomicBoolean();
    private final AtomicInteger liveWorkers = new AtomicInteger(2);
    private final CountDownLatch terminated = new CountDownLatch(1);
    private final long connectedAt = System.nanoTime();
    private volatile long closeRequestedAt;
    private volatile long lastReadAt = System.nanoTime();

    private ViaPlayerConnection(final SocketChannel socket, final BufferPool pool, final NetworkSettings settings, final Consumer<ViaPlayerConnection> onTerminated) throws IOException {
        final Registries registries = MinecraftServer.getRegistries();
        super(socket, socket.getRemoteAddress(), UNUSED, UNUSED);
        this.socket = socket;
        this.localAddress = socket.getLocalAddress();
        this.registries = registries;
        this.actor = new ConnectionActor(this);
        this.actorThread = ACTORS.newThread(actor);
        this.reader = new SocketReader(this, socket, registries, settings.maxPendingReadBytes());
        this.readerThread = READERS.newThread(reader);
        this.writer = new SocketWriter(socket, pool, settings.maxPendingWriteBytes(), this::requestClose, this::writerStopped);
        this.writerThread = WRITERS.newThread(writer);
        this.onTerminated = onTerminated;
        this.bridge = new BridgeChannel(new Host(), actor, getRemoteAddress(), registries);
        this.interceptors = new InterceptorChain(PacketInterceptors.create(this));
        final PacketDispatcher dispatcher = new PacketDispatcher(this, MinecraftServer.getServer().packetParser());
        this.inbound = new InboundPipeline(this, interceptors, bridge.translator(), codec, dispatcher, registries);
        this.outbound = new OutboundPipeline(this, interceptors, bridge.translator(), codec, new WireBatch(pool, writer::send), pool);
    }

    public static ViaPlayerConnection open(final SocketChannel socket, final NetworkSettings settings, final Consumer<ViaPlayerConnection> onTerminated) throws IOException {
        return new ViaPlayerConnection(socket, BufferPool.minestom(), settings, onTerminated);
    }

    public static @Nullable Player player(final UserConnection userConnection) {
        return userConnection.getChannel() instanceof BridgeChannel bridge ? bridge.host().player() : null;
    }

    public void start() {
        actorThread.start();
        readerThread.start();
        writerThread.start();
    }

    @Override
    public Thread readThread() {
        return readerThread;
    }

    @Override
    public Thread writeThread() {
        return writerThread;
    }

    public UserConnection userConnection() {
        return bridge.userConnection();
    }

    public ProtocolVersion clientVersion() {
        return userConnection().getProtocolInfo().protocolVersion();
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public PacketInterceptor[] interceptors() {
        return interceptors.snapshot();
    }

    public <T extends PacketInterceptor> @Nullable T interceptor(final Class<T> type) {
        return interceptors.find(type);
    }

    public void execute(final Runnable task) {
        if (actor.inEventLoop()) {
            task.run();
        } else if (!actor.isShutdown()) {
            actor.execute(task);
        }
    }

    public void write(final NetworkBuffer packet) {
        final byte[] bytes = PacketSerializer.snapshot(packet);
        execute(() -> outbound.fromServerStage(PacketSerializer.wrap(bytes, registries)));
    }

    public void write(final PacketStage after, final NetworkBuffer packet) {
        final byte[] bytes = PacketSerializer.snapshot(packet);
        execute(() -> {
            switch (after) {
                case SERVER -> outbound.afterServerInterceptors(PacketSerializer.wrap(bytes, registries));
                case CLIENT -> outbound.afterClientInterceptors(PacketSerializer.wrap(bytes, registries));
            }
        });
    }

    public void read(final NetworkBuffer packet) {
        final byte[] bytes = PacketSerializer.snapshot(packet);
        execute(() -> inbound.fromClientStage(PacketSerializer.wrap(bytes, registries)));
    }

    public void read(final PacketStage after, final NetworkBuffer packet) {
        final byte[] bytes = PacketSerializer.snapshot(packet);
        execute(() -> {
            switch (after) {
                case CLIENT -> inbound.afterClientInterceptors(PacketSerializer.wrap(bytes, registries));
                case SERVER -> inbound.afterServerInterceptors(PacketSerializer.wrap(bytes, registries));
            }
        });
    }

    @Override
    public void sendPacket(final SendablePacket packet) {
        actor.post(packet);
    }

    @Override
    public void sendPackets(final Collection<SendablePacket> packets) {
        for (final SendablePacket packet : packets) {
            actor.post(packet);
        }
    }

    @Override
    public void setEncryptionKey(final SecretKey secretKey) {
        Check.stateCondition(!encrypted.compareAndSet(false, true), "Encryption is already enabled!");
        final Ciphers ciphers = Ciphers.of(secretKey);
        reader.decryptWith(ciphers.decrypt());
        execute(() -> codec.encrypt(ciphers.encrypt()));
    }

    @Override
    public void startCompression() {
        Check.stateCondition(!compressionRequested.compareAndSet(false, true), "Compression is already enabled!");
        final int threshold = MinecraftServer.getCompressionThreshold();
        Check.stateCondition(threshold == 0, "Compression cannot be enabled because the threshold is equal to 0");
        sendPacket(new SetCompressionPacket(threshold));
    }

    @Override
    public void disconnect() {
        super.disconnect();
        requestClose();
    }

    @Override
    public void read(final PacketParser<ClientPacket> packetParser) {
        throw new UnsupportedOperationException("Packets are read by the connection actor");
    }

    @Override
    public void flushSync() {
        throw new UnsupportedOperationException("Packets are flushed by the connection actor");
    }

    InboundPipeline inbound() {
        return inbound;
    }

    OutboundPipeline outbound() {
        return outbound;
    }

    void frame(final InboundFrame frame) {
        actor.post(frame);
    }

    void touch() {
        lastReadAt = System.nanoTime();
    }

    void requestClose() {
        if (closeRequested.compareAndSet(false, true)) {
            closeRequestedAt = System.nanoTime();
            actor.shutdown();
        }
    }

    private static long allowedUntilPlay(final ConnectionState state, final Deadlines deadlines) {
        return switch (state) {
            case HANDSHAKE, STATUS, LOGIN -> deadlines.loginNanos();
            case CONFIGURATION -> deadlines.configurationNanos();
            case PLAY -> Long.MAX_VALUE;
        };
    }

    boolean isClosing() {
        return closeRequested.get();
    }

    void frameProcessed(final InboundFrame frame) {
        reader.release(frame.length());
    }

    void enforceDeadlines(final long now, final Deadlines deadlines) {
        if (closeRequested.get()) {
            if (now - closeRequestedAt > deadlines.closeNanos()) {
                closeSocket();
            }
            return;
        }
        if (now - lastReadAt > deadlines.readNanos()) {
            requestClose();
        } else if (now - connectedAt > allowedUntilPlay(getClientState(), deadlines)) {
            requestClose();
        }
    }

    boolean awaitTermination(final Duration timeout) {
        try {
            return terminated.await(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (final InterruptedException _) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    void closeSocket() {
        try {
            socket.close();
        } catch (final IOException _) {
        }
    }

    void actorStopping() {
        disconnect();
        interceptors.close();
        bridge.close();
    }

    void actorStopped() {
        writer.shutdown();
        workerStopped();
    }

    void writerStopped() {
        workerStopped();
    }

    private void workerStopped() {
        if (liveWorkers.decrementAndGet() == 0) {
            terminated.countDown();
            onTerminated.accept(this);
        }
    }

    private final class Host implements BridgeHost {

        @Override
        public @Nullable Player player() {
            return getPlayer();
        }

        @Override
        public void outbound(final ByteBuf packet) {
            outbound.afterTranslation(ByteBufs.view(packet, registries));
        }

        @Override
        public void inbound(final ByteBuf packet) {
            inbound.afterTranslation(ByteBufs.view(packet, registries));
        }

        @Override
        public void closed() {
            disconnect();
        }
    }
}
