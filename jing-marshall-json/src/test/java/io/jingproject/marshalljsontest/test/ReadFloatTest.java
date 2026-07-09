package io.jingproject.marshalljsontest.test;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.SegmentReadBuffer;
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

    private static void floatToStringTest(List<String> strList) {
        for (String str : strList) {
            float f = Float.parseFloat(str);
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            byte fb1 = heapReadBuffer.readByte();
            float f1 = JsonNumberUtil.readFloat(heapReadBuffer, fb1);
            Assertions.assertEquals(Float.floatToRawIntBits(f), Float.floatToRawIntBits(f1), "failed, heap float : " + str);

            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(MemorySegment.ofArray(bytes));
            byte fb2 = segmentReadBuffer.readByte();
            float f2 = JsonNumberUtil.readFloat(segmentReadBuffer, fb2);
            Assertions.assertEquals(Float.floatToRawIntBits(f), Float.floatToRawIntBits(f2), "failed, segment float : " + str);
        }
    }

    private static void doubleToStringTest(List<String> strList) {
        for (String str : strList) {
            double f = Double.parseDouble(str);
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            byte fb1 = heapReadBuffer.readByte();
            double f1 = JsonNumberUtil.readDouble(heapReadBuffer, fb1);
            Assertions.assertEquals(Double.doubleToRawLongBits(f), Double.doubleToRawLongBits(f1), "failed, heap double : " + str);

            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(MemorySegment.ofArray(bytes));
            byte fb2 = segmentReadBuffer.readByte();
            double f2 = JsonNumberUtil.readDouble(segmentReadBuffer, fb2);
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
            float f1 = JsonNumberUtil.readFloat(heapReadBuffer, fb1);
            Assertions.assertEquals(Float.floatToRawIntBits(f), Float.floatToRawIntBits(f1));

            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(MemorySegment.ofArray(bytes));
            byte fb2 = segmentReadBuffer.readByte();
            float f2 = JsonNumberUtil.readFloat(segmentReadBuffer, fb2);
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
            double f1 = JsonNumberUtil.readDouble(heapReadBuffer, firstByte);
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

    @Test
    public void parseTargetFpFormatTest() {
        String str = "12.3.4"; // no exception thrown, only 12.3 would be parsed
        HeapReadBuffer heapReadBuffer = new HeapReadBuffer(str.getBytes(StandardCharsets.US_ASCII));
        byte firstByte = heapReadBuffer.readByte();
        Assertions.assertNotNull(JsonNumberUtil.parseFpStrRep(heapReadBuffer, firstByte));
    }

    @Test
    public void parseWrongFpFormatTest() {
        List<String> strList = List.of(
                "--123", "-+123", "+123", "++123", "+-123", "- ", "-",
                "00", "000", "01", "001", "0123", "-00", "-000", "-01", "-001", "-0123",
                ".", ".1", "-.1", "1.", "-1.", "0.", "-0.", "1.a", "1.e", "1.e1", "1.+1",
                "0..", "1..2", "1e", "1e+", "1e+a", "1e--2", "1e+-2", "1ee2"
        );
        for (String str : strList) {
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(str.getBytes(StandardCharsets.US_ASCII));
            byte firstByte = heapReadBuffer.readByte();
            Assertions.assertThrows(NumberFormatException.class, () -> JsonNumberUtil.parseFpStrRep(heapReadBuffer, firstByte), "str : " + str);
        }
    }
}
