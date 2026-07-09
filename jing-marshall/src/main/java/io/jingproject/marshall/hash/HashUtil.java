package io.jingproject.marshall.hash;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

// utility class for selecting and using hash functions
public final class HashUtil {
    // hashers
    private static final List<Hasher> HASHERS = List.of(
            new LengthHasher(),   // 0
            new OneByteHasher(),  // 1
            new TwoByteHasher(),  // 2
            new ThreeByteHasher(),// 3
            new FourByteHasher(), // 4
            new SumHasher(),      // 5
            new FnvHasher()       // 6
    );

    private HashUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    // returns the hasher at the given index
    public static Hasher hasher(int index) {
        return HASHERS.get(index);
    }

    // selects the hasher with the fewest collisions for the given entities,
    // returns its index; if a zero‑collision hasher exists, returns it immediately
    public static <T> int selectUtf8Hasher(List<T> entities, Function<T, byte[]> fn) {
        int maxCollisions = Integer.MAX_VALUE;
        int fallback = Integer.MIN_VALUE;
        Set<Integer> hs = new HashSet<>();
        for(int index = 0; index < HASHERS.size(); index++) {
            Hasher hasher = hasher(index);
            int collisions = 0;
            for (T entity : entities) {
                int hash = hasher.hash(fn.apply(entity));
                if (!hs.add(hash)) {
                    collisions++; // no overflow, can never exceed entities.size()
                }
            }
            hs.clear();
            if (collisions == 0) {
                return index;
            }
            if (collisions < maxCollisions) {
                maxCollisions = collisions;
                fallback = index;
            }
        }
        if(fallback < 0) {
            throw new AssertionError();
        }
        return fallback;
    }

    // compacts all entity‑derived UTF‑8 byte arrays into one contiguous byte array
    public static <T> byte[] compactUtf8Bytes(List<T> entities, Function<T, byte[]> fn) {
        List<byte[]> utf8Data = new ArrayList<>(entities.size());
        int len = 0;
        for (T entity : entities) {
            byte[] bytes = fn.apply(entity);
            utf8Data.add(bytes);
            len = Math.addExact(len, bytes.length);
        }
        byte[] r = new byte[len];
        int index = 0;
        for (byte[] bytes : utf8Data) {
            System.arraycopy(bytes, 0, r, index, bytes.length);
            index += bytes.length;
        }
        return r;
    }
}
