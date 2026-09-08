package io.jingproject.marshall.hash;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/**
 * takes the first byte of the input as the hash value.
 */
public final class OneByteHasher implements Hasher {

    @Override
    public int hash(byte[] bytes, int from, int to) {
        Objects.checkFromToIndex(from, to, bytes.length);
        return bytes[from];
    }

    @Override
    public int hash(MemorySegment segment, long from, long to) {
        Objects.checkFromToIndex(from, to, segment.byteSize());
        return segment.get(ValueLayout.JAVA_BYTE, from);
    }

}
