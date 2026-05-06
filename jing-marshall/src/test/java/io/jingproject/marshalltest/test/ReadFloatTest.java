package io.jingproject.marshalltest.test;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.SegmentReadBuffer;
import io.jingproject.common.SegmentWriteBuffer;
import io.jingproject.marshall.MarshallUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.foreign.Arena;
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
            float f1 = MarshallUtil.readFloat(heapReadBuffer);
            Assertions.assertEquals(Float.floatToRawIntBits(f), Float.floatToRawIntBits(f1));

            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(MemorySegment.ofArray(bytes));
            float f2 = MarshallUtil.readFloat(segmentReadBuffer);
            Assertions.assertEquals(Float.floatToRawIntBits(f), Float.floatToRawIntBits(f2));
        }
    }

    private static void doubleToStringTest(List<String> strList) {
        for (String str : strList) {
            double f = Double.parseDouble(str);
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            double f1 = MarshallUtil.readDouble(heapReadBuffer);
            Assertions.assertEquals(Double.doubleToRawLongBits(f), Double.doubleToRawLongBits(f1));

            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(MemorySegment.ofArray(bytes));
            double f2 = MarshallUtil.readDouble(segmentReadBuffer);
            Assertions.assertEquals(Double.doubleToRawLongBits(f), Double.doubleToRawLongBits(f2));
        }
    }

    @Test
    public void readTargetFloatTest() {
        List<String> strList = List.of("3.4028235E38", "1.4E-45", "0.0", "-0.0");
        for (String str : strList) {
            float f = Float.parseFloat(str);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(str.getBytes(StandardCharsets.US_ASCII));
            float f1 = MarshallUtil.readFloat(heapReadBuffer);
            Assertions.assertEquals(Float.floatToRawIntBits(f), Float.floatToRawIntBits(f1));
        }
    }

    @Test
    public void readRandomFloatTest() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<String> strList = new ArrayList<>();
        for(int i = 0; i < BATCH; ) {
            float f = Float.intBitsToFloat(random.nextInt());
            if(Float.isFinite(f)) {
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
        final int BATCH_SIZE   = 100;
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
        List<String> strList = List.of("1.7976931348623157E308", "4.9E-324", "0.0", "-0.0");
        for (String str : strList) {
            double f = Double.parseDouble(str);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(str.getBytes(StandardCharsets.US_ASCII));
            double f1 = MarshallUtil.readDouble(heapReadBuffer);
            Assertions.assertEquals(Double.doubleToRawLongBits(f), Double.doubleToRawLongBits(f1));
        }
    }

    @Test
    public void readRandomDoubleTest() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<String> strList = new ArrayList<>();
        for(int i = 0; i < BATCH; ) {
            double f = Double.longBitsToDouble(random.nextLong());
            if(Double.isFinite(f)) {
                strList.add(Double.toString(f));
                i++;
            }
        }
        doubleToStringTest(strList);
    }
}
