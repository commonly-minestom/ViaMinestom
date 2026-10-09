package com.viaversion.minestom.network.connection;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;

final class PeerDisconnects {
    private static final String CONNECTION_RESET = "Connection reset";
    private static final String CONNECTION_RESET_BY_PEER = "Connection reset by peer";
    private static final String BROKEN_PIPE = "Broken pipe";

    private PeerDisconnects() {
    }

    static boolean isExpected(final IOException e) {
        if (e instanceof ClosedChannelException || e instanceof EOFException) {
            return true;
        }
        if (e instanceof SocketException) {
            final String message = e.getMessage();
            return CONNECTION_RESET.equals(message) || CONNECTION_RESET_BY_PEER.equals(message) || BROKEN_PIPE.equals(message);
        }
        return false;
    }
}
