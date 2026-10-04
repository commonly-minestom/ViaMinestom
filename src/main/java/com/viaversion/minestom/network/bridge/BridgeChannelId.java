package com.viaversion.minestom.network.bridge;

import io.netty.channel.ChannelId;

record BridgeChannelId(long value) implements ChannelId {
    private static final String PREFIX = "via-bridge-";

    @Override
    public String asShortText() {
        return Long.toHexString(value);
    }

    @Override
    public String asLongText() {
        return PREFIX + value;
    }

    @Override
    public int compareTo(final ChannelId other) {
        if (other instanceof BridgeChannelId(final long otherValue)) {
            return Long.compare(value, otherValue);
        }
        return asLongText().compareTo(other.asLongText());
    }

    @Override
    public String toString() {
        return asLongText();
    }
}
