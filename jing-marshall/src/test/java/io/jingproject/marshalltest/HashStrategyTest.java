package io.jingproject.marshalltest;

import io.jingproject.marshall.hash.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

    private value record CollisionResult(int maxCollisions, int collisionTimes, double collisionRate) {

    }

    private static List<byte[]> createWords() {
        try (InputStream rawStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("words.txt")) {
            if (rawStream == null) {
                throw new AssertionError("words.txt file not found from resources");
            }
            try (InputStreamReader reader = new InputStreamReader(rawStream, StandardCharsets.UTF_8)) {
                return reader.readAllLines().stream().map(s -> s.getBytes(StandardCharsets.UTF_8)).toList();
            }
        } catch (IOException e) {
            throw new AssertionError("Failed to load words.txt file from resources", e);
        }
    }

    private static CollisionResult detectCollisions(int elementSize, Hasher hasher) {
        Random random = new Random(42L);
        int maxCollisions = 0;
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
                collisionTimes++;
            }
            if (col > maxCollisions) {
                maxCollisions = col;
            }
            filter.clear();
            hashes.clear();
        }
        return new CollisionResult(maxCollisions, collisionTimes, collisionTimes * 100.0 / BATCH);
    }

    @Test
    public void testLengthHash() {
        CollisionResult r4 = detectCollisions(4, LENGTH_HASHER);
        Assertions.assertTrue(r4.collisionRate() > 30 && r4.collisionRate() < 80,
                "LengthHash(4) collision rate out of range: " + r4.collisionRate());
        Assertions.assertTrue(r4.maxCollisions() >= 1 && r4.maxCollisions() <= 10,
                "LengthHash(4) max collisions out of range: " + r4.maxCollisions());

        CollisionResult r8 = detectCollisions(8, LENGTH_HASHER);
        Assertions.assertTrue(r8.collisionRate() > 80, "LengthHash(8) should have high collision rate: " + r8.collisionRate());
        Assertions.assertTrue(r8.maxCollisions() >= 3, "LengthHash(8) should have notable max collisions: " + r8.maxCollisions());

        CollisionResult r16 = detectCollisions(16, LENGTH_HASHER);
        Assertions.assertTrue(r16.collisionRate() > 90, "LengthHash(16) should have very high collision rate: " + r16.collisionRate());
        Assertions.assertTrue(r16.maxCollisions() >= 5, "LengthHash(16) should have significant max collisions: " + r16.maxCollisions());
    }

    @Test
    public void testOneByteHash() {
        CollisionResult r4 = detectCollisions(4, ONEBYTE_HASHER);
        Assertions.assertTrue(r4.collisionRate() > 10 && r4.collisionRate() < 60,
                "OneByteHash(4) collision rate out of range: " + r4.collisionRate());
        Assertions.assertTrue(r4.maxCollisions() >= 1 && r4.maxCollisions() <= 6,
                "OneByteHash(4) max collisions out of range: " + r4.maxCollisions());

        CollisionResult r8 = detectCollisions(8, ONEBYTE_HASHER);
        Assertions.assertTrue(r8.collisionRate() > 60, "OneByteHash(8) should have high collision rate: " + r8.collisionRate());
        Assertions.assertTrue(r8.maxCollisions() >= 2, "OneByteHash(8) should have notable max collisions: " + r8.maxCollisions());

        CollisionResult r16 = detectCollisions(16, ONEBYTE_HASHER);
        Assertions.assertTrue(r16.collisionRate() > 90, "OneByteHash(16) should have very high collision rate: " + r16.collisionRate());
        Assertions.assertTrue(r16.maxCollisions() >= 5, "OneByteHash(16) should have significant max collisions: " + r16.maxCollisions());
    }

    @Test
    public void testTwoByteHash() {
        CollisionResult r4 = detectCollisions(4, TWOBYTE_HASHER);
        Assertions.assertTrue(r4.collisionRate() < 20,
                "TwoByteHash(4) collision rate out of range: " + r4.collisionRate());
        Assertions.assertTrue(r4.maxCollisions() <= 3,
                "TwoByteHash(4) max collisions out of range: " + r4.maxCollisions());

        CollisionResult r8 = detectCollisions(8, TWOBYTE_HASHER);
        Assertions.assertTrue(r8.collisionRate() > 10 && r8.collisionRate() < 50,
                "TwoByteHash(8) collision rate out of range: " + r8.collisionRate());
        Assertions.assertTrue(r8.maxCollisions() >= 1 && r8.maxCollisions() <= 5,
                "TwoByteHash(8) max collisions out of range: " + r8.maxCollisions());

        CollisionResult r16 = detectCollisions(16, TWOBYTE_HASHER);
        Assertions.assertTrue(r16.collisionRate() > 50, "TwoByteHash(16) should have high collision rate: " + r16.collisionRate());
        Assertions.assertTrue(r16.maxCollisions() >= 2, "TwoByteHash(16) should have notable max collisions: " + r16.maxCollisions());
    }

    @Test
    public void testThreeByteHash() {
        CollisionResult r4 = detectCollisions(4, THREEBYTE_HASHER);
        Assertions.assertTrue(r4.collisionRate() < 10,
                "ThreeByteHash(4) collision rate out of range: " + r4.collisionRate());
        Assertions.assertTrue(r4.maxCollisions() <= 3,
                "ThreeByteHash(4) max collisions out of range: " + r4.maxCollisions());

        CollisionResult r8 = detectCollisions(8, THREEBYTE_HASHER);
        Assertions.assertTrue(r8.collisionRate() < 15,
                "ThreeByteHash(8) collision rate out of range: " + r8.collisionRate());
        Assertions.assertTrue(r8.maxCollisions() <= 4,
                "ThreeByteHash(8) max collisions out of range: " + r8.maxCollisions());

        CollisionResult r16 = detectCollisions(16, THREEBYTE_HASHER);
        Assertions.assertTrue(r16.collisionRate() > 10 && r16.collisionRate() < 40,
                "ThreeByteHash(16) collision rate out of range: " + r16.collisionRate());

        CollisionResult r32 = detectCollisions(32, THREEBYTE_HASHER);
        Assertions.assertTrue(r32.collisionRate() > 40, "ThreeByteHash(32) should have notable collision rate: " + r32.collisionRate());
    }

    @Test
    public void testFourByteHash() {
        CollisionResult r4 = detectCollisions(4, FOURBYTE_HASHER);
        Assertions.assertTrue(r4.collisionRate() < 5,
                "FourByteHash(4) collision rate out of range: " + r4.collisionRate());
        Assertions.assertTrue(r4.maxCollisions() <= 2,
                "FourByteHash(4) max collisions out of range: " + r4.maxCollisions());

        CollisionResult r8 = detectCollisions(8, FOURBYTE_HASHER);
        Assertions.assertTrue(r8.collisionRate() < 10,
                "FourByteHash(8) collision rate out of range: " + r8.collisionRate());
        Assertions.assertTrue(r8.maxCollisions() <= 3,
                "FourByteHash(8) max collisions out of range: " + r8.maxCollisions());

        CollisionResult r16 = detectCollisions(16, FOURBYTE_HASHER);
        Assertions.assertTrue(r16.collisionRate() < 15,
                "FourByteHash(16) collision rate out of range: " + r16.collisionRate());

        CollisionResult r32 = detectCollisions(32, FOURBYTE_HASHER);
        Assertions.assertTrue(r32.collisionRate() > 10 && r32.collisionRate() < 40,
                "FourByteHash(32) collision rate out of range: " + r32.collisionRate());
    }

    @Test
    public void testSumHash() {
        CollisionResult r4 = detectCollisions(4, SUM_HASHER);
        Assertions.assertTrue(r4.collisionRate() < 5,
                "SumHash(4) collision rate out of range: " + r4.collisionRate());
        Assertions.assertTrue(r4.maxCollisions() <= 2,
                "SumHash(4) max collisions out of range: " + r4.maxCollisions());

        CollisionResult r8 = detectCollisions(8, SUM_HASHER);
        Assertions.assertTrue(r8.collisionRate() < 5,
                "SumHash(8) collision rate out of range: " + r8.collisionRate());
        Assertions.assertTrue(r8.maxCollisions() <= 2,
                "SumHash(8) max collisions out of range: " + r8.maxCollisions());

        CollisionResult r16 = detectCollisions(16, SUM_HASHER);
        Assertions.assertTrue(r16.collisionRate() < 5,
                "SumHash(16) collision rate out of range: " + r16.collisionRate());

        CollisionResult r32 = detectCollisions(32, SUM_HASHER);
        Assertions.assertTrue(r32.collisionRate() < 10,
                "SumHash(32) collision rate out of range: " + r32.collisionRate());
    }

    @Test
    public void testFnvHash() {
        CollisionResult r32 = detectCollisions(32, FNV_HASHER);
        Assertions.assertTrue(r32.collisionRate() < 5,
                "FnvHash(32) collision rate out of range: " + r32.collisionRate());
        Assertions.assertTrue(r32.maxCollisions() <= 2,
                "FnvHash(32) max collisions out of range: " + r32.maxCollisions());

        CollisionResult r128 = detectCollisions(128, FNV_HASHER);
        Assertions.assertTrue(r128.collisionRate() < 5,
                "FnvHash(128) collision rate out of range: " + r128.collisionRate());
        Assertions.assertTrue(r128.maxCollisions() <= 2,
                "FnvHash(128) max collisions out of range: " + r128.maxCollisions());

        CollisionResult r512 = detectCollisions(512, FNV_HASHER);
        Assertions.assertTrue(r512.collisionRate() < 5,
                "FnvHash(512) collision rate out of range: " + r512.collisionRate());

        CollisionResult r2048 = detectCollisions(2048, FNV_HASHER);
        Assertions.assertTrue(r2048.collisionRate() < 5,
                "FnvHash(2048) collision rate out of range: " + r2048.collisionRate());
    }
}
