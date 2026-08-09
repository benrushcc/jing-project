package io.jingproject.marshall.hash;

import io.jingproject.common.Os;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

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
    public int hash(byte[] bytes, int offset, int len) {
        Objects.checkFromIndexSize(offset, len, bytes.length);
        int i1 = Byte.toUnsignedInt(bytes[offset]);
        int i2 = len > 1 ? Byte.toUnsignedInt(bytes[offset + 1]) : 0;
        int i3 = len > 2 ? Byte.toUnsignedInt(bytes[offset + len - 1]) : 0;
        int i4 = len > 3 ? Byte.toUnsignedInt(bytes[offset + len - 2]) : 0;
        return (i4 << 24) | (i3 << 16) | (i2 << 8) | i1;
    }

    @Override
    public int hash(MemorySegment segment, long offset, long len) {
        Objects.checkFromIndexSize(offset, len, segment.byteSize());
        int i1 = Byte.toUnsignedInt(segment.get(ValueLayout.JAVA_BYTE, offset));
        int i2 = len > 1L ? Byte.toUnsignedInt(segment.get(ValueLayout.JAVA_BYTE, offset + 1L)) : 0;
        int i3 = len > 2L ? Byte.toUnsignedInt(segment.get(ValueLayout.JAVA_BYTE, offset + len - 1L)) : 0;
        int i4 = len > 3L ? Byte.toUnsignedInt(segment.get(ValueLayout.JAVA_BYTE, offset + len - 2L)) : 0;
        return (i4 << 24) | (i3 << 16) | (i2 << 8) | i1;
    }

}
