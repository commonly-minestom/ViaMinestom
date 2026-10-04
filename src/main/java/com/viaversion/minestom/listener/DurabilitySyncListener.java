package com.viaversion.minestom.listener;

import com.viaversion.viabackwards.protocol.v1_11to1_10.Protocol1_11To1_10;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.item.ItemStack;

public final class DurabilitySyncListener extends ProtocolEventListener<PlayerBlockBreakEvent> {
    public DurabilitySyncListener(final EventNode<Event> node) {
        super(node, PlayerBlockBreakEvent.class, Protocol1_11To1_10.class);
    }

    @Override
    protected void handle(final PlayerBlockBreakEvent event) {
        if (!event.isCancelled()) {
            return;
        }

        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || !isOnPipe(player)) {
            return;
        }

        final ItemStack item = player.getItemInMainHand();
        if (item.has(DataComponents.MAX_DAMAGE)) {
            player.getInventory().sendSlotRefresh(player.getHeldSlot(), item);
        }
    }
}
