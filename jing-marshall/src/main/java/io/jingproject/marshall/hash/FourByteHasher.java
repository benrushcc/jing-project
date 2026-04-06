package io.jingproject.marshall.hash;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

public final class FourByteHasher implements Hasher {
    @Override
    public int hash(MemorySegment segment, long offset, long len) {
        assert segment != null && segment.byteSize() > 0L && len > 0L;
        Objects.checkFromIndexSize(offset, len, segment.byteSize());
        int i1 = Byte.toUnsignedInt(segment.get(ValueLayout.JAVA_BYTE, offset));
        int i2 = len > 1L ? Byte.toUnsignedInt(segment.get(ValueLayout.JAVA_BYTE, offset + 1L)) : 0;
        int i3 = len > 2L ? Byte.toUnsignedInt(segment.get(ValueLayout.JAVA_BYTE, offset + len - 1L)) : 0;
        int i4 = len > 3L ? Byte.toUnsignedInt(segment.get(ValueLayout.JAVA_BYTE, offset + len - 2L)) : 0;
        return (i4 << 24) | (i3 << 16) | (i2 << 8) | i1;
    }

    @Override
    public int hash(byte[] bytes, int offset, int len) {
        assert bytes != null && bytes.length > 0 && len > 0;
        Objects.checkFromIndexSize(offset, len, bytes.length);
        int i1 = Byte.toUnsignedInt(bytes[offset]);
        int i2 = len > 1 ? Byte.toUnsignedInt(bytes[offset + 1]) : 0;
        int i3 = len > 2 ? Byte.toUnsignedInt(bytes[offset + len - 1]) : 0;
        int i4 = len > 3 ? Byte.toUnsignedInt(bytes[offset + len - 2]) : 0;
        return (i4 << 24) | (i3 << 16) | (i2 << 8) | i1;
    }
}
