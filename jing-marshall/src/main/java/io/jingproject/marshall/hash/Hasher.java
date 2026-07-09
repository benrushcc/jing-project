package io.jingproject.marshall.hash;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

/**
 * Sealed hash function interface. Implementations provide 32-bit hash values for bytes,
 * {@link MemorySegment}, arrays, and strings. All {@code hash} methods assume input
 * bounds (offset/length) are already validated by the caller; no range checks are performed.
 */
public sealed interface Hasher
        permits LengthHasher, OneByteHasher, TwoByteHasher, ThreeByteHasher, FourByteHasher, SumHasher, FnvHasher {
    int hash(byte[] bytes, int offset, int len);

    int hash(MemorySegment segment, long offset, long len);

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
