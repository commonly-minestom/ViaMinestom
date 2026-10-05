package com.viaversion.minestom.gradle.linkage;

import java.lang.reflect.Modifier;
import org.objectweb.asm.ClassReader;

final class ReferenceVerifier implements ReferenceSink {
    private static final String ARRAY_PREFIX = "[";

    private final TypeGraph types;
    private final Violations violations;
    private String referrer;
    private int verified;

    ReferenceVerifier(final TypeGraph types, final Violations violations) {
        this.types = types;
        this.violations = violations;
    }

    void verify(final ClassReader reader) {
        referrer = reader.getClassName();
        ReferenceScanner.scan(reader, this);
    }

    int verified() {
        return verified;
    }

    @Override
    public void type(final String name) {
        if (!types.inNamespace(name)) {
            return;
        }
        verified++;
        if (types.find(name) == null) {
            violations.add("class " + name + " does not exist", referrer);
        }
    }

    @Override
    public void instantiation(final String name) {
        final ClassShape shape = types.inNamespace(name) ? types.find(name) : null;
        if (shape != null && shape.isAbstract()) {
            violations.add("class " + name + " cannot be instantiated", referrer);
        }
    }

    @Override
    public void member(final Member member, final boolean expectStatic, final boolean expectInterface) {
        final String owner = member.owner();
        if (owner.startsWith(ARRAY_PREFIX) || !types.reachesNamespace(owner)) {
            return;
        }
        verified++;

        final ClassShape shape = types.find(owner);
        if (shape == null) {
            return;
        }
        if (!member.isField() && types.inNamespace(owner) && shape.isInterface() != expectInterface) {
            violations.add("class " + owner + (expectInterface ? " is not an interface" : " is an interface"), referrer);
            return;
        }

        final Declaration declaration = types.resolve(member);
        if (declaration == null) {
            violations.add(member + " does not exist", referrer);
        } else if (!declaration.isExternal() && types.inNamespace(declaration.owner().name())) {
            verifyUse(member, declaration, expectStatic);
        }
    }

    private void verifyUse(final Member member, final Declaration declaration, final boolean expectStatic) {
        final String declaringType = declaration.owner().name();
        final Member declared = new Member(declaringType, member.name(), member.descriptor());
        final int access = declaration.access();
        if (Modifier.isStatic(access) != expectStatic) {
            violations.add(declared + (expectStatic ? " is not static" : " is static"), referrer);
        } else if (!Modifier.isPublic(access) && !(Modifier.isProtected(access) && types.isSubclass(referrer, declaringType))) {
            violations.add(declared + " is not accessible", referrer);
        }
    }
}
