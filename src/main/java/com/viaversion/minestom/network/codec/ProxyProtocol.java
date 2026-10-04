package com.viaversion.minestom.network.codec;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;

public final class ProxyProtocol {
    private static final byte[] V1_SIGNATURE = "PROXY ".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] V2_SIGNATURE = {0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A};
    private static final int V1_MAX_LENGTH = 107;
    private static final int V1_FIELDS = 6;
    private static final int V2_HEADER_LENGTH = 16;
    private static final int V2_VERSION = 2;
    private static final int V2_COMMAND_LOCAL = 0;
    private static final int V2_COMMAND_PROXY = 1;
    private static final int V2_FAMILY_INET = 1;
    private static final int V2_FAMILY_INET6 = 2;
    private static final int INET_LENGTH = 4;
    private static final int INET6_LENGTH = 16;
    private static final int PROBE_LENGTH = 256;
    private static final int MAX_PORT = 65535;

    private ProxyProtocol() {
    }

    public enum Status {
        NEED_MORE,
        ABSENT,
        PRESENT
    }

    public record Header(Status status, @Nullable InetSocketAddress source) {
        private static final Header NEED_MORE = new Header(Status.NEED_MORE, null);
        private static final Header ABSENT = new Header(Status.ABSENT, null);

        private static Header present(final @Nullable InetSocketAddress source) {
            return new Header(Status.PRESENT, source);
        }
    }

    public static Header parse(final NetworkBuffer buffer) throws CorruptedFrameException {
        final int available = (int) Math.min(buffer.readableBytes(), PROBE_LENGTH);
        if (available == 0) {
            return Header.NEED_MORE;
        }
        final byte[] bytes = new byte[available];
        buffer.copyTo(buffer.readIndex(), bytes, 0, available);

        if (startsWith(bytes, V1_SIGNATURE)) {
            return parseV1(buffer, bytes);
        }
        if (startsWith(bytes, V2_SIGNATURE)) {
            return parseV2(buffer, bytes);
        }
        if (isPrefixOf(bytes, V1_SIGNATURE) || isPrefixOf(bytes, V2_SIGNATURE)) {
            return Header.NEED_MORE;
        }
        return Header.ABSENT;
    }

    private static Header parseV1(final NetworkBuffer buffer, final byte[] bytes) throws CorruptedFrameException {
        final int limit = Math.min(bytes.length, V1_MAX_LENGTH);
        int end = -1;
        for (int i = V1_SIGNATURE.length; i < limit; i++) {
            if (bytes[i - 1] == '\r' && bytes[i] == '\n') {
                end = i + 1;
                break;
            }
        }
        if (end < 0) {
            if (bytes.length < V1_MAX_LENGTH) {
                return Header.NEED_MORE;
            }
            throw new CorruptedFrameException("PROXY protocol v1 header exceeds " + V1_MAX_LENGTH + " bytes");
        }

        final String[] fields = new String(bytes, 0, end - 2, StandardCharsets.US_ASCII).split(" ", -1);
        final InetSocketAddress source;
        if (fields.length >= 2 && fields[1].equals("UNKNOWN")) {
            source = null;
        } else if (fields.length == V1_FIELDS && (fields[1].equals("TCP4") || fields[1].equals("TCP6"))) {
            source = parseV1Address(fields[1].equals("TCP4"), fields[2], fields[4]);
        } else {
            throw new CorruptedFrameException("Invalid PROXY protocol v1 header");
        }
        buffer.advanceRead(end);
        return Header.present(source);
    }

    private static InetSocketAddress parseV1Address(final boolean inet, final String host, final String port) throws CorruptedFrameException {
        try {
            final InetAddress address = InetAddress.ofLiteral(host);
            final int portNumber = Integer.parseInt(port);
            if (portNumber < 0 || portNumber > MAX_PORT || inet != (address.getAddress().length == INET_LENGTH)) {
                throw new IllegalArgumentException(host + ":" + port);
            }
            return new InetSocketAddress(address, portNumber);
        } catch (final IllegalArgumentException e) {
            throw new CorruptedFrameException("Invalid PROXY protocol v1 source address", e);
        }
    }

    private static Header parseV2(final NetworkBuffer buffer, final byte[] bytes) throws CorruptedFrameException {
        if (bytes.length < V2_HEADER_LENGTH) {
            return Header.NEED_MORE;
        }
        final int versionAndCommand = bytes[12] & 0xFF;
        final int familyAndProtocol = bytes[13] & 0xFF;
        final int payloadLength = ((bytes[14] & 0xFF) << 8) | (bytes[15] & 0xFF);
        final int total = V2_HEADER_LENGTH + payloadLength;
        if (buffer.readableBytes() < total) {
            return Header.NEED_MORE;
        }
        if (versionAndCommand >>> 4 != V2_VERSION) {
            throw new CorruptedFrameException("Invalid PROXY protocol v2 version");
        }

        final int command = versionAndCommand & 0x0F;
        InetSocketAddress source = null;
        if (command == V2_COMMAND_PROXY) {
            source = switch (familyAndProtocol >>> 4) {
                case V2_FAMILY_INET -> parseV2Address(bytes, payloadLength, INET_LENGTH);
                case V2_FAMILY_INET6 -> parseV2Address(bytes, payloadLength, INET6_LENGTH);
                default -> null;
            };
        } else if (command != V2_COMMAND_LOCAL) {
            throw new CorruptedFrameException("Invalid PROXY protocol v2 command");
        }
        buffer.advanceRead(total);
        return Header.present(source);
    }

    private static InetSocketAddress parseV2Address(final byte[] bytes, final int payloadLength, final int addressLength) throws CorruptedFrameException {
        final int required = addressLength * 2 + Short.BYTES * 2;
        if (payloadLength < required || bytes.length < V2_HEADER_LENGTH + required) {
            throw new CorruptedFrameException("Truncated PROXY protocol v2 address block");
        }
        final int portIndex = V2_HEADER_LENGTH + addressLength * 2;
        final int port = ((bytes[portIndex] & 0xFF) << 8) | (bytes[portIndex + 1] & 0xFF);
        try {
            final InetAddress address = InetAddress.getByAddress(Arrays.copyOfRange(bytes, V2_HEADER_LENGTH, V2_HEADER_LENGTH + addressLength));
            return new InetSocketAddress(address, port);
        } catch (final UnknownHostException e) {
            throw new CorruptedFrameException("Invalid PROXY protocol v2 source address", e);
        }
    }

    private static boolean startsWith(final byte[] bytes, final byte[] signature) {
        return bytes.length >= signature.length && Arrays.equals(bytes, 0, signature.length, signature, 0, signature.length);
    }

    private static boolean isPrefixOf(final byte[] bytes, final byte[] signature) {
        return bytes.length < signature.length && Arrays.equals(bytes, 0, bytes.length, signature, 0, bytes.length);
    }
}
