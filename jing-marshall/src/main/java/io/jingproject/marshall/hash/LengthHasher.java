package io.jingproject.marshall.hash;

import java.lang.foreign.MemorySegment;
import java.util.Objects;

public final class LengthHasher implements Hasher {

    @Override
    public int hash(byte[] bytes, int from, int to) {
        Objects.checkFromToIndex(from, to, bytes.length);
        return to - from;
    }

    @Override
    public int hash(MemorySegment segment, long from, long to) {
        Objects.checkFromToIndex(from, to, segment.byteSize());
        return Long.hashCode(to - from);
    }

}
