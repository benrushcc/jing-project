package io.jingproject.marshall.hash;

import io.jingproject.common.ArrayAccess;
import io.jingproject.common.Os;
import io.jingproject.common.SegmentAccess;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Objects;

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
    public int hash(MemorySegment segment, long offset, long len) {
        assert segment != null && Objects.checkFromIndexSize(offset, len, segment.byteSize()) >= 0L;
        if(len > 1L) {
            return SegmentAccess.getShort(segment, offset, ByteOrder.BIG_ENDIAN);
        }
        return SegmentAccess.getByte(segment, offset);
    }

    @Override
    public int hash(byte[] bytes, int offset, int len) {
        assert bytes != null && Objects.checkFromIndexSize(offset, len, bytes.length) >= 0;
        if(len > 1) {
            return ArrayAccess.getShort(bytes, offset, ByteOrder.BIG_ENDIAN);
        }
        return bytes[offset];
    }

}
