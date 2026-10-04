package com.viaversion.minestom.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

@FunctionalInterface
public interface CommandAuthorizer {
    int OPERATOR_LEVEL = 4;

    boolean isAuthorized(CommandSender sender, String permission);

    static CommandAuthorizer operators() {
        return (sender, permission) -> !(sender instanceof Player player) || player.getPermissionLevel() >= OPERATOR_LEVEL;
    }
}
