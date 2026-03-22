package io.jingproject.marshall;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class HashUtil {
    private HashUtil() {
        throw new AssertionError();
    }

    public static int lengthHashCollisions(List<byte[]> bytes) {
        Set<Integer> hashes = new HashSet<>();
        int collisions = 0;
        for (byte[] b : bytes) {
            if(!hashes.add(b.length)) {
                collisions = Math.incrementExact(collisions);
            }
        }
        return collisions;
    }

    public static int firstByteHashCollisions(List<byte[]> bytes) {
        Set<Integer> hashes = new HashSet<>();
        int collisions = 0;
        for (byte[] b : bytes) {
            int hash = b.length == 0 ? 0 : Byte.toUnsignedInt(b[0]);
            if(!hashes.add(hash)) {
                collisions = Math.incrementExact(collisions);
            }
        }
        return collisions;
    }

    public static int factorHash(byte[] bytes, int offset, int len, int factor) {
        Objects.checkFromIndexSize(offset, bytes.length, len);
        int hash = len;
        for (int i = offset; i < offset + len; i++) {
            hash = Math.multiplyExact(hash, factor);
            hash = Math.addExact(hash, Byte.toUnsignedInt(bytes[i]));
        }
        return hash;
    }
}
