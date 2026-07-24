package io.jingproject.marshalljsontest.test;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.SegmentWriteBuffer;
import io.jingproject.marshalljson.JsonNumberUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.foreign.Arena;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class WriteFloatTest {
    private static final int BUFFER_SIZE = 32;
    private static final int BATCH = 1000000;

    private static void stringToFloatTest(List<String> strList) {
        try (Arena arena = Arena.ofConfined()) {
            HeapWriteBuffer heapWriteBuffer = new HeapWriteBuffer(BUFFER_SIZE);
            SegmentWriteBuffer segmentWriteBuffer = new SegmentWriteBuffer(arena, BUFFER_SIZE);
            for (String str : strList) {
                float f = Float.parseFloat(str);
                JsonNumberUtil.writeFloat(f, heapWriteBuffer);
                String heapStr = new String(heapWriteBuffer.toByteArray(), StandardCharsets.UTF_8);
                heapWriteBuffer.reset();
                checkFloatString(str, heapStr);

                JsonNumberUtil.writeFloat(f, segmentWriteBuffer);
                String segmentStr = new String(segmentWriteBuffer.toByteArray(), StandardCharsets.UTF_8);
                segmentWriteBuffer.reset();
                checkFloatString(str, segmentStr);
            }
        }
    }

    private static void stringToDoubleTest(List<String> strList) {
        try (Arena arena = Arena.ofConfined()) {
            HeapWriteBuffer heapWriteBuffer = new HeapWriteBuffer(BUFFER_SIZE);
            SegmentWriteBuffer segmentWriteBuffer = new SegmentWriteBuffer(arena, BUFFER_SIZE);
            for (String str : strList) {
                double f = Double.parseDouble(str);
                JsonNumberUtil.writeDouble(f, heapWriteBuffer);
                String heapStr = new String(heapWriteBuffer.toByteArray(), StandardCharsets.UTF_8);
                heapWriteBuffer.reset();
                checkDoubleString(str, heapStr);

                JsonNumberUtil.writeDouble(f, segmentWriteBuffer);
                String segmentStr = new String(segmentWriteBuffer.toByteArray(), StandardCharsets.UTF_8);
                segmentWriteBuffer.reset();
                checkDoubleString(str, segmentStr);
            }
        }
    }

    private static void checkFloatString(String expected, String actual) {
        if (!expected.equals(actual)) {
            float f1 = Float.parseFloat(expected);
            float f2 = Float.parseFloat(actual);
            if (Float.floatToRawIntBits(f1) != Float.floatToRawIntBits(f2)) {
                Assertions.fail("Float values do not match, expected " + expected + ", actual " + actual);
            }
        }
    }

    private static void checkDoubleString(String expected, String actual) {
        if (!expected.equals(actual)) {
            double f1 = Double.parseDouble(expected);
            double f2 = Double.parseDouble(actual);
            if (Double.doubleToRawLongBits(f1) != Double.doubleToRawLongBits(f2)) {
                Assertions.fail("Double values do not match, expected " + expected + ", actual " + actual);
            }
        }
    }

    @Test
    public void writeZeroFloatTest() {
        List<String> strList = List.of("0", "0.0", "-0", "-0.0");
        stringToFloatTest(strList);
    }

    @Test
    public void writeTargetFloatTest() {
        List<String> strList = List.of("-1.4E-45", "1.0");
        stringToFloatTest(strList);
    }

    @Test
    public void writeZeroDoubleTest() {
        List<String> strList = List.of("0", "0.0", "-0", "-0.0");
        stringToDoubleTest(strList);
    }

    @Test
    public void writeTargetDoubleTest() {
        List<String> strList = List.of("4.9E-324");
        stringToDoubleTest(strList);
    }

    @Test
    public void writeRandomFloatTest() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<String> strList = new ArrayList<>();
        for (int i = 0; i < BATCH; ) {
            float f = Float.intBitsToFloat(random.nextInt());
            if (Float.isFinite(f)) {
                strList.add(Float.toString(f));
                i++;
            }
        }
        stringToFloatTest(strList);
    }

    @Test
    @Tag("exhaustive")
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    public void writeAllFloatTest() {
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
                                stringToFloatTest(batch);
                                batch.clear();
                            }
                        }
                    }
                    if (!batch.isEmpty()) {
                        stringToFloatTest(batch);
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
    public void writeRandomDoubleTest() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<String> strList = new ArrayList<>();
        for (int i = 0; i < BATCH; ) {
            double f = Double.longBitsToDouble(random.nextLong());
            if (Double.isFinite(f)) {
                strList.add(Double.toString(f));
                i++;
            }
        }
        stringToDoubleTest(strList);
    }
}
