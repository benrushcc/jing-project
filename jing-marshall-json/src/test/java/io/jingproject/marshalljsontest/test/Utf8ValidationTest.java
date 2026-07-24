package io.jingproject.marshalljsontest.test;

import io.jingproject.marshalljson.Utf8Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Utf8ValidationTest {
    private static final int BUFFER_SIZE = 128; // two avx-512 vector
    private static final int[] OFFSETS = new int[]{0, 1, 2, 3, 62, 63, 64, 65};

    private static void test(byte[] buffer) {
        boolean r1 = Utf8Validator.scalarValidate(buffer, 0, buffer.length);
        boolean r2 = Utf8Validator.validate(buffer, 0, buffer.length);
        Assertions.assertEquals(r1, r2);
        MemorySegment memorySegment = MemorySegment.ofArray(buffer);
        boolean r3 = Utf8Validator.validate(memorySegment, 0L, memorySegment.byteSize());
        boolean r4 = Utf8Validator.scalarValidate(memorySegment, 0L, memorySegment.byteSize());
        Assertions.assertEquals(r3, r4);
    }

    private static void validateTwoBytes(byte first, byte[] buffer) {
        for(int i1 = -128; i1 < 128; i1++) {
            for (int offset : OFFSETS) {
                Arrays.fill(buffer, (byte) 0);
                buffer[offset] = first;
                buffer[offset + 1] = (byte) i1;
                test(buffer);
            }
        }
    }

    private static void validateThreeBytes(byte first, byte[] buffer) {
        for(int i1 = -128; i1 < 128; i1++) {
            for(int i2 = -128; i2 < 128; i2++) {
                for (int offset : OFFSETS) {
                    Arrays.fill(buffer, (byte) 0);
                    buffer[offset] = first;
                    buffer[offset + 1] = (byte) i1;
                    buffer[offset + 2] = (byte) i2;
                    test(buffer);
                }
            }
        }
    }

    private static void validateFourBytes(byte first, byte[] buffer) {
        for(int i1 = -128; i1 < 128; i1++) {
            for(int i2 = -128; i2 < 128; i2++) {
                for(int i3 = -128; i3 < 128; i3++) {
                    for (int offset : OFFSETS) {
                        Arrays.fill(buffer, (byte) 0);
                        buffer[offset] = first;
                        buffer[offset + 1] = (byte) i1;
                        buffer[offset + 2] = (byte) i2;
                        buffer[offset + 3] = (byte) i3;
                        test(buffer);
                    }
                }
            }
        }
    }

    @Test
    public void testOneByteValidation() {
        byte[] buffer = new byte[BUFFER_SIZE];
        for(int i = -128; i < 128; i++) {
            for(int offset : OFFSETS) {
                Arrays.fill(buffer, (byte) 0);
                buffer[offset] = (byte) i;
                test(buffer);
            }
        }
    }

    @Test
    public void testTwoByteValidation() {
        byte[] buffer = new byte[BUFFER_SIZE];
        for(int i = -128; i < 128; i++) {
            validateTwoBytes((byte) i, buffer);
        }
    }

    @Test
    @Tag("exhaustive")
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    public void testThreeByteValidation() {
        byte[] buffer = new byte[BUFFER_SIZE];
        for(int i = -128; i < 128; i++) {
            validateThreeBytes((byte) i, buffer);
        }
    }

    @Test
    @Tag("exhaustive")
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    public void testFourByteValidation() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(256);
        for(int i = -128; i < 128; i++) {
            byte b = (byte) i;
            new Thread(() -> {
                validateFourBytes(b, new byte[BUFFER_SIZE]);
                latch.countDown();
            }).start();
        }
        latch.await();
    }
}
