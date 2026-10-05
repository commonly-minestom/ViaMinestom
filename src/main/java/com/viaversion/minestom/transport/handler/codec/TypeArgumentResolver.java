package com.viaversion.minestom.transport.handler.codec;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

final class TypeArgumentResolver {

    private TypeArgumentResolver() {
    }

    static Class<?> resolve(final Class<?> type, final Class<?> declaration, final int index) {
        Class<?> subtype = type;
        while (subtype != null && subtype.getSuperclass() != declaration) {
            subtype = subtype.getSuperclass();
        }
        if (subtype == null || !(subtype.getGenericSuperclass() instanceof ParameterizedType parameterized)) {
            return erase(declaration.getTypeParameters()[index]);
        }

        final Type argument = parameterized.getActualTypeArguments()[index];
        if (!(argument instanceof TypeVariable<?> variable)) {
            return erase(argument);
        }
        final TypeVariable<?>[] parameters = subtype.getTypeParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].equals(variable)) {
                return resolve(type, subtype, i);
            }
        }
        return erase(variable);
    }

    private static Class<?> erase(final Type type) {
        return switch (type) {
            case Class<?> resolved -> resolved;
            case ParameterizedType parameterized -> erase(parameterized.getRawType());
            case TypeVariable<?> variable -> erase(variable.getBounds()[0]);
            default -> Object.class;
        };
    }
}
