package com.viaversion.minestom.provider;

import com.viaversion.minestom.network.connection.ViaPlayerConnection;
import com.viaversion.viarewind.protocol.v1_9to1_8.provider.InventoryProvider;
import com.viaversion.viaversion.api.connection.UserConnection;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;

public final class MinestomInventoryProvider extends InventoryProvider {

    @Override
    public boolean hasElytra(final UserConnection connection) {
        final Player player = ViaPlayerConnection.player(connection);
        return player != null && player.getChestplate().material() == Material.ELYTRA;
    }
}
