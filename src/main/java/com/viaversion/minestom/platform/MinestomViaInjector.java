package com.viaversion.minestom.platform;

import com.viaversion.minestom.network.bridge.HandlerNames;
import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonObject;
import net.minestom.server.MinecraftServer;

public final class MinestomViaInjector implements ViaInjector {
    private static final String TRANSPORT = "nio-virtual-threads";

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
        dump.addProperty("transport", TRANSPORT);
        return dump;
    }
}
