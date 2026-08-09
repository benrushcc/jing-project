package io.jingproject.marshall.hash;

import io.jingproject.common.ArrayAccess;
import io.jingproject.common.Os;
import io.jingproject.common.SegmentAccess;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Objects;

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
    public int hash(byte[] bytes, int offset, int len) {
        Objects.checkFromIndexSize(offset, len, bytes.length);
        long hash = FNV_OFFSET_BASIS;
        int index = 0;
        for (; index <= len - 8; index += 8) {
            hash ^= ArrayAccess.getLong(bytes, offset + index, ByteOrder.BIG_ENDIAN);
            hash *= FNV_PRIME;
        }
        if (index <= len - 4) {
            hash ^= Integer.toUnsignedLong(ArrayAccess.getInt(bytes, offset + index, ByteOrder.BIG_ENDIAN));
            hash *= FNV_PRIME;
            index += 4;
        }
        for (; index < len; index++) {
            hash ^= Byte.toUnsignedInt(bytes[offset + index]);
            hash *= FNV_PRIME;
        }
        return Long.hashCode(hash);
    }

    @Override
    public int hash(MemorySegment segment, long offset, long len) {
        Objects.checkFromIndexSize(offset, len, segment.byteSize());
        long hash = FNV_OFFSET_BASIS;
        long index = 0L;
        for (; index <= len - 8L; index += 8L) {
            hash ^= SegmentAccess.getLong(segment, offset + index, ByteOrder.BIG_ENDIAN);
            hash *= FNV_PRIME;
        }
        if (index <= len - 4L) {
            hash ^= Integer.toUnsignedLong(SegmentAccess.getInt(segment, offset + index, ByteOrder.BIG_ENDIAN));
            hash *= FNV_PRIME;
            index += 4L;
        }
        for (; index < len; index++) {
            hash ^= Byte.toUnsignedInt(segment.get(ValueLayout.JAVA_BYTE, offset + index));
            hash *= FNV_PRIME;
        }
        return Long.hashCode(hash);
    }
}
