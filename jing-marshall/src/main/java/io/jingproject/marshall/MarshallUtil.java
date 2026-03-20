package io.jingproject.marshall;

import io.jingproject.common.ArrayAccess;
import io.jingproject.common.anno.ProcessorApi;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

@ProcessorApi
public final class MarshallUtil {

    private MarshallUtil() {
        throw new AssertionError();
    }

    private static int checkAccess(byte[] bytes, int offset, int len) {
        int size = bytes.length;
        if((size | offset | len) < 0 || size - offset < len) {
            throw new IllegalArgumentException();
        }
        return len + offset;
    }

    private static long checkAccess(MemorySegment segment, long offset, long len) {
        long size = segment.byteSize();
        // 先检查正负性，确保都是正数，然后减法运算就不可能产生溢出了
        if((size | offset | len) < 0L || size - offset < len) {
            throw new IllegalArgumentException();
        }
        // len + offset的范围一定在size之内，所以这个地方也不可能溢出
        return len + offset;
    }

    public static int hash(byte[] bytes, int offset, int len) {
        checkAccess(bytes, offset, len);
        int h = len;
        while (len >= 4) {
            int v = ArrayAccess.getInt(bytes, offset, ByteOrder.LITTLE_ENDIAN);
            h = (h ^ v) * 31;
            offset += 4; len -= 4;
        }
        while (len > 0) {
            int v = Byte.toUnsignedInt(bytes[offset]);
            h = (h ^ v) * 31;
            offset += 1;
            len -= 1;
        }
        return h;
    }

    public static byte parseByte(MemorySegment segment, long offset, long len) {
        int r = parseInt(segment, offset, len);
        if(r instanceof byte b) {
            return b;
        }
        throw new ArithmeticException("byte overflow");
    }

    public static short parseShort(MemorySegment segment, long offset, long len) {
        int r = parseInt(segment, offset, len);
        if(r instanceof short s) {
            return s;
        }
        throw new ArithmeticException("short overflow");
    }

    // 算术溢出是ArithmeticException，如果是数据有问题就是NumberFormatException
    public static int parseInt(MemorySegment segment, long offset, long len) {
        assert segment != null;
        long end = checkAccess(segment, offset, len);
        byte firstByte = segment.get(ValueLayout.JAVA_BYTE, offset);
        boolean negative = false;
        if(firstByte == '-') {
            negative = true;
            offset = Math.incrementExact(offset);
        }
        int r = 0;
        for(long index = offset; index < end; index++) {
            byte b = segment.get(ValueLayout.JAVA_BYTE, index);
            if(b < '0' || b > '9') {
                throw new NumberFormatException("invalid byte : " + b);
            }
            int current = b - '0';
            r = Math.subtractExact(Math.multiplyExact(r, 10), current);
        }
        if(negative) {
            return r;
        }
        if(r == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        return -r;
    }

    public static long parseLong(MemorySegment segment, long offset, long len) {
        assert segment != null;
        long end = checkAccess(segment, offset, len);
        byte firstByte = segment.get(ValueLayout.JAVA_BYTE, offset);
        boolean negative = false;
        if(firstByte == '-') {
            negative = true;
            offset = Math.incrementExact(offset);
        }
        long r = 0L;
        for(long index = offset; index < end; index++) {
            byte b = segment.get(ValueLayout.JAVA_BYTE, index);
            if(b < '0' || b > '9') {
                throw new NumberFormatException("invalid byte : " + b);
            }
            long current = b - '0';
            r = Math.subtractExact(Math.multiplyExact(r, 10L), current);
        }
        if(negative) {
            return r;
        }
        if(r == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        return -r;
    }

    private record Spec(int significandBits, int expBits, int bias) {

    }

    private static final Spec FLOAT_SPEC = new Spec(23, 8, -127);
    private static final Spec DOUBLE_SPEC = new Spec(52, 11, -1023);

    private static final class Dec {
        // CAP不应该被更改，这是一个经过验证的，可以覆盖所有浮点值的固定大小
        private static final int CAP = 800;
        private final byte[] d = new byte[CAP];
        private int nd = 0;
        private int dp = 0;
        private boolean neg = false;
        private boolean trunc = false;

        public void init() {

        }
    }

    private static double buildDouble(boolean neg, long significand, long exp) {
        long r = significand & ((1L << DOUBLE_SPEC.significandBits()) - 1L);
        r |= ((exp - DOUBLE_SPEC.bias()) & ((1L << DOUBLE_SPEC.expBits()) - 1L)) << DOUBLE_SPEC.significandBits();
        if(neg) {
            r |= 1L << (DOUBLE_SPEC.significandBits() + DOUBLE_SPEC.expBits());
        }
        return Double.longBitsToDouble(r);
    }

    public static float parseFloat(MemorySegment segment, long offset, long len) {
        assert segment != null;
        checkAccess(segment, offset, len);
        // TODO
        return Float.NaN;
    }

    public static double parseDouble(MemorySegment segment, long offset, long len) {
        assert segment != null;
        checkAccess(segment, offset, len);
        byte firstByte = segment.get(ValueLayout.JAVA_BYTE, offset);
        // TODO
        return Double.NaN;
    }

}
