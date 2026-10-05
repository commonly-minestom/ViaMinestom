package com.viaversion.minestom.gradle.linkage;

import org.jetbrains.annotations.Nullable;

record Declaration(@Nullable ClassShape owner, int access) {
    static final Declaration EXTERNAL = new Declaration(null, 0);

    boolean isExternal() {
        return owner == null;
    }
}
