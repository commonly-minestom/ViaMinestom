package com.viaversion.minestom.listener.impl;

import com.viaversion.minestom.listener.ProtocolEventListener;
import com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.Protocol1_21_11To1_21_9;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;

public final class SpearAttackListener extends ProtocolEventListener<PlayerBlockBreakEvent> {
    public SpearAttackListener(final EventNode<Event> node) {
        super(node, PlayerBlockBreakEvent.class, Protocol1_21_11To1_21_9.class);
    }

    @Override
    protected void handle(final PlayerBlockBreakEvent event) {
        final Player player = event.getPlayer();
        if (!isOnPipe(player)) {
            return;
        }

        if (player.getItemInMainHand().material().key().value().endsWith("_spear")) {
            event.setCancelled(true);
        }
    }
}
