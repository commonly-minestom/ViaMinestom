package com.viaversion.minestom.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

/**
 * Decides whether a sender holds one of the permission nodes checked by the Via commands.
 * Minestom ships no permission system, so servers plug their own in here.
 */
@FunctionalInterface
public interface CommandAuthorizer {
    int OPERATOR_LEVEL = 4;

    boolean isAuthorized(CommandSender sender, String permission);

    /**
     * Grants every permission to the console and to players with operator level.
     */
    static CommandAuthorizer operators() {
        return (sender, permission) -> !(sender instanceof Player player) || player.getPermissionLevel() >= OPERATOR_LEVEL;
    }
}
