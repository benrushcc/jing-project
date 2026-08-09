package io.jingproject.marshalltest.test;

import io.jingproject.marshall.hash.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Tag("view-output")
public class HashStrategyTest {
    private static final List<byte[]> WORDS = createWords();
    private static final int BATCH = 1000;
    private static final Hasher LENGTH_HASHER = new LengthHasher();
    private static final Hasher ONEBYTE_HASHER = new OneByteHasher();
    private static final Hasher TWOBYTE_HASHER = new TwoByteHasher();
    private static final Hasher THREEBYTE_HASHER = new ThreeByteHasher();
    private static final Hasher FOURBYTE_HASHER = new FourByteHasher();
    private static final Hasher SUM_HASHER = new SumHasher();
    private static final Hasher FNV_HASHER = new FnvHasher();

    private static List<byte[]> createWords() {
        try (InputStream rawStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("words.txt")) {
            if (rawStream == null) {
                throw new AssertionError("words.txt file not found from resources");
            }
            try (InputStreamReader reader = new InputStreamReader(rawStream, StandardCharsets.UTF_8)) {
                List<String> lines = reader.readAllLines();
                System.out.println("Read lines: " + lines.size());
                return lines.stream().map(s -> s.getBytes(StandardCharsets.UTF_8)).toList();
            }
        } catch (IOException e) {
            throw new AssertionError("Failed to load words.txt file from resources", e);
        }
    }

    private static void detectCollisions(int elementSize, Hasher hasher) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int maxCollisions = Integer.MIN_VALUE;
        int collisionTimes = 0;
        Set<Integer> hashes = new HashSet<>(elementSize);
        Set<Integer> filter = new HashSet<>(elementSize);
        for (int r = 0; r < BATCH; r++) {
            int col = 0;
            int i = 0;
            while (i < elementSize) {
                int index = random.nextInt(WORDS.size());
                if (filter.add(index)) {
                    byte[] word = WORDS.get(index);
                    int hash = hasher.hash(word);
                    if (!hashes.add(hash)) {
                        col++;
                    }
                    i++;
                }
            }
            if (col > 0) {
                collisionTimes = Math.incrementExact(collisionTimes);
            }
            if (col > maxCollisions) {
                maxCollisions = col;
            }
            filter.clear();
            hashes.clear();
        }
        System.out.println("maximum collisions: " + maxCollisions);
        System.out.println("collision count: " + collisionTimes);
        System.out.println("collision rate: " + (collisionTimes * 100.0 / BATCH) + "%");
    }

    @Test
    public void testDetectLengthHashFor4Elements() {
        // 54% -> 3
        detectCollisions(4, LENGTH_HASHER);
    }

    @Test
    public void testDetectLengthHashFor8Elements() {
        // 98% -> 6, not usable
        detectCollisions(8, LENGTH_HASHER);
    }

    @Test
    public void testDetectLengthHashFor16Elements() {
        // 100% -> 12, not usable
        detectCollisions(16, LENGTH_HASHER);
    }

    @Test
    public void testOneByteHashFor4Elements() {
        // 30% -> 2
        detectCollisions(4, ONEBYTE_HASHER);
    }

    @Test
    public void testOneByteHashFor8Elements() {
        // 84% -> 5, not usable
        detectCollisions(8, ONEBYTE_HASHER);
    }

    @Test
    public void testOneByteHashFor16Elements() {
        // 100% -> 10, not usable
        detectCollisions(16, ONEBYTE_HASHER);
    }

    @Test
    public void testTwoByteHashFor4Elements() {
        // 6% -> 1
        detectCollisions(4, TWOBYTE_HASHER);
    }

    @Test
    public void testTwoByteHashFor8Elements() {
        // 26% -> 3
        detectCollisions(8, TWOBYTE_HASHER);
    }

    @Test
    public void testTwoByteHashFor16Elements() {
        // 72% -> 5, not usable
        detectCollisions(16, TWOBYTE_HASHER);
    }

    @Test
    public void testThreeByteHashFor4Elements() {
        // 1.3% -> 1
        detectCollisions(4, THREEBYTE_HASHER);
    }

    @Test
    public void testThreeByteHashFor8Elements() {
        // 5% -> 2
        detectCollisions(8, THREEBYTE_HASHER);
    }

    @Test
    public void testThreeByteHashFor16Elements() {
        // 20% -> 3
        detectCollisions(16, THREEBYTE_HASHER);
    }

    @Test
    public void testThreeByteHashFor32Elements() {
        // 62% -> 5 not usable
        detectCollisions(32, THREEBYTE_HASHER);
    }

    @Test
    public void testFourByteHashFor4Elements() {
        // 0.5% -> 1
        detectCollisions(4, FOURBYTE_HASHER);
    }

    @Test
    public void testFourByteHashFor8Elements() {
        // 1.2% -> 1
        detectCollisions(8, FOURBYTE_HASHER);
    }

    @Test
    public void testFourByteHashFor16Elements() {
        // 5.5% -> 2
        detectCollisions(16, FOURBYTE_HASHER);
    }

    @Test
    public void testFourByteHashFor32Elements() {
        // 22% -> 3
        detectCollisions(32, FOURBYTE_HASHER);
    }

    @Test
    public void testSumHashFor4Elements() {
        // 0.1% -> 1
        detectCollisions(4, SUM_HASHER);
    }

    @Test
    public void testSumHashFor8Elements() {
        // 0.1% -> 1
        detectCollisions(8, SUM_HASHER);
    }

    @Test
    public void testSumHashFor16Elements() {
        // 0.1% -> 1
        detectCollisions(16, SUM_HASHER);
    }

    @Test
    public void testSumHashFor32Elements() {
        // 1% -> 1
        detectCollisions(32, SUM_HASHER);
    }

    @Test
    public void testFnvHashFor32Elements() {
        // 0%
        detectCollisions(32, FNV_HASHER);
    }

    @Test
    public void testFnvHashFor128Elements() {
        // 0%
        detectCollisions(128, FNV_HASHER);
    }

    @Test
    public void testFnvHashFor512Elements() {
        // 0.1% -> 1
        detectCollisions(512, FNV_HASHER);
    }

    @Test
    public void testFnvHashFor2048Elements() {
        // 0.3% -> 1
        detectCollisions(2048, FNV_HASHER);
    }
}
