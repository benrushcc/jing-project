package io.jingproject.marshall.hash;

import java.lang.foreign.MemorySegment;

public final class LengthHasher implements Hasher {

    @Override
    public int hash(byte[] bytes, int from, int to) {
        if (from < 0 || from > to || to > bytes.length) {
            throw new IndexOutOfBoundsException("range [" + from + ", " + to + ") out of bounds for length : " + bytes.length);
        }
        return to - from;
    }

    @Override
    public int hash(MemorySegment segment, long from, long to) {
        if (from < 0 || from > to || to > segment.byteSize()) {
            throw new IndexOutOfBoundsException("range [" + from + ", " + to + ") out of bounds for length : " + segment.byteSize());
        }
        return Long.hashCode(to - from);
    }

}
