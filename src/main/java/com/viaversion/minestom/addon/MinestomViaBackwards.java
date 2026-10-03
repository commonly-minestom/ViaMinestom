package com.viaversion.minestom.addon;

import com.viaversion.minestom.listener.DurabilitySyncListener;
import com.viaversion.minestom.listener.FireExtinguishListener;
import com.viaversion.minestom.listener.LecternInteractListener;
import com.viaversion.minestom.listener.SpearAttackListener;
import com.viaversion.minestom.provider.MinestomAdvancementCriteriaProvider;
import com.viaversion.minestom.util.Slf4jLoggerAdapter;
import com.viaversion.viabackwards.api.ViaBackwardsPlatform;
import com.viaversion.viabackwards.protocol.v1_20_2to1_20.provider.AdvancementCriteriaProvider;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaManager;
import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import org.slf4j.LoggerFactory;

public final class MinestomViaBackwards implements ViaBackwardsPlatform, ViaAddon {
    private final Logger logger = new Slf4jLoggerAdapter(LoggerFactory.getLogger("ViaBackwards"));
    private final EventNode<Event> eventNode = EventNode.all("viabackwards");
    private File dataFolder;

    @Override
    public void install(final Path dataDirectory) {
        this.dataFolder = dataDirectory.resolve("viabackwards").toFile();

        final ViaManager manager = Via.getManager();
        manager.addEnableListener(() -> init(new File(dataFolder, "config.yml")));
        manager.addPostEnableListener(this::enable);
    }

    @Override
    public void enable() {
        ViaBackwardsPlatform.super.enable();

        new SpearAttackListener(eventNode).register();
        new FireExtinguishListener(eventNode).register();
        new LecternInteractListener(eventNode).register();
        new DurabilitySyncListener(eventNode).register();
        // Run after the listeners of the server so that their cancellations are visible
        eventNode.setPriority(Integer.MAX_VALUE);
        MinecraftServer.getGlobalEventHandler().addChild(eventNode);

        Via.getManager().getProviders().use(AdvancementCriteriaProvider.class, new MinestomAdvancementCriteriaProvider());
    }

    @Override
    public void disable() {
        MinecraftServer.getGlobalEventHandler().removeChild(eventNode);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }
}
