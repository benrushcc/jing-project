package io.jingproject.marshall.hash;

import io.jingproject.common.Os;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;

/**
 * takes the first two and last two bytes of the input to form a 4-byte int as the hash value.
 */
public final class FourByteHasher implements Hasher {

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public int hash(byte[] bytes, int from, int to) {
        if (from < 0 || from > to || to > bytes.length) {
            throw new IndexOutOfBoundsException("range [" + from + ", " + to + ") out of bounds for length : " + bytes.length);
        }
        int len = to - from;
        int i1 = bytes[from] & 0xFF;
        int i2 = len > 1 ? (bytes[from + 1] & 0xFF) : 0;
        int i3 = len > 2 ? (bytes[to - 1] & 0xFF) : 0;
        int i4 = len > 3 ? (bytes[to - 2] & 0xFF) : 0;
        return (i4 << 24) | (i3 << 16) | (i2 << 8) | i1;
    }

    @Override
    public int hash(MemorySegment segment, long from, long to) {
        if (from < 0 || from > to || to > segment.byteSize()) {
            throw new IndexOutOfBoundsException("range [" + from + ", " + to + ") out of bounds for length : " + segment.byteSize());
        }
        long len = to - from;
        int i1 = segment.get(ValueLayout.JAVA_BYTE, from) & 0xFF;
        int i2 = len > 1L ? (segment.get(ValueLayout.JAVA_BYTE, from + 1L) & 0xFF) : 0;
        int i3 = len > 2L ? (segment.get(ValueLayout.JAVA_BYTE, to - 1L) & 0xFF) : 0;
        int i4 = len > 3L ? (segment.get(ValueLayout.JAVA_BYTE, to - 2L) & 0xFF) : 0;
        return (i4 << 24) | (i3 << 16) | (i2 << 8) | i1;
    }

}
