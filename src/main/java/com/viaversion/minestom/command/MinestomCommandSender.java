package com.viaversion.minestom.command;

import com.viaversion.viaversion.api.command.ViaCommandSender;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

public record MinestomCommandSender(CommandSender sender, CommandAuthorizer authorizer) implements ViaCommandSender {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final UUID CONSOLE_UUID = new UUID(0, 0);

    @Override
    public boolean hasPermission(final String permission) {
        return authorizer.isAuthorized(sender, permission);
    }

    @Override
    public void sendMessage(final String message) {
        sender.sendMessage(LEGACY_SERIALIZER.deserialize(message));
    }

    @Override
    public UUID getUUID() {
        return sender instanceof Player player ? player.getUuid() : CONSOLE_UUID;
    }

    @Override
    public String getName() {
        return sender instanceof Player player ? player.getUsername() : "CONSOLE";
    }
}
