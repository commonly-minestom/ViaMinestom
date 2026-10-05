package com.viaversion.minestom.gradle.linkage;

record Member(String owner, String name, String descriptor) {
    private static final char METHOD_DESCRIPTOR_START = '(';
    private static final char FIELD_SEPARATOR = ':';

    static Member method(final String owner, final String signature) {
        final int descriptorStart = signature.indexOf(METHOD_DESCRIPTOR_START);
        return new Member(owner, signature.substring(0, descriptorStart), signature.substring(descriptorStart));
    }

    static String signature(final String name, final String descriptor) {
        return descriptor.charAt(0) == METHOD_DESCRIPTOR_START ? name + descriptor : name + FIELD_SEPARATOR + descriptor;
    }

    boolean isField() {
        return descriptor.charAt(0) != METHOD_DESCRIPTOR_START;
    }

    String signature() {
        return signature(name, descriptor);
    }

    @Override
    public String toString() {
        return (isField() ? "field " : "method ") + owner + '.' + signature();
    }
}
