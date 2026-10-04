package com.viaversion.minestom.addon;

import java.nio.file.Path;

@FunctionalInterface
public interface ViaAddon {
    void install(Path dataDirectory);
}
