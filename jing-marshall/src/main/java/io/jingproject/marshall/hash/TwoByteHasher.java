package io.jingproject.marshall.hash;

import io.jingproject.common.ArrayAccess;
import io.jingproject.common.Os;
import io.jingproject.common.SegmentAccess;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;

/**
 * takes the first two bytes as the hash value, or the only byte if the input length is 1.
 */
public final class TwoByteHasher implements Hasher {

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
        if(to - from > 1) {
            return ArrayAccess.getShort(bytes, from, ByteOrder.BIG_ENDIAN);
        }
        return bytes[from];
    }

    @Override
    public int hash(MemorySegment segment, long from, long to) {
        if (from < 0 || from > to || to > segment.byteSize()) {
            throw new IndexOutOfBoundsException("range [" + from + ", " + to + ") out of bounds for length : " + segment.byteSize());
        }
        if(to - from > 1L) {
            return SegmentAccess.getShort(segment, from, ByteOrder.BIG_ENDIAN);
        }
        return SegmentAccess.getByte(segment, from);
    }

}
