package com.viaversion.minestom.network.connection;

import com.viaversion.minestom.network.bridge.ViaTranslator;
import com.viaversion.minestom.network.codec.WireCodec;
import com.viaversion.minestom.network.intercept.PacketStage;
import java.util.zip.DataFormatException;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.Registries;

final class InboundPipeline {
    private final ViaPlayerConnection connection;
    private final InterceptorChain interceptors;
    private final ViaTranslator translator;
    private final WireCodec codec;
    private final PacketDispatcher dispatcher;
    private final Registries registries;

    InboundPipeline(final ViaPlayerConnection connection, final InterceptorChain interceptors, final ViaTranslator translator, final WireCodec codec, final PacketDispatcher dispatcher, final Registries registries) {
        this.connection = connection;
        this.interceptors = interceptors;
        this.translator = translator;
        this.codec = codec;
        this.dispatcher = dispatcher;
        this.registries = registries;
    }

    void accept(final InboundFrame frame) {
        if (!connection.isOnline()) {
            return;
        }
        try {
            final NetworkBuffer body = codec.decode(PacketSerializer.wrap(frame.bytes(), registries), ServerFlag.MAX_PACKET_SIZE);
            fromClientStage(body);
        } catch (final DataFormatException | RuntimeException e) {
            MinecraftServer.getExceptionManager().handleException(e);
            connection.disconnect();
        }
    }

    boolean fromClientStage(final NetworkBuffer packet) {
        final NetworkBuffer next = interceptors.inbound(PacketStage.CLIENT, packet);
        return next != null && afterClientInterceptors(next);
    }

    boolean afterClientInterceptors(final NetworkBuffer packet) {
        final NetworkBuffer translated = translator.serverbound(packet);
        return translated != null && afterTranslation(translated);
    }

    boolean afterTranslation(final NetworkBuffer packet) {
        final NetworkBuffer next = interceptors.inbound(PacketStage.SERVER, packet);
        return next != null && afterServerInterceptors(next);
    }

    boolean afterServerInterceptors(final NetworkBuffer packet) {
        dispatcher.dispatch(packet);
        return true;
    }
}
