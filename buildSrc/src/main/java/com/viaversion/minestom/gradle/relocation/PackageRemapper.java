package com.viaversion.minestom.gradle.relocation;

import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;

final class PackageRemapper extends Remapper {
    private final String[] sources;
    private final String[] targets;

    PackageRemapper(final Map<String, String> packages) {
        super(Opcodes.ASM9);
        this.sources = new String[packages.size()];
        this.targets = new String[packages.size()];

        int index = 0;
        for (final Map.Entry<String, String> relocation : packages.entrySet()) {
            sources[index] = toPrefix(relocation.getKey());
            targets[index] = toPrefix(relocation.getValue());
            index++;
        }
    }

    @Override
    public String map(final String internalName) {
        for (int i = 0; i < sources.length; i++) {
            if (internalName.startsWith(sources[i])) {
                return targets[i] + internalName.substring(sources[i].length());
            }
        }
        return internalName;
    }

    private static String toPrefix(final String packageName) {
        return packageName.replace('.', '/') + '/';
    }
}
