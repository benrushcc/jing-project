package io.jingproject.marshall.hash;

import java.lang.foreign.MemorySegment;
import java.util.Objects;

public final class LengthHasher implements Hasher {

    @Override
    public int hash(byte[] bytes, int offset, int len) {
        return len;
    }

    @Override
    public int hash(MemorySegment segment, long offset, long len) {
        return Math.toIntExact(len);
    }

}
