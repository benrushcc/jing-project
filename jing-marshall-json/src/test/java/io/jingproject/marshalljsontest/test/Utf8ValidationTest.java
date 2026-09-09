package io.jingproject.marshalljsontest.test;

import io.jingproject.common.ArrayAccess;
import io.jingproject.marshalljson.Utf8Validator;
import jdk.incubator.vector.ByteVector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Utf8ValidationTest {
    private static final int VEC_LEN = ByteVector.SPECIES_PREFERRED.length();
    private static final int BUFFER_SIZE = VEC_LEN * 2;
    private static final int[] OFFSETS = new int[] {VEC_LEN - 4, VEC_LEN - 3, VEC_LEN - 2, VEC_LEN - 1, VEC_LEN};

    private static void test(byte[] buffer, int offset, int len) {
        boolean r1 = Utf8Validator.scalarValidateHeap(buffer, offset, offset + len);
        boolean r2 = Utf8Validator.validateHeap(buffer, 0, buffer.length);
        Assertions.assertEquals(r1, r2);
        MemorySegment memorySegment = MemorySegment.ofArray(buffer);
        boolean r3 = Utf8Validator.scalarValidateSegment(memorySegment, offset, offset + len);
        boolean r4 = Utf8Validator.validateSegment(memorySegment, 0L, memorySegment.byteSize());
        Assertions.assertEquals(r3, r4);
    }

    @Test
    public void testOneByteValidation() {
        byte[] buffer = new byte[BUFFER_SIZE];
        for (int offset : OFFSETS) {
            Arrays.fill(buffer, (byte) 0);
            for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
                buffer[offset] = (byte) i;
                test(buffer, offset, 1);
            }
        }
    }

    @Test
    public void testTwoByteValidation() {
        byte[] buffer = new byte[BUFFER_SIZE];
        for(int offset : OFFSETS) {
            Arrays.fill(buffer, (byte) 0);
            for(int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
                ArrayAccess.setShort(buffer, offset, (short) i);
                test(buffer, offset, 2);
            }
        }
    }

    @Test
    @Tag("exhaustive")
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    public void testThreeByteValidation() {
        byte[] buffer = new byte[BUFFER_SIZE];
        for(int offset : OFFSETS) {
            Arrays.fill(buffer, (byte) 0);
            for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
                for(int j = Short.MIN_VALUE; j <= Short.MAX_VALUE; j++) {
                    buffer[offset] = (byte) i;
                    ArrayAccess.setShort(buffer, offset + 1, (short) j);
                    test(buffer, offset, 3);
                }
            }
        }
    }

    @Test
    public void testSurrogateValidation() {
        byte[] buffer = new byte[BUFFER_SIZE];
        for(int offset : OFFSETS) {
            Arrays.fill(buffer, (byte) 0);
            for (int cp = 0x10000; cp <= 0x10FFFF; cp++) {
                buffer[offset] = (byte) (0xF0 | (cp >> 18));
                buffer[offset + 1] = (byte) (0x80 | ((cp >> 12) & 0x3F));
                buffer[offset + 2] = (byte) (0x80 | ((cp >> 6) & 0x3F));
                buffer[offset + 3] = (byte) (0x80 | (cp & 0x3F));
                test(buffer, offset, 4);
            }
        }
    }

    record Bound(long low, long high) {

    }

    @Test
    @Tag("exhaustive")
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    public void testFourByteValidation() throws InterruptedException {
        int threadCount = Runtime.getRuntime().availableProcessors();
        int batchSize = Integer.MAX_VALUE / 1000;
        Queue<Bound> q = new ConcurrentLinkedQueue<>();
        long l = Integer.MIN_VALUE;
        long u = Integer.MAX_VALUE + 1L;
        while (l + batchSize < u) {
            q.add(new Bound(l, l + batchSize));
            l += batchSize;
        }
        if(l < u) {
            q.add(new Bound(l, u));
        }
        int size = q.size();
        CountDownLatch countDownLatch = new CountDownLatch(size);
        for(int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                for( ; ; ) {
                    Bound b = q.poll();
                    if(b == null) {
                        return ;
                    }
                    byte[] bytes = new byte[BUFFER_SIZE];
                    for(int offset : OFFSETS) {
                        Arrays.fill(bytes, (byte) 0);
                        for(long ll = b.low(); ll < b.high(); ll++) {
                            ArrayAccess.setInt(bytes, offset, (int) ll);
                            test(bytes, offset, 4);
                        }
                    }
                    System.out.printf("finished range : %d ~ %d%n", b.low(), b.high());
                    countDownLatch.countDown();
                }
            });
            thread.start();
        }
        countDownLatch.await();
    }
}
