package io.jingproject.marshalltest.test;

import io.jingproject.marshall.hash.HashUtil;
import io.jingproject.marshall.hash.Hasher;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Tag("view-output")
public class HashStrategyTest {
    private static final List<byte[]> WORDS = createWords();
    private static final int BATCH = 1000;

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

    private static void detectCollisions(int elements, Hasher hasher) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int successfulCount = 0;
        int maxCollisions = -1;
        for (int i = 0; i < BATCH; i++) {
            Set<byte[]> selectedWords = new LinkedHashSet<>();
            while (selectedWords.size() < elements) {
                int index = random.nextInt(WORDS.size());
                byte[] word = WORDS.get(index);
                selectedWords.add(word);
            }
            Set<Integer> hashes = new LinkedHashSet<>();
            int collisions = 0;
            for (byte[] selectedWord : selectedWords) {
                int hash = hasher.hash(selectedWord);
                if (!hashes.add(hash)) {
                    collisions = Math.incrementExact(collisions);
                }
            }
            if (collisions > maxCollisions) {
                maxCollisions = collisions;
            }
            if (collisions == 0) {
                successfulCount = Math.incrementExact(successfulCount);
            }
        }
        System.out.println("Maximum collisions: " + maxCollisions);
        System.out.println("Successful count: " + successfulCount);
        System.out.println("Successful rate: " + (successfulCount * 100.0 / BATCH) + "%");
    }

    @Test
    public void testDetectLengthHashFor4Elements() {
        // 45%
        detectCollisions(4, HashUtil.lengthHasher());
    }

    @Test
    public void testDetectLengthHashFor8Elements() {
        // 1.5%
        detectCollisions(8, HashUtil.lengthHasher());
    }

    @Test
    public void testDetectLengthHashFor16Elements() {
        // 0%
        detectCollisions(16, HashUtil.lengthHasher());
    }

    @Test
    public void testOneByteHashFor4Elements() {
        // 67%
        detectCollisions(4, HashUtil.oneByteHasher());
    }

    @Test
    public void testOneByteHashFor8Elements() {
        // 14%
        detectCollisions(8, HashUtil.oneByteHasher());
    }

    @Test
    public void testOneByteHashFor16Elements() {
        // 0.1%
        detectCollisions(16, HashUtil.oneByteHasher());
    }

    @Test
    public void testTwoByteHashFor4Elements() {
        // 93%
        detectCollisions(4, HashUtil.twoByteHasher());
    }

    @Test
    public void testTwoByteHashFor8Elements() {
        // 72%
        detectCollisions(8, HashUtil.twoByteHasher());
    }

    @Test
    public void testTwoByteHashFor16Elements() {
        // 28%
        detectCollisions(16, HashUtil.twoByteHasher());
    }

    @Test
    public void testThreeByteHashFor4Elements() {
        // 97%
        detectCollisions(4, HashUtil.threeByteHasher());
    }

    @Test
    public void testThreeByteHashFor8Elements() {
        // 94%
        detectCollisions(8, HashUtil.threeByteHasher());
    }

    @Test
    public void testThreeByteHashFor16Elements() {
        // 79%
        detectCollisions(16, HashUtil.threeByteHasher());
    }

    @Test
    public void testThreeByteHashFor32Elements() {
        // 37%
        detectCollisions(32, HashUtil.threeByteHasher());
    }

    @Test
    public void testFourByteHashFor4Elements() {
        // 99%
        detectCollisions(4, HashUtil.fourByteHasher());
    }

    @Test
    public void testFourByteHashFor8Elements() {
        // 98%
        detectCollisions(8, HashUtil.fourByteHasher());
    }

    @Test
    public void testFourByteHashFor16Elements() {
        // 93%
        detectCollisions(16, HashUtil.fourByteHasher());
    }

    @Test
    public void testFourByteHashFor32Elements() {
        // 76%
        detectCollisions(32, HashUtil.fourByteHasher());
    }

    @Test
    public void testFnvMulHashFor32Elements() {
        // 100%
        detectCollisions(32, HashUtil.fnvMulHasher());
    }

    @Test
    public void testFnvMulHashFor128Elements() {
        // 100%
        detectCollisions(128, HashUtil.fnvMulHasher());
    }

    @Test
    public void testFnvMulHashFor512Elements() {
        // 99.9%
        detectCollisions(512, HashUtil.fnvMulHasher());
    }

    @Test
    public void testFnvMulHashFor2048Elements() {
        // 99.9%
        detectCollisions(2048, HashUtil.fnvMulHasher());
    }
}
