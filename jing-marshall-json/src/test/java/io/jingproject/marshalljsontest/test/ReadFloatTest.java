package io.jingproject.marshalljsontest.test;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.SegmentReadBuffer;
import io.jingproject.marshalljson.JsonDeserializerException;
import io.jingproject.marshalljson.JsonNumberUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ReadFloatTest {
    private static final int BATCH = 1000000;
    private static final int MAX_FP_SIZE = 24;

    private static void floatToStringTest(List<String> strList) {
        for (String str : strList) {
            float f = Float.parseFloat(str);
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            byte fb1 = heapReadBuffer.readByte();
            float f1 = JsonNumberUtil.readFloat(heapReadBuffer, MAX_FP_SIZE, fb1);
            Assertions.assertEquals(Float.floatToRawIntBits(f), Float.floatToRawIntBits(f1), "failed, heap float : " + str);

            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(MemorySegment.ofArray(bytes));
            byte fb2 = segmentReadBuffer.readByte();
            float f2 = JsonNumberUtil.readFloat(segmentReadBuffer, MAX_FP_SIZE, fb2);
            Assertions.assertEquals(Float.floatToRawIntBits(f), Float.floatToRawIntBits(f2), "failed, segment float : " + str);
        }
    }

    private static void doubleToStringTest(List<String> strList) {
        for (String str : strList) {
            double f = Double.parseDouble(str);
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            byte fb1 = heapReadBuffer.readByte();
            double f1 = JsonNumberUtil.readDouble(heapReadBuffer, MAX_FP_SIZE, fb1);
            Assertions.assertEquals(Double.doubleToRawLongBits(f), Double.doubleToRawLongBits(f1), "failed, heap double : " + str);

            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(MemorySegment.ofArray(bytes));
            byte fb2 = segmentReadBuffer.readByte();
            double f2 = JsonNumberUtil.readDouble(segmentReadBuffer, MAX_FP_SIZE, fb2);
            Assertions.assertEquals(Double.doubleToRawLongBits(f), Double.doubleToRawLongBits(f2), "failed, segment double : " + str);
        }
    }

    @Test
    public void readTargetFloatTest() {
        List<String> strList = List.of("3.4028235E38", "1.4E-45", "0.0", "-0.0");
        for (String str : strList) {
            float f = Float.parseFloat(str);
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            byte fb1 = heapReadBuffer.readByte();
            float f1 = JsonNumberUtil.readFloat(heapReadBuffer, MAX_FP_SIZE, fb1);
            Assertions.assertEquals(Float.floatToRawIntBits(f), Float.floatToRawIntBits(f1));

            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(MemorySegment.ofArray(bytes));
            byte fb2 = segmentReadBuffer.readByte();
            float f2 = JsonNumberUtil.readFloat(segmentReadBuffer, MAX_FP_SIZE, fb2);
            Assertions.assertEquals(Float.floatToRawIntBits(f), Float.floatToRawIntBits(f2));
        }
    }

    @Test
    public void readRandomFloatTest() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<String> strList = new ArrayList<>();
        for (int i = 0; i < BATCH; ) {
            float f = Float.intBitsToFloat(random.nextInt());
            if (Float.isFinite(f)) {
                strList.add(Float.toString(f));
                i++;
            }
        }
        floatToStringTest(strList);
    }

    @Test
    @Tag("exhaustive")
    @Timeout(value = 1, unit = TimeUnit.HOURS)
    public void readAllFloatTest() {
        final int SEGMENT_SIZE = 10000;
        final int BATCH_SIZE = 100;
        final int THREAD_COUNT = Math.max(Runtime.getRuntime().availableProcessors(), 4);
        try (ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT)) {
            List<Future<Void>> futures = new ArrayList<>();
            final long min = Integer.MIN_VALUE;
            final long max = Integer.MAX_VALUE + 1L;
            for (long segStart = min; segStart < max; segStart += SEGMENT_SIZE) {
                final long start = segStart;
                final long end = Math.min(segStart + SEGMENT_SIZE, max);
                futures.add(executor.submit(() -> {
                    List<String> batch = new ArrayList<>(BATCH_SIZE);
                    for (long bits = start; bits < end; bits++) {
                        int ibits = Math.toIntExact(bits);
                        float f = Float.intBitsToFloat(ibits);
                        if (Float.isFinite(f)) {
                            batch.add(Float.toString(f));
                            if (batch.size() >= BATCH_SIZE) {
                                floatToStringTest(batch);
                                batch.clear();
                            }
                        }
                    }
                    if (!batch.isEmpty()) {
                        floatToStringTest(batch);
                    }
                    return null;
                }));
            }
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    Assertions.fail("Failed to run all float test", e.getCause());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Test
    public void readTargetDoubleTest() {
        List<String> strList = List.of("1.7976931348623157E308", "0.99e308", "4.9E-324", "0.0", "-0.0", "1e-345");
        for (String str : strList) {
            double f = Double.parseDouble(str);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(str.getBytes(StandardCharsets.US_ASCII));
            byte firstByte = heapReadBuffer.readByte();
            double f1 = JsonNumberUtil.readDouble(heapReadBuffer, MAX_FP_SIZE, firstByte);
            Assertions.assertEquals(Double.doubleToRawLongBits(f), Double.doubleToRawLongBits(f1));
        }
    }

    @Test
    public void readRandomDoubleTest() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<String> strList = new ArrayList<>();
        for (int i = 0; i < BATCH; ) {
            double f = Double.longBitsToDouble(random.nextLong());
            if (Double.isFinite(f)) {
                strList.add(Double.toString(f));
                i++;
            }
        }
        doubleToStringTest(strList);
    }

    private static void testReadFp(String str, boolean negative, boolean trunc, long d, int p, int len) {
        byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
        if(bytes.length == 0) {
            throw new AssertionError();
        }
        byte firstByte = bytes[0];
        JsonNumberUtil.FpRep fpRepHeap = JsonNumberUtil.readFpFromHeap(firstByte, bytes, 1, MAX_FP_SIZE);
        JsonNumberUtil.FpRep fpRepSeg = JsonNumberUtil.readFpFromSegment(firstByte, MemorySegment.ofArray(bytes), 1L, MAX_FP_SIZE);
        Assertions.assertEquals(fpRepHeap, fpRepSeg);
        Assertions.assertEquals(fpRepHeap.trunc(), trunc);
        if(!trunc) {
            Assertions.assertEquals(fpRepHeap.negative(), negative);
            Assertions.assertEquals(fpRepHeap.d(), d);
            if(p < JsonNumberUtil.MAX_DECIMAL_P) {
                Assertions.assertEquals(fpRepHeap.p(), p);
            }
        }
        Assertions.assertEquals(fpRepHeap.len(), len);
    }

    @Test
    public void readFpBasicIntegers() {
        testReadFp("0", false, false, 0L, 0, 1);
        testReadFp("1", false, false, 1L, 0, 1);
        testReadFp("9", false, false, 9L, 0, 1);
        testReadFp("10", false, false, 10L, 0, 2);
        testReadFp("99", false, false, 99L, 0, 2);
        testReadFp("100", false, false, 100L, 0, 3);
        testReadFp("123", false, false, 123L, 0, 3);
        testReadFp("12345", false, false, 12345L, 0, 5);
        testReadFp("123456789", false, false, 123456789L, 0, 9);
    }

    @Test
    public void readFpNegativeIntegers() {
        testReadFp("-0", true, false, 0L, 0, 2);
        testReadFp("-1", true, false, 1L, 0, 2);
        testReadFp("-10", true, false, 10L, 0, 3);
        testReadFp("-123", true, false, 123L, 0, 4);
        testReadFp("-12345", true, false, 12345L, 0, 6);
    }

    @Test
    public void readFpUnsignedIntegers() {
        testReadFp("9223372036854775807", false, false, 9223372036854775807L, 0, 19);
        testReadFp("9223372036854775808", false, false, Long.parseUnsignedLong("9223372036854775808"), 0, 19);
        testReadFp("9223372036854775809", false, false, Long.parseUnsignedLong("9223372036854775809"), 0, 19);
        testReadFp("9999999999999999999", false, false, Long.parseUnsignedLong("9999999999999999999"), 0, 19);
        testReadFp("10000000000000000000", false, true, 0, 0, 20);
        testReadFp("19999999999999999999", false, true, 0, 0, 20);
    }

    @Test
    public void readFpFracs() {
        testReadFp("0", false, false, 0L, 0, 1);
        testReadFp("0.0", false, false, 0L, -1, 3);
        testReadFp("0.00", false, false, 0L, -2, 4);
        testReadFp("0.000", false, false, 0L, -3, 5);
        testReadFp("-0", true, false, 0L, 0, 2);
        testReadFp("-0.0", true, false, 0L, -1, 4);
        testReadFp("0.1", false, false, 1L, -1, 3);
        testReadFp("0.5", false, false, 5L, -1, 3);
        testReadFp("0.9", false, false, 9L, -1, 3);
        testReadFp("1.0", false, false, 10L, -1, 3);
        testReadFp("1.5", false, false, 15L, -1, 3);
        testReadFp("9.9", false, false, 99L, -1, 3);
        testReadFp("10.5", false, false, 105L, -1, 4);
        testReadFp("123.456", false, false, 123456L, -3, 7);
        testReadFp("-0.0", true, false, 0L, -1, 4);
        testReadFp("-0.5", true, false, 5L, -1, 4);
        testReadFp("-1.5", true, false, 15L, -1, 4);
        testReadFp("-123.456", true, false, 123456L, -3, 8);
    }

    @Test
    public void readFpExps() {
        testReadFp("1e1", false, false, 1L, 1, 3);
        testReadFp("1e10", false, false, 1L, 10, 4);
        testReadFp("1e100", false, false, 1L, 100, 5);
        testReadFp("1e308", false, false, 1L, 308, 5);
        testReadFp("1.5e2", false, false, 15L, 1, 5);
        testReadFp("1.5e10", false, false, 15L, 9, 6);
        testReadFp("1e-1", false, false, 1L, -1, 4);
        testReadFp("1e-10", false, false, 1L, -10, 5);
        testReadFp("1e-100", false, false, 1L, -100, 6);
        testReadFp("1.5e-3", false, false, 15L, -4, 6);
        testReadFp("1.5e-10", false, false, 15L, -11, 7);
        testReadFp("1e+1", false, false, 1L, 1, 4);
        testReadFp("1e+10", false, false, 1L, 10, 5);
        testReadFp("1.5e+2", false, false, 15L, 1, 6);
        testReadFp("-1e+1", true, false, 1L, 1, 5);
        testReadFp("1E1", false, false, 1L, 1, 3);
        testReadFp("1E10", false, false, 1L, 10, 4);
        testReadFp("1.5E-3", false, false, 15L, -4, 6);
        testReadFp("1.5E308", false, false, 15L, 307, 7);
        testReadFp("1e007", false, false, 1L, 7, 5);
        testReadFp("1e0007", false, false, 1L, 7, 6);
        testReadFp("1e00007", false, false, 1L, 7, 7);
        testReadFp("1e0", false, false, 1L, 0, 3);
        testReadFp("1e00", false, false, 1L, 0, 4);
        testReadFp("1e000", false, false, 1L, 0, 5);
        testReadFp("1.5e003", false, false, 15L, 2, 7);
        testReadFp("1.5e-003", false, false, 15L, -4, 8);
        testReadFp("-1e007", true, false, 1L, 7, 6);
        testReadFp("1E007", false, false, 1L, 7, 5);
        testReadFp("1e+007", false, false, 1L, 7, 6);
        testReadFp("1e-007", false, false, 1L, -7, 6);
    }

    @Test
    public void readFpBoundary() {
        testReadFp("3.4028235E38", false, false, 34028235L, 31, 12);
        testReadFp("1.4E-45", false, false, 14L, -46, 7);
        testReadFp("1.7976931348623157E308", false, false, 17976931348623157L, 292, 22);
        testReadFp("2.2250738585072014e-308", false, false, 22250738585072014L, -324, 23);
    }

    @Test
    public void readWrongFpTest() {
        List<String> strList = List.of(
                "--123", "-+123", "+123", "++123", "+-123", "- ", "-",
                "00", "000", "01", "001", "0123", "-00", "-000", "-01", "-001", "-0123",
                ".", ".1", "-.1", "1.", "-1.", "0.", "-0.", "1.a", "1.e", "1.e1", "1.+1",
                "0..", "1..2", "1e", "1e+", "1e+a", "1e--2", "1e+-2", "1ee2"
        );
        for (String str : strList) {
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(str.getBytes(StandardCharsets.US_ASCII));
            byte firstByte = heapReadBuffer.readByte();
            Assertions.assertThrows(JsonDeserializerException.class, () -> JsonNumberUtil.readFpStrRep(heapReadBuffer, MAX_FP_SIZE, firstByte), "str : " + str);
        }
    }
}
