package com.viaversion.minestom.gradle.linkage;

import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.jetbrains.annotations.Nullable;

final class PlatformClasses {
    private static final String CONSTRUCTOR_NAME = "<init>";

    private PlatformClasses() {
    }

    static boolean exists(final String type) {
        return load(type) != null;
    }

    static boolean lacks(final String type, final Member member) {
        final Class<?> platform = load(type);
        return platform != null && !inherits(platform, member);
    }

    private static @Nullable Class<?> load(final String type) {
        try {
            return Class.forName(type.replace('/', '.'), false, ClassLoader.getPlatformClassLoader());
        } catch (final ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    private static boolean inherits(final Class<?> type, final Member member) {
        if (declares(type, member)) {
            return true;
        }
        final Class<?> parent = type.getSuperclass();
        if (parent != null && inherits(parent, member)) {
            return true;
        }
        for (final Class<?> implemented : type.getInterfaces()) {
            if (inherits(implemented, member)) {
                return true;
            }
        }
        return false;
    }

    private static boolean declares(final Class<?> type, final Member member) {
        if (member.isField()) {
            return declaresField(type, member);
        }
        if (member.name().equals(CONSTRUCTOR_NAME)) {
            return declaresConstructor(type, member);
        }
        return declaresMethod(type, member);
    }

    private static boolean declaresField(final Class<?> type, final Member member) {
        for (final Field field : type.getDeclaredFields()) {
            if (field.getName().equals(member.name()) && field.getType().descriptorString().equals(member.descriptor())) {
                return true;
            }
        }
        return false;
    }

    private static boolean declaresConstructor(final Class<?> type, final Member member) {
        for (final Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (MethodType.methodType(void.class, constructor.getParameterTypes()).descriptorString().equals(member.descriptor())) {
                return true;
            }
        }
        return false;
    }

    private static boolean declaresMethod(final Class<?> type, final Member member) {
        for (final Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(member.name()) && MethodType.methodType(method.getReturnType(), method.getParameterTypes()).descriptorString().equals(member.descriptor())) {
                return true;
            }
        }
        return false;
    }
}
