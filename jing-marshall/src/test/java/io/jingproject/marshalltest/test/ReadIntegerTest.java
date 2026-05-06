package io.jingproject.marshalltest.test;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.marshall.MarshallUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class ReadIntegerTest {

    private static final int RANGE = 65535;

    @Test
    public void readByteTest() {
        for(int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte[] bytes = String.valueOf(i).getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            byte b = MarshallUtil.readByte(heapReadBuffer);
            Assertions.assertEquals(i, b);
        }
    }

    @Test
    public void readByteOverflowTest() {
        String[] overflowValues = {
                "128",
                "-129",
                "1000",
                "-1000"
        };
        for (String str : overflowValues) {
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            Assertions.assertThrows(ArithmeticException.class, () -> MarshallUtil.readByte(heapReadBuffer));
        }
    }

    @Test
    public void readShortTest() {
        for(int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
            byte[] bytes = String.valueOf(i).getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            short s = MarshallUtil.readShort(heapReadBuffer);
            Assertions.assertEquals(i, s);
        }
    }

    @Test
    public void readShortOverflowTest() {
        String[] overflowValues = {
                "32768",
                "-32769",
                "100000",
                "-100000"
        };
        for (String str : overflowValues) {
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            Assertions.assertThrows(ArithmeticException.class, () -> MarshallUtil.readShort(heapReadBuffer));
        }
    }

    @Test
    public void readIntTest() {
        Stream.of(IntStream.range(Integer.MIN_VALUE, Integer.MIN_VALUE + RANGE),
                IntStream.range(-RANGE, RANGE),
                IntStream.range(Integer.MAX_VALUE - RANGE, Integer.MAX_VALUE))
                .flatMapToInt(Function.identity()).forEach(i -> {
                    byte[] bytes = String.valueOf(i).getBytes(StandardCharsets.US_ASCII);
                    HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
                    int i1 = MarshallUtil.readInt(heapReadBuffer);
                    Assertions.assertEquals(i, i1);
                });
    }

    @Test
    public void readIntOverflowTest() {
        String[] overflowValues = {
                "2147483648",
                "-2147483649",
                "9999999999",
                "-9999999999",
        };
        for (String str : overflowValues) {
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            Assertions.assertThrows(ArithmeticException.class, () -> MarshallUtil.readInt(heapReadBuffer));
        }
    }

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
