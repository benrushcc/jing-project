package io.jingproject.marshall.hash;

import io.jingproject.common.ArrayAccess;
import io.jingproject.common.Os;
import io.jingproject.common.SegmentAccess;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

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
    public int hash(byte[] bytes, int offset, int len) {
        assert bytes != null && Objects.checkFromIndexSize(offset, len, bytes.length) >= 0;
        int hash = 0;
        int index = 0;
        for (; index <= len - 4; index += 4) {
            hash += ArrayAccess.getInt(bytes, offset + index);
        }
        for (; index < len; index++) {
            hash += bytes[offset + index];
        }
        return hash;
    }

    @Override
    public int hash(MemorySegment segment, long offset, long len) {
        assert segment != null && Objects.checkFromIndexSize(offset, len, segment.byteSize()) >= 0L;
        int hash = 0;
        long index = 0;
        for (; index <= len - 4L; index += 4L) {
            hash += SegmentAccess.getInt(segment, offset + index);
        }
        for (; index < len; index++) {
            hash += SegmentAccess.getByte(segment, offset + index);
        }
        return hash;
    }

}
