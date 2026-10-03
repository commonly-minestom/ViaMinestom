package com.viaversion.minestom.platform;

import com.viaversion.viaversion.configuration.AbstractViaConfig;
import java.io.File;
import java.util.logging.Logger;

public final class MinestomViaConfig extends AbstractViaConfig {

    public MinestomViaConfig(final File dataFolder, final Logger logger) {
        super(new File(dataFolder, "config.yml"), logger);
    }
}
