package io.jingproject.marshalltest.test;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;

import java.lang.foreign.MemorySegment;

public final class RecordEntityMarshallImpl implements MarshallFacade {

    @Override
    public int totalElements() {
        return 4;
    }

    @Override
    public int objectElements() {
        return 2;
    }

    @Override
    public int primitiveElements() {
        return 2;
    }

    @Override
    public int primitiveBytes() {
        return Integer.BYTES + Long.BYTES;
    }

    @Override
    public MarshallInfo elementInfoByIndex(int index) {
        return null;
    }

    @Override
    public MarshallInfo elementInfoByName(String name) {
        return null;
    }

    @Override
    public MarshallInfo elementInfoByUtf8Bytes(byte[] bytes, int offset, int len) {
        return null;
    }

    @Override
    public MarshallInfo elementInfoByUtf8Segment(MemorySegment segment, long offset, long len) {
        return null;
    }
}
