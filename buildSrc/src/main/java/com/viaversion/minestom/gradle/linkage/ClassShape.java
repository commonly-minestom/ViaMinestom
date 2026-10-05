package com.viaversion.minestom.gradle.linkage;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

record ClassShape(String name, int access, @Nullable String superName, List<String> interfaces, Map<String, Integer> fields, Map<String, Integer> methods) {

    boolean isInterface() {
        return Modifier.isInterface(access);
    }

    boolean isAbstract() {
        return Modifier.isAbstract(access);
    }

    boolean isFinal() {
        return Modifier.isFinal(access);
    }

    @Nullable Integer declared(final Member member) {
        return (member.isField() ? fields : methods).get(member.signature());
    }
}
