package io.jingproject.marshall.hash;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/**
 * takes the first byte of the input as the hash value.
 */
public final class OneByteHasher implements Hasher {

    @Override
    public int hash(byte[] bytes, int offset, int len) {
        assert bytes != null && Objects.checkFromIndexSize(offset, len, bytes.length) >= 0;
        return bytes[offset];
    }

    @Override
    public int hash(MemorySegment segment, long offset, long len) {
        assert segment != null && Objects.checkFromIndexSize(offset, len, segment.byteSize()) >= 0L;
        return segment.get(ValueLayout.JAVA_BYTE, offset);
    }

}
