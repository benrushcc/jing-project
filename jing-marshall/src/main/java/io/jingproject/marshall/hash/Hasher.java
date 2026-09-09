package io.jingproject.marshall.hash;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

/**
 * Sealed hash function interface. Implementations provide 32-bit hash values for bytes,
 * {@link MemorySegment}, arrays, and strings.
 */
public sealed interface Hasher
        permits LengthHasher, OneByteHasher, TwoByteHasher, ThreeByteHasher, FourByteHasher, SumHasher, FnvHasher {
    int hash(byte[] bytes, int from, int to);

    int hash(MemorySegment segment, long from, long to);

    default int hash(MemorySegment segment) {
        return hash(segment, 0L, segment.byteSize());
    }

    default int hash(byte[] bytes) {
        return hash(bytes, 0, bytes.length);
    }

    default int hash(String str) {
        return hash(str.getBytes(StandardCharsets.UTF_8));
    }
}
