package com.viaversion.minestom.gradle.linkage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class ClassIndex {
    private final Map<String, ClassShape> shapes = new HashMap<>();

    void add(final byte[] content) {
        final ShapeReader reader = new ShapeReader();
        new ClassReader(content).accept(reader, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        shapes.put(reader.name, new ClassShape(reader.name, reader.access, reader.superName, reader.interfaces, reader.fields, reader.methods));
    }

    @Nullable ClassShape find(final String name) {
        return shapes.get(name);
    }

    private static final class ShapeReader extends ClassVisitor {
        private final Map<String, Integer> fields = new HashMap<>();
        private final Map<String, Integer> methods = new HashMap<>();
        private String name;
        private int access;
        private @Nullable String superName;
        private List<String> interfaces;

        private ShapeReader() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
            this.name = name;
            this.access = access;
            this.superName = superName;
            this.interfaces = interfaces == null ? List.of() : List.of(interfaces);
        }

        @Override
        public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
            fields.put(Member.signature(name, descriptor), access);
            return null;
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
            methods.put(Member.signature(name, descriptor), access);
            return null;
        }
    }
}
