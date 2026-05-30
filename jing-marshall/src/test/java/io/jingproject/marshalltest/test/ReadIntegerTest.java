package io.jingproject.marshalltest.test;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.marshall.MarshallUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ReadIntegerTest {

    private static final int RANGE = 1024 * 1024;

    @Test
    public void readIntTest() {
        List<String> stringList = new ArrayList<>();
        for (int i = Integer.MIN_VALUE; i <= Integer.MIN_VALUE + RANGE; i++) {
            stringList.add(String.valueOf(i));
        }
        for (int i = -RANGE; i <= RANGE; i++) {
            stringList.add(String.valueOf(i));
        }
        for (int i = Integer.MAX_VALUE - RANGE; i < Integer.MAX_VALUE; i++) {
            stringList.add(String.valueOf(i));
        }
        stringList.add(String.valueOf(Integer.MAX_VALUE));
        for (String s : stringList) {
            int expected = Integer.parseInt(s);
            byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            int actual = MarshallUtil.readInt(heapReadBuffer);
            Assertions.assertEquals(expected, actual);
        }
    }

    @Test
    public void readIntOverflowTest() {
        String[] overflowValues = {
                "2147483648",        // Integer.MAX_VALUE + 1
                "-2147483649",       // Integer.MIN_VALUE - 1
                "9999999999",
                "-9999999999",
                "10000000000",
                "-10000000000"
        };
        for (String str : overflowValues) {
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            Assertions.assertThrows(ArithmeticException.class, () -> MarshallUtil.readInt(heapReadBuffer));
        }
    }

    @Test
    public void readIntWrongFormatTest() {
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
            Assertions.assertThrows(NumberFormatException.class, () -> MarshallUtil.readInt(heapReadBuffer));
        }
    }

    @Test
    public void readLongTest() {
        List<String> stringList = new ArrayList<>();
        for(long l = Long.MIN_VALUE; l <= Long.MIN_VALUE + RANGE; l++) {
            stringList.add(String.valueOf(l));
        }
        for(long l = -RANGE; l <= RANGE; l++) {
            stringList.add(String.valueOf(l));
        }
        for(long l = Long.MAX_VALUE - RANGE; l < Long.MAX_VALUE; l++) {
            stringList.add(String.valueOf(l));
        }
        stringList.add(String.valueOf(Long.MAX_VALUE));
        for (String s : stringList) {
            long l = Long.parseLong(s);
            byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            long l1 = MarshallUtil.readLong(heapReadBuffer);
            Assertions.assertEquals(l, l1);
        }
    }

    @Test
    public void readLongOverflowTest() {
        String[] overflowValues = {
                "9223372036854775808", // Long.MAX_VALUE + 1
                "-9223372036854775809", // Long.MIN_VALUE - 1
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
