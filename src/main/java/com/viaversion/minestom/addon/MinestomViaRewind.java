package com.viaversion.minestom.addon;

import com.viaversion.minestom.network.pipeline.HandlerNames;
import com.viaversion.minestom.provider.MinestomInventoryProvider;
import com.viaversion.minestom.util.Slf4jLoggerAdapter;
import com.viaversion.viarewind.api.ViaRewindPlatform;
import com.viaversion.viarewind.protocol.v1_9to1_8.provider.InventoryProvider;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaManager;
import java.io.File;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

public final class MinestomViaRewind implements ViaRewindPlatform {
    private final Logger logger = new Slf4jLoggerAdapter(LoggerFactory.getLogger("ViaRewind"));
    private final File dataFolder;

    public MinestomViaRewind(final File dataFolder) {
        this.dataFolder = dataFolder;
    }

    /**
     * Hooks the addon into the Via lifecycle, has to be called before the manager is initialised.
     */
    public void install() {
        final ViaManager manager = Via.getManager();
        manager.addEnableListener(() -> init(new File(dataFolder, "config.yml")));
        manager.addPostEnableListener(() -> manager.getProviders().use(InventoryProvider.class, new MinestomInventoryProvider()));
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    @Override
    public String compressHandlerName() {
        return HandlerNames.COMPRESS;
    }

    @Override
    public String decompressHandlerName() {
        return HandlerNames.DECOMPRESS;
    }
}
