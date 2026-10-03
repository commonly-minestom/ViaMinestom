package com.viaversion.minestom.addon;

import java.nio.file.Path;

/**
 * A project building on ViaVersion that is loaded along with it, such as ViaBackwards or ViaRewind.
 */
@FunctionalInterface
public interface ViaAddon {

    /**
     * Hooks the addon into the Via lifecycle. Called once the Via manager exists and before it is initialised.
     *
     * @param dataDirectory the directory shared by ViaVersion and its addons, each keeping a folder of its own in it
     */
    void install(Path dataDirectory);
}
