package io.jingproject.marshall.hash;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

public sealed interface Hasher permits LengthHasher, OneByteHasher, TwoByteHasher, ThreeByteHasher, FourByteHasher, FnvHasher {
    int hash(MemorySegment segment, long offset, long len);

    default int hash(MemorySegment segment) {
        return hash(segment, 0L, segment.byteSize());
    }

    default int hash(byte[] bytes, int offset, int len) {
        return hash(MemorySegment.ofArray(bytes), offset, len);
    }

    default int hash(byte[] bytes) {
        return hash(bytes, 0, bytes.length);
    }

    default int hash(String str) {
        return hash(str.getBytes(StandardCharsets.UTF_8));
    }
}
