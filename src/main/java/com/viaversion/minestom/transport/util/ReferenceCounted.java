package com.viaversion.minestom.transport.util;

public interface ReferenceCounted {

    int refCnt();

    ReferenceCounted retain();

    boolean release();
}
