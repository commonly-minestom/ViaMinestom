package com.viaversion.minestom.network.connection;

import net.minestom.server.network.packet.server.SendablePacket;

sealed interface ActorMessage permits InboundFrame, ActorMessage.OutboundPacket, ActorMessage.Task {

    record OutboundPacket(SendablePacket packet) implements ActorMessage {
    }

    record Task(Runnable runnable) implements ActorMessage {
    }
}
