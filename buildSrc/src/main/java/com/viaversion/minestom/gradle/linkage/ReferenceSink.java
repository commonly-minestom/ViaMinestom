package com.viaversion.minestom.gradle.linkage;

interface ReferenceSink {

    void type(String name);

    void instantiation(String name);

    void member(Member member, boolean expectStatic, boolean expectInterface);
}
