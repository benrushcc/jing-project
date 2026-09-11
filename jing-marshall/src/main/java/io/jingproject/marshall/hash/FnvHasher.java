package io.jingproject.marshall.hash;

import io.jingproject.common.ArrayAccess;
import io.jingproject.common.Os;
import io.jingproject.common.SegmentAccess;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;

/**
 * using FNV-1a hash implementation optimized with 8-byte word processing.
 * Processes bytes in 8-byte chunks for better performance while maintaining
 * the low collision rate of the original FNV algorithm.
 */
public final class FnvHasher implements Hasher {
    private static final long FNV_OFFSET_BASIS = 0x6C62272E07BB0142L;
    private static final long FNV_PRIME = 0x100000001B3L;

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
        long hash = FNV_OFFSET_BASIS;
        for (; from <= to - 8; from += 8) {
            hash ^= ArrayAccess.getLong(bytes, from, ByteOrder.BIG_ENDIAN);
            hash *= FNV_PRIME;
        }
        if (from <= to - 4) {
            hash ^= ArrayAccess.getInt(bytes, from, ByteOrder.BIG_ENDIAN);
            hash *= FNV_PRIME;
            from += 4;
        }
        for (; from < to; from++) {
            hash ^= bytes[from];
            hash *= FNV_PRIME;
        }
        return Long.hashCode(hash);
    }

    @Override
    public int hash(MemorySegment segment, long from, long to) {
        if (from < 0 || from > to || to > segment.byteSize()) {
            throw new IndexOutOfBoundsException("range [" + from + ", " + to + ") out of bounds for length : " + segment.byteSize());
        }
        long hash = FNV_OFFSET_BASIS;
        for (; from <= to - 8L; from += 8L) {
            hash ^= SegmentAccess.getLong(segment, from, ByteOrder.BIG_ENDIAN);
            hash *= FNV_PRIME;
        }
        if (from <= to - 4L) {
            hash ^= SegmentAccess.getInt(segment, from, ByteOrder.BIG_ENDIAN);
            hash *= FNV_PRIME;
            from += 4L;
        }
        for (; from < to; from++) {
            hash ^= segment.get(ValueLayout.JAVA_BYTE, from);
            hash *= FNV_PRIME;
        }
        return Long.hashCode(hash);
    }
}
