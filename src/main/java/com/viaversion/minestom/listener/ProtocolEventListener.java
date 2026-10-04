package com.viaversion.minestom.listener;

import com.viaversion.viaversion.ViaListener;
import com.viaversion.viaversion.api.protocol.Protocol;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

public abstract class ProtocolEventListener<E extends Event> extends ViaListener {
    private final EventNode<Event> node;
    private final Class<E> eventType;

    @SuppressWarnings("rawtypes")
    protected ProtocolEventListener(final EventNode<Event> node, final Class<E> eventType, final Class<? extends Protocol> requiredPipeline) {
        super(requiredPipeline);
        this.node = node;
        this.eventType = eventType;
    }

    protected abstract void handle(E event);

    protected boolean isOnPipe(final Player player) {
        return isOnPipe(player.getUuid());
    }

    @Override
    public void register() {
        if (isRegistered()) {
            return;
        }

        setRegistered(true);
        node.addListener(eventType, this::handle);
    }
}
