package io.jingproject.marshall.hash;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

public final class OneByteHasher implements Hasher {
    @Override
    public int hash(MemorySegment segment, long offset, long len) {
        assert segment != null && segment.byteSize() > 0L && len > 0L;
        Objects.checkFromIndexSize(offset, len, segment.byteSize());
        return Byte.toUnsignedInt(segment.get(ValueLayout.JAVA_BYTE, offset));
    }

    @Override
    public int hash(byte[] bytes, int offset, int len) {
        assert bytes != null && bytes.length > 0 && len > 0;
        Objects.checkFromIndexSize(offset, len, bytes.length);
        return Byte.toUnsignedInt(bytes[offset]);
    }
}
