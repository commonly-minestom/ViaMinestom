package com.viaversion.minestom.gradle.linkage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

final class TypeGraph {
    private final ClassIndex index;
    private final String namespace;
    private final Map<String, Boolean> reach = new HashMap<>();

    TypeGraph(final ClassIndex index, final String namespace) {
        this.index = index;
        this.namespace = namespace;
    }

    @Nullable ClassShape find(final String type) {
        return index.find(type);
    }

    boolean inNamespace(final String type) {
        return type.startsWith(namespace);
    }

    boolean reachesNamespace(final String type) {
        Boolean known = reach.get(type);
        if (known == null) {
            known = reachesNamespace(type, new HashSet<>());
            reach.put(type, known);
        }
        return known;
    }

    boolean isSubclass(final String type, final String ancestor) {
        ClassShape shape = index.find(type);
        while (shape != null && shape.superName() != null) {
            if (shape.superName().equals(ancestor)) {
                return true;
            }
            shape = index.find(shape.superName());
        }
        return false;
    }

    @Nullable Declaration resolve(final Member member) {
        return resolve(member.owner(), member, new HashSet<>());
    }

    private boolean reachesNamespace(final String type, final Set<String> visited) {
        if (inNamespace(type)) {
            return true;
        }
        final ClassShape shape = index.find(type);
        if (shape == null || !visited.add(type)) {
            return false;
        }
        if (shape.superName() != null && reachesNamespace(shape.superName(), visited)) {
            return true;
        }
        for (final String parent : shape.interfaces()) {
            if (reachesNamespace(parent, visited)) {
                return true;
            }
        }
        return false;
    }

    private @Nullable Declaration resolve(final String type, final Member member, final Set<String> visited) {
        if (!visited.add(type)) {
            return null;
        }
        final ClassShape shape = index.find(type);
        if (shape == null) {
            return PlatformClasses.lacks(type, member) ? null : Declaration.EXTERNAL;
        }
        final Integer access = shape.declared(member);
        if (access != null) {
            return new Declaration(shape, access);
        }

        Declaration declaration = shape.superName() == null ? null : resolve(shape.superName(), member, visited);
        for (final String parent : shape.interfaces()) {
            if (declaration != null && !declaration.isExternal()) {
                break;
            }
            final Declaration inherited = resolve(parent, member, visited);
            if (inherited != null) {
                declaration = inherited;
            }
        }
        return declaration;
    }
}
