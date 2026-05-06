package io.jingproject.marshalltest.test;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.SegmentWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class WriteIntegerTest {
    private static final int BUFFER_SIZE = 32;
    private static final int OFFSET = 65535;

    private static void testWriteInt(BiConsumer<Integer, WriteBuffer> biConsumer) {
        try (Arena arena = Arena.ofConfined()) {
            HeapWriteBuffer heapWriteBuffer = new HeapWriteBuffer(BUFFER_SIZE);
            SegmentWriteBuffer segmentWriteBuffer = new SegmentWriteBuffer(arena, BUFFER_SIZE);
            Stream.of(IntStream.range(Integer.MIN_VALUE, Integer.MIN_VALUE + OFFSET),
                            IntStream.range(-OFFSET, OFFSET),
                            IntStream.range(Integer.MAX_VALUE - OFFSET, Integer.MAX_VALUE))
                    .flatMapToInt(Function.identity()).forEach(i -> {
                        biConsumer.accept(i, heapWriteBuffer);
                        byte[] heapArray = heapWriteBuffer.toByteArray();
                        heapWriteBuffer.reset();
                        Assertions.assertEquals(String.valueOf(i), new String(heapArray));

                        biConsumer.accept(i, segmentWriteBuffer);
                        byte[] segmentArray = segmentWriteBuffer.toByteArray();
                        segmentWriteBuffer.reset();
                        Assertions.assertEquals(String.valueOf(i), new String(segmentArray));
                    });
        }
    }

    private static void testWriteLong(BiConsumer<Long, WriteBuffer> biConsumer) {
        try (Arena arena = Arena.ofConfined()) {
            HeapWriteBuffer heapWriteBuffer = new HeapWriteBuffer(BUFFER_SIZE);
            SegmentWriteBuffer segmentWriteBuffer = new SegmentWriteBuffer(arena, BUFFER_SIZE);
            Stream.of(LongStream.range(Long.MIN_VALUE, Long.MIN_VALUE + OFFSET),
                            LongStream.range(-OFFSET, OFFSET),
                            LongStream.range(Long.MAX_VALUE - OFFSET, Long.MAX_VALUE))
                    .flatMapToLong(Function.identity()).forEach(i -> {
                        biConsumer.accept(i, heapWriteBuffer);
                        byte[] heapArray = heapWriteBuffer.toByteArray();
                        heapWriteBuffer.reset();
                        Assertions.assertEquals(String.valueOf(i), new String(heapArray));

                        biConsumer.accept(i, segmentWriteBuffer);
                        byte[] segmentArray = segmentWriteBuffer.toByteArray();
                        segmentWriteBuffer.reset();
                        Assertions.assertEquals(String.valueOf(i), new String(segmentArray));
                    });
        }
    }

    @Test
    public void jdkWriteIntTest() {
        testWriteInt(MarshallUtil::writeInt0);
        testWriteLong(MarshallUtil::writeLong0);
    }

    @Test
    public void singleWriteIntTest() {
        testWriteInt(MarshallUtil::writeInt1);
        testWriteLong(MarshallUtil::writeLong1);
    }

    @Test
    public void lutWriteIntTest() {
        testWriteInt(MarshallUtil::writeInt2);
        testWriteLong(MarshallUtil::writeLong2);
    }

    @Test
    public void thWriteIntTest() {
        testWriteInt(MarshallUtil::writeInt);
        testWriteLong(MarshallUtil::writeLong);
    }
}
