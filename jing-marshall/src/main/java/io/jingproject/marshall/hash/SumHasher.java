package io.jingproject.marshall.hash;

import io.jingproject.common.ArrayAccess;
import io.jingproject.common.Os;
import io.jingproject.common.SegmentAccess;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;

/**
 * sums all bytes of the input as the hash value, reading 4 bytes at a time for performance.
 */
public final class SumHasher implements Hasher {

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
        int hash = 0;
        for (; from <= to - 4; from += 4) {
            hash += ArrayAccess.getInt(bytes, from);
        }
        for (; from < to; from++) {
            hash += bytes[from];
        }
        return hash;
    }

    @Override
    public int hash(MemorySegment segment, long from, long to) {
        if (from < 0 || from > to || to > segment.byteSize()) {
            throw new IndexOutOfBoundsException("range [" + from + ", " + to + ") out of bounds for length : " + segment.byteSize());
        }
        int hash = 0;
        for (; from <= to - 4L; from += 4L) {
            hash += SegmentAccess.getInt(segment, from);
        }
        for (; from < to; from++) {
            hash += SegmentAccess.getByte(segment, from);
        }
        return hash;
    }

}
