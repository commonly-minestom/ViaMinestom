package com.viaversion.minestom.network.codec;

import java.util.zip.DataFormatException;
import javax.crypto.Cipher;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;

public final class WireCodec {
    private static final int LENGTH_PREFIX_BYTES = 3;
    private static final int UNCOMPRESSED_MARKER_BYTES = 1;
    private static final int DEFLATE_OVERHEAD = 13;

    private int threshold;
    private @Nullable Cipher encrypt;

    public boolean compressed() {
        return threshold > 0;
    }

    public int threshold() {
        return threshold;
    }

    public void enableCompression(final int threshold) {
        this.threshold = threshold;
    }

    public void encrypt(final Cipher cipher) {
        this.encrypt = cipher;
    }

    public NetworkBuffer decode(final NetworkBuffer frame, final int maxSize) throws DataFormatException {
        if (!compressed()) {
            return frame;
        }
        final int size = frame.read(NetworkBuffer.VAR_INT);
        if (size == 0) {
            return frame;
        }
        if (size < 0 || size > maxSize) {
            throw new DataFormatException("Invalid decompressed length: " + size);
        }
        final NetworkBuffer body = NetworkBuffer.wrap(new byte[size], 0, 0, frame.registries());
        final long produced = frame.decompress(frame.readIndex(), frame.readableBytes(), body);
        if (produced != size) {
            throw new DataFormatException("Decompressed length mismatch: expected " + size + ", got " + produced);
        }
        return body;
    }

    public boolean encode(final NetworkBuffer body, final NetworkBuffer out) {
        final int size = Math.toIntExact(body.readableBytes());
        final long start = out.writeIndex();
        if (!compressed()) {
            if (out.writableBytes() < varIntLength(size) + size) {
                return false;
            }
            out.write(NetworkBuffer.VAR_INT, size);
            copy(body, size, out);
        } else if (size < threshold) {
            if (out.writableBytes() < varIntLength(size + UNCOMPRESSED_MARKER_BYTES) + UNCOMPRESSED_MARKER_BYTES + size) {
                return false;
            }
            out.write(NetworkBuffer.VAR_INT, size + UNCOMPRESSED_MARKER_BYTES);
            out.write(NetworkBuffer.VAR_INT, 0);
            copy(body, size, out);
        } else {
            if (out.writableBytes() < LENGTH_PREFIX_BYTES + varIntLength(size) + deflateBound(size)) {
                return false;
            }
            final long lengthIndex = out.advanceWrite(LENGTH_PREFIX_BYTES);
            out.write(NetworkBuffer.VAR_INT, size);
            body.compress(body.readIndex(), size, out);
            out.writeAt(lengthIndex, NetworkBuffer.VAR_INT_3, (int) (out.writeIndex() - lengthIndex - LENGTH_PREFIX_BYTES));
        }
        cipher(out, start);
        return true;
    }

    public boolean copyRaw(final NetworkBuffer frames, final long index, final long length, final NetworkBuffer out) {
        if (out.writableBytes() < length) {
            return false;
        }
        final long start = out.writeIndex();
        NetworkBuffer.copy(frames, index, out, start, length);
        out.advanceWrite(length);
        cipher(out, start);
        return true;
    }

    private void cipher(final NetworkBuffer out, final long start) {
        final Cipher cipher = encrypt;
        final long length = out.writeIndex() - start;
        if (cipher != null && length > 0) {
            out.cipher(cipher, start, length);
        }
    }

    private static void copy(final NetworkBuffer body, final int size, final NetworkBuffer out) {
        NetworkBuffer.copy(body, body.readIndex(), out, out.writeIndex(), size);
        out.advanceWrite(size);
    }

    public static int varIntLength(final int value) {
        return value == 0 ? 1 : (38 - Integer.numberOfLeadingZeros(value)) / 7;
    }

    public static int deflateBound(final int size) {
        return size + (size >>> 12) + (size >>> 14) + (size >>> 25) + DEFLATE_OVERHEAD;
    }
}
