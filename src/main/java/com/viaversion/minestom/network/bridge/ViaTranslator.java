package com.viaversion.minestom.network.bridge;

import com.viaversion.minestom.transport.buffer.ByteBuf;
import com.viaversion.minestom.transport.buffer.ByteBufAllocator;
import com.viaversion.minestom.transport.handler.codec.CodecException;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.exception.CancelCodecException;
import com.viaversion.viaversion.exception.CancelDecoderException;
import com.viaversion.viaversion.exception.CancelEncoderException;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.Registries;
import org.jetbrains.annotations.Nullable;

public final class ViaTranslator {
    private final UserConnection user;
    private final ByteBufAllocator allocator;
    private final @Nullable Registries registries;

    ViaTranslator(final UserConnection user, final ByteBufAllocator allocator, final @Nullable Registries registries) {
        this.user = user;
        this.allocator = allocator;
        this.registries = registries;
    }

    public UserConnection user() {
        return user;
    }

    public boolean active() {
        return user.isActive();
    }

    public @Nullable NetworkBuffer serverbound(final NetworkBuffer packet) {
        if (!user.checkIncomingPacket(Math.toIntExact(packet.readableBytes()))) {
            return null;
        }
        if (!user.shouldTransformPacket()) {
            return packet;
        }
        return transform(packet, true);
    }

    public @Nullable NetworkBuffer clientbound(final NetworkBuffer packet) {
        if (!user.checkOutgoingPacket()) {
            return null;
        }
        if (!user.shouldTransformPacket()) {
            return packet;
        }
        return transform(packet, false);
    }

    public boolean serverbound(final ByteBuf packet) {
        if (!user.checkIncomingPacket(packet.readableBytes())) {
            return false;
        }
        return !user.shouldTransformPacket() || transform(packet, true);
    }

    public boolean clientbound(final ByteBuf packet) {
        if (!user.checkOutgoingPacket()) {
            return false;
        }
        return !user.shouldTransformPacket() || transform(packet, false);
    }

    private @Nullable NetworkBuffer transform(final NetworkBuffer packet, final boolean serverbound) {
        final ByteBuf buf = ByteBufs.copy(allocator, packet);
        try {
            return transform(buf, serverbound) ? ByteBufs.view(buf, registries) : null;
        } finally {
            buf.release();
        }
    }

    private boolean transform(final ByteBuf buf, final boolean serverbound) {
        try {
            if (serverbound) {
                user.transformIncoming(buf, CancelDecoderException::generate);
            } else {
                user.transformOutgoing(buf, CancelEncoderException::generate);
            }
            return true;
        } catch (final CodecException e) {
            if (e instanceof CancelCodecException) {
                return false;
            }
            throw e;
        }
    }
}
