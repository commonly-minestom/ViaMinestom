package com.viaversion.minestom.network.pipeline;

import com.viaversion.viaversion.platform.ViaDecodeHandler;
import com.viaversion.viaversion.platform.ViaEncodeHandler;

/**
 * Names of the handlers in a client pipeline, listed in the order they are installed in.
 */
public final class HandlerNames {
    public static final String PROXY_PROTOCOL = "proxy-protocol";
    public static final String READ_TIMEOUT = "read-timeout";
    public static final String CIPHER = "cipher";
    public static final String FRAME_DECODER = "frame-decoder";
    public static final String FRAME_ENCODER = "frame-encoder";
    public static final String DECOMPRESS = "decompress";
    public static final String COMPRESS = "compress";
    public static final String CLIENT_INTERCEPTOR = "client-interceptor";
    public static final String VIA_DECODER = ViaDecodeHandler.NAME;
    public static final String VIA_ENCODER = ViaEncodeHandler.NAME;
    public static final String SERVER_INTERCEPTOR = "server-interceptor";
    public static final String PACKET_HANDLER = "packet-handler";

    private HandlerNames() {
    }
}
