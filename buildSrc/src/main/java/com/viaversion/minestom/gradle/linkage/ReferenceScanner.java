package com.viaversion.minestom.gradle.linkage;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

final class ReferenceScanner {
    private static final String LAMBDA_FACTORY = "java/lang/invoke/LambdaMetafactory";

    private ReferenceScanner() {
    }

    static void scan(final ClassReader reader, final ReferenceSink sink) {
        final Remapper types = new Remapper(Opcodes.ASM9) {

            @Override
            public String map(final String internalName) {
                sink.type(internalName);
                return internalName;
            }
        };
        reader.accept(new ClassRemapper(new MemberScanner(sink), types), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private static final class MemberScanner extends ClassVisitor {
        private final ReferenceSink sink;

        private MemberScanner(final ReferenceSink sink) {
            super(Opcodes.ASM9);
            this.sink = sink;
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
            return new InstructionScanner(sink);
        }
    }

    private static final class InstructionScanner extends MethodVisitor {
        private final ReferenceSink sink;

        private InstructionScanner(final ReferenceSink sink) {
            super(Opcodes.ASM9);
            this.sink = sink;
        }

        @Override
        public void visitTypeInsn(final int opcode, final String type) {
            if (opcode == Opcodes.NEW) {
                sink.instantiation(type);
            }
        }

        @Override
        public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
            final boolean expectStatic = opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC;
            sink.member(new Member(owner, name, descriptor), expectStatic, false);
        }

        @Override
        public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
            sink.member(new Member(owner, name, descriptor), opcode == Opcodes.INVOKESTATIC, isInterface);
        }

        @Override
        public void visitLdcInsn(final Object value) {
            if (value instanceof Handle handle) {
                handle(handle);
            }
        }

        @Override
        public void visitInvokeDynamicInsn(final String name, final String descriptor, final Handle bootstrap, final Object... arguments) {
            for (final Object argument : arguments) {
                if (argument instanceof Handle handle) {
                    handle(handle);
                }
            }
            if (LAMBDA_FACTORY.equals(bootstrap.getOwner()) && arguments.length > 0 && arguments[0] instanceof Type erased) {
                sink.member(new Member(Type.getReturnType(descriptor).getInternalName(), name, erased.getDescriptor()), false, true);
            }
        }

        private void handle(final Handle handle) {
            final int tag = handle.getTag();
            if (tag == Opcodes.H_NEWINVOKESPECIAL) {
                sink.instantiation(handle.getOwner());
            }
            final boolean expectStatic = tag == Opcodes.H_GETSTATIC || tag == Opcodes.H_PUTSTATIC || tag == Opcodes.H_INVOKESTATIC;
            sink.member(new Member(handle.getOwner(), handle.getName(), handle.getDesc()), expectStatic, handle.isInterface());
        }
    }
}
