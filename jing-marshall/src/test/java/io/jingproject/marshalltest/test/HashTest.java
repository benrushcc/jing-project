package io.jingproject.marshalltest.test;

import io.jingproject.marshall.HashUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class HashTest {
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int MIN_LENGTH = 2;
    private static final int MID_LENGTH = 10;
    private static final int MAX_LENGTH = 64;

    private static final int BATCH = 1000;

    private static List<byte[]> generateRandomBytes(int elements) {
        Set<String> seen = new HashSet<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < elements; ) {
            int byteCount;
            if(random.nextInt(10) < 2) {
                byteCount = random.nextInt(MID_LENGTH, MAX_LENGTH);
            } else {
                byteCount = random.nextInt(MIN_LENGTH, MID_LENGTH);
            }
            StringBuilder sb = new StringBuilder(byteCount);
            for(int j = 0; j < byteCount; j++) {
                int charIndex = random.nextInt(CHAR_POOL.length());
                sb.append(CHAR_POOL.charAt(charIndex));
            }
            if(seen.add(sb.toString())) {
                i++;
            }
        }
        return seen.stream().map(s -> s.getBytes(StandardCharsets.UTF_8)).toList();
    }

    private static void detectLengthHash(int elements) {
        int successfulCount = 0;
        int maxCollisions = -1;
        for(int i = 0; i < BATCH; i++) {
            List<byte[]> bytes = generateRandomBytes(elements);
            int collisions = HashUtil.lengthHashCollisions(bytes);
            if(collisions > maxCollisions) {
                maxCollisions = collisions;
            }
            if(collisions == 0) {
                successfulCount++;
            }
        }
        System.out.println("SuccessfulCount : " + successfulCount);
        System.out.println("SuccessfulRate : " + (successfulCount * 100.0 / BATCH) + "%");
        System.out.println("MaxCollisions : " + maxCollisions);
    }

    private static void detectFirstByteHash(int elements) {
        int successfulCount = 0;
        int maxCollisions = -1;
        for(int i = 0; i < BATCH; i++) {
            List<byte[]> bytes = generateRandomBytes(elements);
            int collisions = HashUtil.firstByteHashCollisions(bytes);
            if(collisions > maxCollisions) {
                maxCollisions = collisions;
            }
            if(collisions == 0) {
                successfulCount++;
            }
        }
        System.out.println("SuccessfulCount : " + successfulCount);
        System.out.println("SuccessfulRate : " + (successfulCount * 100.0 / BATCH) + "%");
        System.out.println("MaxCollisions : " + maxCollisions);
    }

    @Test
    public void testDetectLengthHashFor4Elements() {
        // 60%
        detectLengthHash(4);
    }

    @Test
    public void testDetectLengthHashFor8Elements() {
        // 8%
        detectLengthHash(8);
    }

    @Test
    public void testDetectLengthHashFor16Elements() {
        // 0%
        detectLengthHash(16);
    }

    @Test
    public void testDetectFirstByteHashFor4Elements() {
        // 91%
        detectFirstByteHash(4);
    }

    @Test
    public void testDetectFirstByteHashFor8Elements() {
        // 61%
        detectFirstByteHash(8);
    }

    @Test
    public void testDetectFirstByteHashFor16Elements() {
        // 11%
        detectFirstByteHash(16);
    }
}
