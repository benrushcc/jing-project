package io.jingproject.marshall.hash;

import io.jingproject.common.Os;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

public final class TwoByteHasher implements Hasher {
    static {
        try{
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public int hash(MemorySegment segment, long offset, long len) {
        assert segment != null && segment.byteSize() > 0L && len > 0L;
        Objects.checkFromIndexSize(offset, len, segment.byteSize());
        int i1 = Byte.toUnsignedInt(segment.get(ValueLayout.JAVA_BYTE, offset));
        int i2 = len > 1L ? Byte.toUnsignedInt(segment.get(ValueLayout.JAVA_BYTE, offset + 1L)) : 0;
        return (i2 << 8) | i1;
    }

    @Override
    public int hash(byte[] bytes, int offset, int len) {
        assert bytes != null && bytes.length > 0 && len > 0;
        Objects.checkFromIndexSize(offset, len, bytes.length);
        int i1 = Byte.toUnsignedInt(bytes[offset]);
        int i2 = len > 1 ? Byte.toUnsignedInt(bytes[offset + 1]) : 0;
        return (i2 << 8) | i1;
    }
}
