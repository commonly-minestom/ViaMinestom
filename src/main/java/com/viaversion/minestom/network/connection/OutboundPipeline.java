package com.viaversion.minestom.network.connection;

import com.viaversion.minestom.network.bridge.ViaTranslator;
import com.viaversion.minestom.network.codec.WireCodec;
import com.viaversion.minestom.network.intercept.PacketStage;
import java.util.Locale;
import java.util.zip.DataFormatException;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.ListenerHandle;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.PacketVanilla;
import net.minestom.server.network.packet.server.BufferedPacket;
import net.minestom.server.network.packet.server.CachedPacket;
import net.minestom.server.network.packet.server.FramedPacket;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.login.SetCompressionPacket;
import org.jetbrains.annotations.Nullable;

final class OutboundPipeline {
    private record Preframed(NetworkBuffer frames, long index, long length) {
    }

    private final ViaPlayerConnection connection;
    private final InterceptorChain interceptors;
    private final ViaTranslator translator;
    private final WireCodec codec;
    private final WireBatch batch;
    private final BufferPool pool;
    private final ListenerHandle<PlayerPacketOutEvent> outgoing = EventDispatcher.getHandle(PlayerPacketOutEvent.class);

    OutboundPipeline(final ViaPlayerConnection connection, final InterceptorChain interceptors, final ViaTranslator translator,
                     final WireCodec codec, final WireBatch batch, final BufferPool pool) {
        this.connection = connection;
        this.interceptors = interceptors;
        this.translator = translator;
        this.codec = codec;
        this.batch = batch;
        this.pool = pool;
    }

    void write(final SendablePacket sendable) {
        try {
            dispatch(sendable);
        } catch (final DataFormatException | RuntimeException e) {
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    void flush() {
        batch.flush();
    }

    void close() {
        batch.close();
    }

    private void dispatch(SendablePacket sendable) throws DataFormatException {
        final Player player = connection.getPlayer();
        final ConnectionState state = connection.getServerState();
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
            case ServerPacket packet -> packet(state, packet, null);
            case FramedPacket framed -> packet(state, framed.packet(), framed.body());
            case CachedPacket cached -> packet(state, cached.packet(state), cached.body(state));
            case BufferedPacket buffered -> buffered(buffered.buffer(), buffered.index(), buffered.length());
        }
    }

    private void packet(final ConnectionState state, final ServerPacket packet, final @Nullable NetworkBuffer frame) {
        if (frame != null && interceptors.isEmpty() && passThrough()) {
            raw(frame, 0, frame.capacity());
            return;
        }

        final ConnectionState nextState = PacketVanilla.nextServerState(packet, state);
        if (nextState != state) {
            connection.setServerState(nextState);
        }

        final NetworkBuffer scratch = pool.acquire();
        final boolean delivered;
        try {
            final NetworkBuffer body = PacketSerializer.serialize(scratch, state, packet);
            final Preframed preframed = frame != null && passThrough() ? new Preframed(frame, 0, frame.capacity()) : null;
            delivered = fromServerStage(body, preframed);
        } finally {
            pool.release(scratch);
        }
        if (delivered && packet instanceof SetCompressionPacket(final int threshold)) {
            codec.enableCompression(threshold);
        }
    }

    private void buffered(final NetworkBuffer frames, final long index, final long length) throws DataFormatException {
        if (interceptors.isEmpty() && passThrough()) {
            raw(frames, index, length);
            return;
        }
        final boolean compressedFrames = MinecraftServer.getCompressionThreshold() > 0;
        PacketSerializer.unframe(frames, index, length, compressedFrames, (body, frameIndex, frameLength) ->
            fromServerStage(body, passThrough() ? new Preframed(frames, frameIndex, frameLength) : null));
    }

    private boolean passThrough() {
        return !translator.active() && codec.compressed() == (MinecraftServer.getCompressionThreshold() > 0);
    }

    boolean fromServerStage(final NetworkBuffer packet) {
        return fromServerStage(packet, null);
    }

    boolean afterServerInterceptors(final NetworkBuffer packet) {
        return afterServerInterceptors(packet, null);
    }

    boolean afterTranslation(final NetworkBuffer packet) {
        return afterTranslation(packet, null);
    }

    boolean afterClientInterceptors(final NetworkBuffer packet) {
        return afterClientInterceptors(packet, null);
    }

    private boolean fromServerStage(final NetworkBuffer packet, final @Nullable Preframed preframed) {
        if (interceptors.isEmpty()) {
            return afterServerInterceptors(packet, preframed);
        }
        return interceptors.outbound(PacketStage.SERVER, packet, next -> afterServerInterceptors(next, next == packet ? preframed : null));
    }

    private boolean afterServerInterceptors(final NetworkBuffer packet, final @Nullable Preframed preframed) {
        final NetworkBuffer translated = translator.clientbound(packet);
        return translated != null && afterTranslation(translated, translated == packet ? preframed : null);
    }

    private boolean afterTranslation(final NetworkBuffer packet, final @Nullable Preframed preframed) {
        if (interceptors.isEmpty()) {
            return afterClientInterceptors(packet, preframed);
        }
        return interceptors.outbound(PacketStage.CLIENT, packet, next -> afterClientInterceptors(next, next == packet ? preframed : null));
    }

    private boolean afterClientInterceptors(final NetworkBuffer packet, final @Nullable Preframed preframed) {
        if (preframed != null) {
            raw(preframed.frames(), preframed.index(), preframed.length());
        } else {
            batch.write(out -> codec.encode(packet, out));
        }
        return true;
    }

    private void raw(final NetworkBuffer frames, final long index, final long length) {
        batch.write(out -> codec.copyRaw(frames, index, length, out));
    }
}
