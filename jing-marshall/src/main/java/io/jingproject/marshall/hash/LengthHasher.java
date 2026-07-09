package io.jingproject.marshall.hash;

import java.lang.foreign.MemorySegment;
import java.util.Objects;

public final class LengthHasher implements Hasher {

    @Override
    public int hash(byte[] bytes, int offset, int len) {
        assert bytes != null && Objects.checkFromIndexSize(offset, len, bytes.length) >= 0;
        return len;
    }

    @Override
    public int hash(MemorySegment segment, long offset, long len) {
        assert segment != null && Objects.checkFromIndexSize(offset, len, segment.byteSize()) >= 0L;
        return Math.toIntExact(len);
    }

}
