package com.viaversion.minestom.listener;

import com.viaversion.viabackwards.protocol.v1_16to1_15_2.Protocol1_16To1_15_2;
import net.minestom.server.coordinate.Point;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerStartDiggingEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

/**
 * Before 1.16 fire has no hitbox, clients put it out by hitting the block it burns on.
 */
public final class FireExtinguishListener extends ProtocolEventListener<PlayerStartDiggingEvent> {

    public FireExtinguishListener(final EventNode<Event> node) {
        super(node, PlayerStartDiggingEvent.class, Protocol1_16To1_15_2.class);
    }

    @Override
    protected void handle(final PlayerStartDiggingEvent event) {
        if (event.isCancelled() || !isOnPipe(event.getPlayer())) {
            return;
        }

        final Instance instance = event.getInstance();
        final Point position = event.getBlockPosition().relative(event.getBlockFace());
        if (instance.getBlock(position).compare(Block.FIRE)) {
            event.setCancelled(true);
            instance.setBlock(position, Block.AIR);
        }
    }
}
