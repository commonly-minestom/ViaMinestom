package com.viaversion.minestom.gradle.linkage;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.jetbrains.annotations.Nullable;

final class InheritanceVerifier {
    private static final String INITIALIZER_PREFIX = "<";

    private final TypeGraph types;
    private final Violations violations;

    InheritanceVerifier(final TypeGraph types, final Violations violations) {
        this.types = types;
        this.violations = violations;
    }

    void verify(final ClassShape subject) {
        if (!types.reachesNamespace(subject.name())) {
            return;
        }
        verifySupertypes(subject);
        verifyOverrides(subject);
        if (!subject.isAbstract()) {
            verifyImplementations(subject);
        }
    }

    private void verifySupertypes(final ClassShape subject) {
        final ClassShape parent = declared(subject.superName());
        if (parent != null && (parent.isInterface() || parent.isFinal())) {
            violations.add("class " + parent.name() + " cannot be extended", subject.name());
        }
        for (final String name : subject.interfaces()) {
            final ClassShape implemented = declared(name);
            if (implemented != null && !implemented.isInterface()) {
                violations.add("class " + name + " is not an interface", subject.name());
            }
        }
    }

    private void verifyOverrides(final ClassShape subject) {
        if (subject.superName() == null) {
            return;
        }
        for (final Map.Entry<String, Integer> method : subject.methods().entrySet()) {
            if (!inheritable(method.getKey(), method.getValue())) {
                continue;
            }
            final Member overridden = Member.method(subject.superName(), method.getKey());
            final Declaration declaration = types.resolve(overridden);
            if (declaration != null && !declaration.isExternal() && types.inNamespace(declaration.owner().name())
                && Modifier.isFinal(declaration.access()) && inheritable(method.getKey(), declaration.access())) {
                violations.add("method " + declaration.owner().name() + '.' + method.getKey() + " is final", subject.name());
            }
        }
    }

    private void verifyImplementations(final ClassShape subject) {
        final Map<String, String> required = new TreeMap<>();
        final Set<String> implemented = new HashSet<>();
        if (!collect(subject.name(), required, implemented, new HashSet<>())) {
            return;
        }
        for (final Map.Entry<String, String> method : required.entrySet()) {
            if (!implemented.contains(method.getKey())) {
                violations.add("method " + method.getValue() + '.' + method.getKey() + " is not implemented", subject.name());
            }
        }
    }

    private boolean collect(final String type, final Map<String, String> required, final Set<String> implemented, final Set<String> visited) {
        if (!visited.add(type)) {
            return true;
        }
        final ClassShape shape = types.find(type);
        if (shape == null) {
            return PlatformClasses.exists(type);
        }
        for (final Map.Entry<String, Integer> method : shape.methods().entrySet()) {
            if (!inheritable(method.getKey(), method.getValue())) {
                continue;
            }
            if (!Modifier.isAbstract(method.getValue())) {
                implemented.add(method.getKey());
            } else if (types.inNamespace(type)) {
                required.putIfAbsent(method.getKey(), type);
            }
        }

        boolean complete = shape.superName() == null || collect(shape.superName(), required, implemented, visited);
        for (final String parent : shape.interfaces()) {
            complete &= collect(parent, required, implemented, visited);
        }
        return complete;
    }

    private @Nullable ClassShape declared(final @Nullable String type) {
        return type != null && types.inNamespace(type) ? types.find(type) : null;
    }

    private static boolean inheritable(final String signature, final int access) {
        return !signature.startsWith(INITIALIZER_PREFIX) && !Modifier.isStatic(access) && !Modifier.isPrivate(access);
    }
}
