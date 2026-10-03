package com.viaversion.minestom.platform;

import com.viaversion.minestom.network.Transport;
import com.viaversion.minestom.network.pipeline.HandlerNames;
import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonObject;
import net.minestom.server.MinecraftServer;

/**
 * The pipeline is owned by this platform, so the Via handlers are installed while the channel
 * is initialised and there is nothing left to inject afterwards.
 */
public final class MinestomViaInjector implements ViaInjector {
    private final Transport transport;

    public MinestomViaInjector(final Transport transport) {
        this.transport = transport;
    }

    @Override
    public void inject() {
    }

    @Override
    public void uninject() {
    }

    @Override
    public ProtocolVersion getServerProtocolVersion() {
        return ProtocolVersion.getProtocol(MinecraftServer.PROTOCOL_VERSION);
    }

    @Override
    public String getEncoderName() {
        return HandlerNames.VIA_ENCODER;
    }

    @Override
    public String getDecoderName() {
        return HandlerNames.VIA_DECODER;
    }

    @Override
    public JsonObject getDump() {
        final JsonObject dump = new JsonObject();
        dump.addProperty("transport", transport.name());
        return dump;
    }
}
