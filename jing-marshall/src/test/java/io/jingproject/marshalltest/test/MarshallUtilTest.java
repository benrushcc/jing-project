package io.jingproject.marshalltest.test;

import io.jingproject.marshall.MarshallUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

public class MarshallUtilTest {

    private static final int RANGE = 2000;

    @Test
    public void parseByteTest() {
        for(int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            String str = String.valueOf(i);
            MemorySegment seg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            byte b = MarshallUtil.parseByte(seg, 0L, seg.byteSize());
            Assertions.assertEquals(i, b, () -> "parse byte failed");
        }
    }

    @Test
    public void parseByteOverflowTest() {
        String[] overflowValues = {
                "128",
                "-129",
                "1000",
                "-1000"
        };

        for (String str : overflowValues) {
            MemorySegment seg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(ArithmeticException.class, () -> {
                MarshallUtil.parseByte(seg, 0L, seg.byteSize());
            }, () -> "parse byte should overflow for: " + str);
        }
    }

    @Test
    public void parseShortTest() {
        for(int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
            String str = String.valueOf(i);
            MemorySegment seg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            short s = MarshallUtil.parseShort(seg, 0L, seg.byteSize());
            Assertions.assertEquals(i, s, () -> "parse short failed");
        }
    }

    @Test
    public void parseShortOverflowTest() {
        String[] overflowValues = {
                "32768",
                "-32769",
                "100000",
                "-100000"
        };

        for (String str : overflowValues) {
            MemorySegment seg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(ArithmeticException.class, () -> {
                MarshallUtil.parseShort(seg, 0L, seg.byteSize());
            }, () -> "parse short should overflow for: " + str);
        }
    }

    @Test
    public void parseIntNormalTest() {
        for(int i = -1000; i < 1000; i++) {
            String str = String.valueOf(i);
            MemorySegment seg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            int r = MarshallUtil.parseInt(seg, 0L, seg.byteSize());
            Assertions.assertEquals(i, r, () -> "parse normal int failed : " + str);
        }
    }

    @Test
    public void parseIntPositiveTest() {
        for(int i = Integer.MAX_VALUE; i > Math.subtractExact(Integer.MAX_VALUE, RANGE); i--) {
            String str = String.valueOf(i);
            MemorySegment seg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            int r = MarshallUtil.parseInt(seg, 0L, seg.byteSize());
            Assertions.assertEquals(i, r, () -> "parse large int failed : " + str);
        }
    }

    @Test
    public void parseIntNegativeTest() {
        for(int i = Integer.MIN_VALUE; i < Math.addExact(Integer.MIN_VALUE, RANGE); i++) {
            String str = String.valueOf(i);
            MemorySegment seg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            int r = MarshallUtil.parseInt(seg, 0L, seg.byteSize());
            Assertions.assertEquals(i, r, () -> "parse small int failed : " + str);
        }
    }

    @Test
    public void parseIntOverflowTest() {
        String[] overflowValues = {
                "2147483648",
                "-2147483649",
                "9999999999",
                "-9999999999",
        };

        for (String str : overflowValues) {
            MemorySegment seg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(ArithmeticException.class, () -> {
                MarshallUtil.parseInt(seg, 0L, seg.byteSize());
            }, () -> "parse int should overflow for: " + str);
        }
    }

    @Test
    public void parseLongNormalTest() {
        for(long i = -1000L; i < 1000L; i++) {
            String str = String.valueOf(i);
            MemorySegment seg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            long r = MarshallUtil.parseLong(seg, 0L, seg.byteSize());
            Assertions.assertEquals(i, r, () -> "parse normal long failed : " + str);
        }
    }

    @Test
    public void parseLongPositiveTest() {
        for(long i = Long.MAX_VALUE; i > Math.subtractExact(Long.MAX_VALUE, RANGE); i--) {
            String str = String.valueOf(i);
            MemorySegment seg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            long r = MarshallUtil.parseLong(seg, 0L, seg.byteSize());
            Assertions.assertEquals(i, r, () -> "parse large long failed : " + str);
        }
    }

    @Test
    public void parseLongNegativeTest() {
        for(long i = Long.MIN_VALUE; i < Math.addExact(Long.MIN_VALUE, RANGE); i++) {
            String str = String.valueOf(i);
            MemorySegment seg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            long r = MarshallUtil.parseLong(seg, 0L, seg.byteSize());
            Assertions.assertEquals(i, r, () -> "parse small long failed : " + str);
        }
    }

    @Test
    public void parseLongOverflowTest() {
        String[] overflowValues = {
                "9223372036854775808",
                "-9223372036854775809",
                "99999999999999999999",
                "-99999999999999999999",
                "100000000000000000000",
                "-100000000000000000000"
        };

        for (String str : overflowValues) {
            MemorySegment seg = MemorySegment.ofArray(str.getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(ArithmeticException.class, () -> {
                MarshallUtil.parseLong(seg, 0L, seg.byteSize());
            }, () -> "parse long should overflow for: " + str);
        }
    }
}
