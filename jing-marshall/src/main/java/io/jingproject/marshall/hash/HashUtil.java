package io.jingproject.marshall.hash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HashUtil {
    // hashers
    private static final List<Hasher> HASHERS = List.of(
            new LengthHasher(),
            new OneByteHasher(),
            new TwoByteHasher(),
            new ThreeByteHasher(),
            new FourByteHasher(),
            new FnvHasher()
    );

    private HashUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static Hasher lengthHasher() {
        return HASHERS.get(0);
    }

    public static Hasher oneByteHasher() {
        return HASHERS.get(1);
    }

    public static Hasher twoByteHasher() {
        return HASHERS.get(2);
    }

    public static Hasher threeByteHasher() {
        return HASHERS.get(3);
    }

    public static Hasher fourByteHasher() {
        return HASHERS.get(4);
    }

    public static Hasher fnvMulHasher() {
        return HASHERS.get(5);
    }

    public static Hasher calcHasher(List<String> strings) {
        int maxCollisions = Integer.MAX_VALUE;
        Hasher fallback = HASHERS.getLast();
        for (Hasher currentHasher : HASHERS) {
            Set<Integer> hs = new HashSet<>(strings.size());
            int collisions = 0;
            for (String s : strings) {
                int hash = currentHasher.hash(s);
                if (!hs.add(hash)) {
                    collisions = Math.incrementExact(collisions);
                }
            }
            if (collisions == 0) {
                return currentHasher;
            }
            if (collisions < maxCollisions) {
                maxCollisions = collisions;
                fallback = currentHasher;
            }
        }
        return fallback;
    }

    public static byte[] calcBytes(List<String> strings) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (String s : strings) {
                baos.write(s.getBytes(StandardCharsets.UTF_8));
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
