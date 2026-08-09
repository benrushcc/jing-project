package io.jingproject.marshalljsontest.test;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.SegmentWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshalljson.JsonNumberUtil;
import io.jingproject.marshalljsontest.NumberUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class WriteIntegerTest {
    private static final int BUFFER_SIZE = 32;

    private static final int RANGE = 65535;

    private static void testWriteInt(BiConsumer<Integer, WriteBuffer> biConsumer) {
        List<Integer> list = new ArrayList<>();
        for (int i = Integer.MIN_VALUE; i <= Integer.MIN_VALUE + RANGE; i++) {
            list.add(i);
        }
        for (int i = -RANGE; i <= RANGE; i++) {
            list.add(i);
        }
        for (int i = Integer.MAX_VALUE - RANGE; i < Integer.MAX_VALUE; i++) {
            list.add(i);
        }
        list.add(Integer.MAX_VALUE);
        try (Arena arena = Arena.ofConfined()) {
            HeapWriteBuffer heapWriteBuffer = new HeapWriteBuffer(BUFFER_SIZE);
            SegmentWriteBuffer segmentWriteBuffer = new SegmentWriteBuffer(arena, BUFFER_SIZE);
            for (Integer i : list) {
                biConsumer.accept(i, heapWriteBuffer);
                byte[] heapArray = heapWriteBuffer.toByteArray();
                heapWriteBuffer.reset();
                Assertions.assertEquals(String.valueOf(i), new String(heapArray));

                biConsumer.accept(i, segmentWriteBuffer);
                byte[] segmentArray = segmentWriteBuffer.toByteArray();
                segmentWriteBuffer.reset();
                Assertions.assertEquals(String.valueOf(i), new String(segmentArray));
            }
        }
    }

    private static void testWriteLong(BiConsumer<Long, WriteBuffer> biConsumer) {
        List<Long> list = new ArrayList<>();
        for(long l = Long.MIN_VALUE; l <= Long.MIN_VALUE + RANGE; l++) {
            list.add(l);
        }
        for(long l = -RANGE; l <= RANGE; l++) {
            list.add(l);
        }
        for(long l = Long.MAX_VALUE - RANGE; l < Long.MAX_VALUE; l++) {
            list.add(l);
        }
        list.add(Long.MAX_VALUE);
        try (Arena arena = Arena.ofConfined()) {
            HeapWriteBuffer heapWriteBuffer = new HeapWriteBuffer(BUFFER_SIZE);
            SegmentWriteBuffer segmentWriteBuffer = new SegmentWriteBuffer(arena, BUFFER_SIZE);
            for (Long l : list) {
                biConsumer.accept(l, heapWriteBuffer);
                byte[] heapArray = heapWriteBuffer.toByteArray();
                heapWriteBuffer.reset();
                Assertions.assertEquals(String.valueOf(l), new String(heapArray));

                biConsumer.accept(l, segmentWriteBuffer);
                byte[] segmentArray = segmentWriteBuffer.toByteArray();
                segmentWriteBuffer.reset();
                Assertions.assertEquals(String.valueOf(l), new String(segmentArray));
            }
        }
    }

    @Test
    public void jdkWriteIntTest() {
        testWriteInt(NumberUtil::writeInt0);
        testWriteLong(NumberUtil::writeLong0);
    }

    @Test
    public void singleWriteIntTest() {
        testWriteInt(NumberUtil::writeInt1);
        testWriteLong(NumberUtil::writeLong1);
    }

    @Test
    public void lutWriteIntTest() {
        testWriteInt(NumberUtil::writeInt2);
        testWriteLong(NumberUtil::writeLong2);
    }

    @Test
    public void lutLoopWriteIntTest() {
        testWriteInt(NumberUtil::writeInt3);
        testWriteLong(NumberUtil::writeLong3);
    }

    @Test
    public void writeIntTest() {
        testWriteInt(JsonNumberUtil::writeInt);
        testWriteLong(JsonNumberUtil::writeLong);
    }
}
