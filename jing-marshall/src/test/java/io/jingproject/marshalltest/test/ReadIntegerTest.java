package io.jingproject.marshalltest.test;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.marshall.MarshallUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class ReadIntegerTest {

    private static final int RANGE = 1024 * 1024;

    @Test
    public void readLongTest() {
        Stream.of(LongStream.range(Long.MIN_VALUE, Long.MIN_VALUE + RANGE),
                        LongStream.range(-RANGE, RANGE),
                        LongStream.range(Long.MAX_VALUE - RANGE, Long.MAX_VALUE))
                .flatMapToLong(Function.identity()).forEach(l -> {
                    byte[] bytes = String.valueOf(l).getBytes(StandardCharsets.US_ASCII);
                    HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
                    long l1 = MarshallUtil.readLong(heapReadBuffer);
                    Assertions.assertEquals(l, l1);
                });
    }

    @Test
    public void readLongOverflowTest() {
        String[] overflowValues = {
                "9223372036854775808",
                "-9223372036854775809",
                "99999999999999999999",
                "-99999999999999999999",
                "100000000000000000000",
                "-100000000000000000000"
        };
        for (String str : overflowValues) {
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            Assertions.assertThrows(ArithmeticException.class, () -> MarshallUtil.readLong(heapReadBuffer));
        }
    }

    @Test
    public void readLongWrongFormatTest() {
        String[] wrongFormatValues = {
                "-0",
                "0123",
                "a12",
                "-0123",
                "-a12"
        };
        for (String str : wrongFormatValues) {
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            Assertions.assertThrows(NumberFormatException.class, () -> MarshallUtil.readLong(heapReadBuffer));
        }
    }
}
