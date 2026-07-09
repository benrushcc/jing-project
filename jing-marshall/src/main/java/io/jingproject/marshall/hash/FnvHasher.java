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
    private static final int FNV_OFFSET_BASIS = 0x811C9DC5;
    private static final int FNV_PRIME = 0x01000193;

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public int hash(byte[] bytes, int offset, int len) {
        assert bytes != null && Objects.checkFromIndexSize(offset, len, bytes.length) >= 0;
        int hash = FNV_OFFSET_BASIS;
        int index = 0;
        for (; index <= len - 8; index += 8) {
            long v = ArrayAccess.getLong(bytes, offset + index, ByteOrder.BIG_ENDIAN);
            hash ^= (int) (v >>> 32);
            hash *= FNV_PRIME;
            hash ^= (int) (v);
            hash *= FNV_PRIME;
        }
        if(index <= len - 4) {
            hash ^= ArrayAccess.getInt(bytes, offset + index, ByteOrder.BIG_ENDIAN);
            hash *= FNV_PRIME;
            index += 4;
        }
        for (; index < len; index++) {
            hash ^= Byte.toUnsignedInt(bytes[offset + index]);
            hash *= FNV_PRIME;
        }
        return hash;
    }

    @Override
    public int hash(MemorySegment segment, long offset, long len) {
        assert segment != null && Objects.checkFromIndexSize(offset, len, segment.byteSize()) >= 0L;
        int hash = FNV_OFFSET_BASIS;
        long index = 0L;
        for (; index <= len - 8L; index += 8L) {
            long v = SegmentAccess.getLong(segment, offset + index, ByteOrder.BIG_ENDIAN);
            hash ^= (int) (v >>> 32);
            hash *= FNV_PRIME;
            hash ^= (int) (v);
            hash *= FNV_PRIME;
        }
        if(index <= len - 4L) {
            hash ^= SegmentAccess.getInt(segment, offset + index, ByteOrder.BIG_ENDIAN);
            hash *= FNV_PRIME;
            index += 4L;
        }
        for (; index < len; index++) {
            hash ^= Byte.toUnsignedInt(segment.get(ValueLayout.JAVA_BYTE, offset + index));
            hash *= FNV_PRIME;
        }
        return hash;
    }
}
