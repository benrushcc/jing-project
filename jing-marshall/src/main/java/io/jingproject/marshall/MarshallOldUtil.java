package io.jingproject.marshall;

import io.jingproject.common.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public final class MarshallOldUtil {

    // byte constants
    public static final byte BYTE_ZERO = (byte) '0';
    public static final byte BYTE_NINE = (byte) '9';
    public static final byte BYTE_MINUS = (byte) '-';
    public static final byte BYTE_PLUS = (byte) '+';
    public static final byte BYTE_PERIOD = (byte) '.';
    public static final byte BYTE_e = (byte) 'e';
    public static final byte BYTE_E = (byte) 'E';
    public static final byte BYTE_a = (byte) 'a';
    public static final byte BYTE_z = (byte) 'z';
    public static final byte BYTE_A = (byte) 'A';
    public static final byte BYTE_Z = (byte) 'Z';

    // integer to string constants
    private static final byte[] MIN_INT_BYTES = String.valueOf(Integer.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MIN_LONG_BYTES = String.valueOf(Long.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final short[] ITOA_LUT_TABLE = makeItoaLutTable();
    private static final int[] INT_LEN_TABLE = makeIntLenTable();
    private static final int[] INT_POW_TABLE = makeIntPowTable();
    private static final int[] LONG_LEN_TABLE = makeLongLenTable();
    private static final long[] LONG_POW_TABLE = makeLongPowTable();

    // float to string constants
    private static final int MAX_FLOAT_CAPACITY = 15; // same as MAX_CHARS in jdk/internal/math/FloatToDecimal.java
    private static final int MAX_DOUBLE_CAPACITY = 24; // same as MAX_CHARS in jdk/internal/math/DoubleToDecimal.java
    private static final short NEG_ZERO = Utils.compact(BYTE_MINUS, BYTE_ZERO);
    private static final short ZERO_PERIOD = Utils.compact(BYTE_ZERO, BYTE_PERIOD);
    private static final short E_MINUS = Utils.compact(BYTE_E, BYTE_MINUS);
    private static final int POW10MIN = -348;
    private static final int POW10MAX = 347;
    private static final long[] POW10TAB = makePow10Table(); // huge table
    private static final FpSpec FLOAT_SPEC = new FpSpec(23, 8, -127, -189, 38, -45);
    private static final FpSpec DOUBLE_SPEC = new FpSpec(52, 11, -1023, -1085, 308, -324);

    // string to integer constants
    private static final byte[] ZERO_NINE_TABLE = makeZeroNineTable();

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // no overflow
    private static short[] makeItoaLutTable() {
        short[] r = new short[100];
        for (int i = 0; i < 100; i++) {
            byte b0 = (byte) (BYTE_ZERO + (i / 10));
            byte b1 = (byte) (BYTE_ZERO + (i % 10));
            r[i] = Utils.compact(b0, b1);
        }
        return r;
    }

    // no overflow
    private static int[] makeIntLenTable() {
        int[] r = new int[32];
        for (int i = 1; i <= 31; i++) {
            int maxNum = (1 << (32 - i)) - 1;
            r[i] = String.valueOf(maxNum).getBytes(StandardCharsets.US_ASCII).length;
        }
        return r;
    }

    // no overflow
    private static int[] makeIntPowTable() {
        int[] r = new int[11];
        for (int i = 2; i <= 10; i++) {
            r[i] = Math.powExact(10, i - 1);
        }
        return r;
    }

    // no overflow
    private static int[] makeLongLenTable() {
        int[] r = new int[64];
        for (int i = 1; i <= 63; i++) {
            long maxNum = (1L << (64 - i)) - 1;
            r[i] = String.valueOf(maxNum).getBytes(StandardCharsets.US_ASCII).length;
        }
        return r;
    }

    // no overflow
    private static long[] makeLongPowTable() {
        long[] r = new long[20];
        for (int i = 2; i <= 19; i++) {
            r[i] = Math.powExact(10L, i - 1);
        }
        return r;
    }

    // no overflow
    private static byte[] makeZeroNineTable() {
        byte[] r = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
        Arrays.fill(r, Byte.MAX_VALUE);
        for (byte b = BYTE_ZERO; b <= BYTE_NINE; b++) {
            int index = Byte.toUnsignedInt(b);
            r[index] = (byte) (BYTE_ZERO - b);
        }
        return r;
    }

    // no overflow
    private static long[] makePow10Table() {
        BigInteger two = BigInteger.valueOf(2);
        BigInteger oneShift64 = BigInteger.ONE.shiftLeft(64);
        BigInteger oneShift128 = BigInteger.ONE.shiftLeft(128);
        int count = POW10MAX - POW10MIN + 1;
        long[] r = new long[count * 2];
        int index = 0;
        for (int e = POW10MIN; e <= POW10MAX; e++) {
            BigInteger num, den;
            if (e >= 0) {
                num = BigInteger.TEN.pow(e);
                den = BigInteger.ONE;
            } else {
                num = BigInteger.ONE;
                den = BigInteger.TEN.pow(-e);
            }
            while (num.compareTo(den.multiply(oneShift128)) < 0) {
                num = num.multiply(two);
            }
            while (num.compareTo(den.multiply(oneShift128)) >= 0) {
                den = den.multiply(two);
            }
            BigInteger d = num.divide(den);
            BigInteger[] hiLo = d.divideAndRemainder(oneShift64);
            long uhi = hiLo[0].longValue();
            long ulo = hiLo[1].longValue();
            if (!num.mod(den).equals(BigInteger.ZERO)) {
                ulo = Math.incrementExact(ulo);
                if (ulo == 0L) {
                    uhi = Math.incrementExact(uhi);
                }
            }
            if (ulo != 0L) {
                uhi = Math.incrementExact(uhi);
                ulo = Math.negateExact(ulo);
            }
            r[index++] = uhi;
            r[index++] = ulo;
        }
        return r;
    }

    private MarshallOldUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * using jdk string conversion to write integer number to writeBuffer, slower but guaranteed to be safe, no overflow
     */

    public static void writeInt0(int number, WriteBuffer writeBuffer) {
        String s = Integer.toString(number);
        writeBuffer.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
    }

    public static void writeLong0(long number, WriteBuffer writeBuffer) {
        String s = Long.toString(number);
        writeBuffer.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * write 1 digit to writeBuffer each time, significantly faster than jdk string version, no overflow
     */

    public static void writeInt1(int number, WriteBuffer writeBuffer) {
        if (number == 0) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        byte[] buffer = new byte[MIN_INT_BYTES.length];
        int index = buffer.length;
        boolean negative = number < 0;
        int sum = negative ? number : -number;
        while (sum != 0) {
            int i = sum % 10;
            buffer[--index] = (byte) (BYTE_ZERO - i);
            sum = sum / 10;
        }
        if (negative) {
            buffer[--index] = BYTE_MINUS;
        }
        writeBuffer.writeBytes(buffer, index, buffer.length - index);
    }

    public static void writeLong1(long number, WriteBuffer writeBuffer) {
        if (number == 0) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        byte[] buffer = new byte[MIN_LONG_BYTES.length];
        int index = buffer.length;
        boolean negative = number < 0L;
        long sum = negative ? number : -number;
        while (sum != 0) {
            int i = (int) (sum % 10L);
            buffer[--index] = (byte) (BYTE_ZERO - i);
            sum = sum / 10L;
        }
        if (negative) {
            buffer[--index] = BYTE_MINUS;
        }
        writeBuffer.writeBytes(buffer, index, buffer.length - index);
    }

    /**
     * using lookup table to write 2 digits to writeBuffer each time, slightly faster than the 1 digit version, no overflow
     */

    public static void writeInt2(int number, WriteBuffer writeBuffer) {
        if (number == 0) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        byte[] buffer = new byte[MIN_INT_BYTES.length];
        int index = buffer.length;
        boolean negative = number < 0;
        if (!negative) {
            number = -number;
        }
        int q;
        while (number <= -100) {
            q = number / 100;
            index -= 2;
            ArrayAccess.setShort(buffer, index, ITOA_LUT_TABLE[(q * 100) - number]);
            number = q;
        }
        if (number <= -10) {
            index -= 2;
            ArrayAccess.setShort(buffer, index, ITOA_LUT_TABLE[-number]);
        } else {
            buffer[--index] = (byte) (BYTE_ZERO - number);
        }
        if (negative) {
            buffer[--index] = BYTE_MINUS;
        }
        writeBuffer.writeBytes(buffer, index, buffer.length - index);
    }

    public static void writeLong2(long number, WriteBuffer writeBuffer) {
        if (number == 0L) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        byte[] buffer = new byte[MIN_LONG_BYTES.length];
        int index = buffer.length;
        boolean negative = number < 0L;
        if (!negative) {
            number = -number;
        }
        long q;
        while (number <= -100L) {
            q = number / 100L;
            index = index - 2;
            ArrayAccess.setShort(buffer, index, ITOA_LUT_TABLE[(int) ((q * 100L) - number)]);
            number = q;
        }
        if (number <= -10L) {
            index = index - 2;
            ArrayAccess.setShort(buffer, index, ITOA_LUT_TABLE[(int) (-number)]);
        } else {
            buffer[--index] = (byte) (BYTE_ZERO - number);
        }
        if (negative) {
            buffer[--index] = BYTE_MINUS;
        }
        writeBuffer.writeBytes(buffer, index, buffer.length - index);
    }

    // no overflow, n必须是一个大于0的正数，返回该值需要占用的十进制的位数，原理是通过前置0的数量以及提前计算出的2次幂对应10次幂的常量表进行快速推断
    // 因为numberOfLeadingZeros是intrinsic方法，这种技巧会比直接计算的方式更快
    private static int digitCount(int n) {
        assert n > 0;
        int leadingZeros = Integer.numberOfLeadingZeros(n);
        assert leadingZeros >= 1 && leadingZeros <= (Float.SIZE - 1); // n should not be 0 or negative
        int count = INT_LEN_TABLE[leadingZeros];
        if (n < INT_POW_TABLE[count]) {
            return count - 1;
        }
        return count;
    }

    // no overflow, n必须是一个大于0的正数，返回该值需要占用的十进制的位数，原理是通过前置0的数量以及提前计算出的2次幂对应10次幂的常量表进行快速推断
    // 因为numberOfLeadingZeros是intrinsic方法，这种技巧会比直接计算的方式更快
    private static int digitCount(long n) {
        assert n > 0L;
        int leadingZeros = Long.numberOfLeadingZeros(n);
        assert leadingZeros >= 1 && leadingZeros <= (Double.SIZE - 1); // n should not be 0 or negative
        int count = LONG_LEN_TABLE[leadingZeros];
        if (n < LONG_POW_TABLE[count]) {
            return count - 1;
        }
        return count;
    }

    /**
     * using lookup table to write 2 digits to writeBuffer each time, using write-through strategy to implement manual loop-unrolling
     * using in-place byte assignment to avoid allocation and memcpy, slightly faster than the standard 2 digit version, no overflow
     */

    public static void writeInt(int value, WriteBuffer writeBuffer) {
        if (value == 0) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        if (value == Integer.MIN_VALUE) {
            writeBuffer.writeBytes(MIN_INT_BYTES);
            return;
        }
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writeIntToHeapWriteBuffer(value, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> writeIntToSegmentWriteBuffer(value, segmentWriteBuffer);
        }
    }

    public static void writeLong(long value, WriteBuffer writeBuffer) {
        if (value == 0L) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        if (value == Long.MIN_VALUE) {
            writeBuffer.writeBytes(MIN_LONG_BYTES);
            return;
        }
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writeLongToHeapWriteBuffer(value, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> writeLongToSegmentWriteBuffer(value, segmentWriteBuffer);
        }
    }

    // no overflow
    private static void writeIntToHeapWriteBuffer(int value, HeapWriteBuffer heapWriteBuffer) {
        int n;
        if (value < 0) {
            value = -value;
            n = digitCount(value);
            heapWriteBuffer.writeByte(BYTE_MINUS);
        } else {
            n = digitCount(value);
        }
        heapWriteBuffer.ensureCapacity(n);
        int position = heapWriteBuffer.intPosition();
        byte[] bytes = heapWriteBuffer.rawByteArray();
        position = writePositiveIntToBytes(value, bytes, position, n);
        heapWriteBuffer.setPosition(position);
    }

    // no overflow
    private static int writePositiveIntToBytes(int value, byte[] bytes, int position, int n) {
        assert bytes != null && Objects.checkFromIndexSize(position, n, bytes.length) >= 0;
        int v;
        switch (n) {
            case 10:
                v = value / 100000000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100000000;
            case 8:
                v = value / 1000000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1000000;
            case 6:
                v = value / 10000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10000;
            case 4:
                v = value / 100;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100;
            case 2:
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[value]);
                position += 2;
                break;
            case 9:
                v = value / 10000000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10000000;
            case 7:
                v = value / 100000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100000;
            case 5:
                v = value / 1000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1000;
            case 3:
                v = value / 10;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10;
            case 1:
                bytes[position] = (byte) (BYTE_ZERO + value);
                position++;
        }
        return position;
    }

    // no overflow
    private static void writeIntToSegmentWriteBuffer(int value, SegmentWriteBuffer segmentWriteBuffer) {
        int n;
        if (value < 0) {
            value = -value;
            n = digitCount(value);
            segmentWriteBuffer.writeByte(BYTE_MINUS);
        } else {
            n = digitCount(value);
        }
        segmentWriteBuffer.ensureCapacity(n);
        long position = segmentWriteBuffer.longPosition();
        MemorySegment segment = segmentWriteBuffer.rawSegment();
        position = writePositiveIntToSegment(value, segment, position, n);
        segmentWriteBuffer.setPosition(position);
    }

    // no overflow
    private static long writePositiveIntToSegment(int value, MemorySegment segment, long position, int n) {
        int v;
        switch (n) {
            case 10:
                v = value / 100000000;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100000000;
            case 8:
                v = value / 1000000;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1000000;
            case 6:
                v = value / 10000;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10000;
            case 4:
                v = value / 100;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100;
            case 2:
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[value]);
                position += 2;
                break;
            case 9:
                v = value / 10000000;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10000000;
            case 7:
                v = value / 100000;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100000;
            case 5:
                v = value / 1000;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1000;
            case 3:
                v = value / 10;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10;
            case 1:
                SegmentAccess.setByte(segment, position, (byte) (BYTE_ZERO + value));
                position++;
        }
        return position;
    }

    // no overflow
    private static void writeLongToHeapWriteBuffer(long value, HeapWriteBuffer heapWriteBuffer) {
        int n;
        if (value < 0) {
            value = -value;
            n = digitCount(value);
            heapWriteBuffer.writeByte(BYTE_MINUS);
        } else {
            n = digitCount(value);
        }
        heapWriteBuffer.ensureCapacity(n);
        int position = heapWriteBuffer.intPosition();
        byte[] bytes = heapWriteBuffer.rawByteArray();
        position = writePositiveLongToBytes(value, bytes, position, n);
        heapWriteBuffer.setPosition(position);
    }

    // no overflow
    private static int writePositiveLongToBytes(long value, byte[] bytes, int position, int n) {
        int v;
        switch (n) {
            case 18:
                v = (int) (value / 1_000_000_000_000_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000_0L;
            case 16:
                v = (int) (value / 1_000_000_000_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_00L;
            case 14:
                v = (int) (value / 1_000_000_000_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000L;
            case 12:
                v = (int) (value / 1_000_000_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_0L;
            case 10:
                v = (int) (value / 1_000_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_00L;
            case 8:
                v = (int) (value / 1_000_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000L;
            case 6:
                v = (int) (value / 1_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_0L;
            case 4:
                v = (int) (value / 100L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100L;
            case 2:
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[(int) value]);
                position += 2;
                break;
            case 19:
                v = (int) (value / 1_000_000_000_000_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000_00L;
            case 17:
                v = (int) (value / 1_000_000_000_000_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000L;
            case 15:
                v = (int) (value / 1_000_000_000_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_0L;
            case 13:
                v = (int) (value / 1_000_000_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_00L;
            case 11:
                v = (int) (value / 1_000_000_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000L;
            case 9:
                v = (int) (value / 1_000_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_0L;
            case 7:
                v = (int) (value / 1_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_00L;
            case 5:
                v = (int) (value / 1_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000L;
            case 3:
                v = (int) (value / 10L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10L;
            case 1:
                bytes[position] = (byte) (BYTE_ZERO + value);
                position++;
        }
        return position;
    }

    // no overflow
    private static void writeLongToSegmentWriteBuffer(long value, SegmentWriteBuffer segmentWriteBuffer) {
        int n;
        if (value < 0) {
            value = -value;
            n = digitCount(value);
            segmentWriteBuffer.writeByte(BYTE_MINUS);
        } else {
            n = digitCount(value);
        }
        segmentWriteBuffer.ensureCapacity(n);
        long position = segmentWriteBuffer.longPosition();
        MemorySegment segment = segmentWriteBuffer.rawSegment();
        position = writePositiveLongToSegment(value, segment, position, n);
        segmentWriteBuffer.setPosition(position);
    }

    // no overflow
    public static long writePositiveLongToSegment(long value, MemorySegment segment, long position, int n) {
        int v;
        switch (n) {
            case 18:
                v = (int) (value / 1_000_000_000_000_000_0L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000_0L;
            case 16:
                v = (int) (value / 1_000_000_000_000_00L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_00L;
            case 14:
                v = (int) (value / 1_000_000_000_000L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000L;
            case 12:
                v = (int) (value / 1_000_000_000_0L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_0L;
            case 10:
                v = (int) (value / 1_000_000_00L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_00L;
            case 8:
                v = (int) (value / 1_000_000L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000L;
            case 6:
                v = (int) (value / 1_000_0L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_0L;
            case 4:
                v = (int) (value / 100L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100L;
            case 2:
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[(int) value]);
                position += 2;
                break;
            case 19:
                v = (int) (value / 1_000_000_000_000_000_00L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000_00L;
            case 17:
                v = (int) (value / 1_000_000_000_000_000L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000L;
            case 15:
                v = (int) (value / 1_000_000_000_000_0L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_0L;
            case 13:
                v = (int) (value / 1_000_000_000_00L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_00L;
            case 11:
                v = (int) (value / 1_000_000_000L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000L;
            case 9:
                v = (int) (value / 1_000_000_0L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_0L;
            case 7:
                v = (int) (value / 1_000_00L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_00L;
            case 5:
                v = (int) (value / 1_000L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000L;
            case 3:
                v = (int) (value / 10L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10L;
            case 1:
                SegmentAccess.setByte(segment, position, (byte) (BYTE_ZERO + value));
                position++;
        }
        return position;
    }

    // 新的实现
    private static BinaryFp buildBinaryFp(long b, FpSpec fpSpec) {
        long mant = b & ((1L << fpSpec.mantBits()) - 1);
        int exp = (int) ((b >>> fpSpec.mantBits()) & ((1L << fpSpec.expBits()) - 1));
        // 区别处理正规数与非正规数的情况
        if (exp == 0) {
            exp++;
        } else {
            mant |= (1L << fpSpec.mantBits());
        }
        exp += fpSpec.bias();
        int s = Long.numberOfLeadingZeros(mant);
        // 让尾数尽可能顶到高位，给低位预留出uscale计算的差值空间，减去mantBits是为了补偿尾数默认包含的2^mantBits因子
        return new BinaryFp(mant << s, exp - s - fpSpec.mantBits());
    }

    private static long ufloor(long u) {
        return (u) >>> 2;
    }

    private static long uceil(long u) {
        return (u + 3L) >>> 2;
    }

    private static long unudge(long u, int d) {
        return u + d;
    }

    private static long uround(long u) {
        return (u + 1L + ((u >>> 2) & 1L)) >>> 2;
    }

    private static long umin(long u) {
        return (u << 2) - 2L;
    }

    private static int log10Pow2(int x) {
        // log₁₀ 2 ≈ 0.30102999566 ≈ 78913 / 2^18
        assert x <= Integer.MAX_VALUE / 78913;
        return (x * 78913) >> 18;
    }

    private static int log2Pow10(int x) {
        // log₂ 10 ≈ 3.32192809489 ≈ 108853 / 2^15
        assert x <= Integer.MAX_VALUE / 108853;
        return (x * 108853) >> 15;
    }

    private static int skewed(int e) {
        // skewed computes the skewed footprint of m * 2**e,
        // which is ⌊log₁₀ 3/4 * 2**e⌋ = ⌊e*(log₁₀ 2)-(log₁₀ 4/3)⌋.
        assert e <= Integer.MAX_VALUE / 631305;
        return (e * 631305 - 261663) >> 21;
    }

    // no overflow
    private static Scalers prescale(int e, int p, int lp) {
        assert p >= POW10MIN && p <= POW10MAX;
        int s = -(e + lp + 3);
        assert s >= 0;
        int idx = (p - POW10MIN) << 1;
        long pmHi = POW10TAB[idx];
        long pmLo = POW10TAB[idx + 1];
        return new Scalers(pmHi, pmLo, s);
    }

    // no overflow
    private static long uscale(long x, Scalers c) {
        assert c.s() >=0 && c.s() < 64;
        long hi = Math.unsignedMultiplyHigh(x, c.pmHi());
        long mid1 = x * c.pmHi();
        long sticky = 1L;
        if ((hi & ((1L << c.s()) - 1L)) == 0L) {
            long mid2 = Math.unsignedMultiplyHigh(x, c.pmLo());
            sticky = Long.compareUnsigned(mid1 - mid2, 1L) > 0 ? 1L : 0L;
            if (Long.compareUnsigned(mid1, mid2) < 0) {
                hi -= 1L;
            }
        }
        return (hi >>> c.s()) | sticky;
    }

    // precomputed constants used by trimZeros()
    private static final long MAX_UINT_64 = 0xFFFFFFFFFFFFFFFFL;
    private static final long DIV_1_E_8_M = 0xc767074b22e90e21L;  // inverse of 5^8
    private static final long DIV_1_E_4_M = 0xd288ce703afb7e91L;  // inverse of 5^4
    private static final long DIV_1_E_2_M = 0x8f5c28f5c28f5c29L;  // inverse of 5^2
    private static final long DIV_1_E_1_M = 0xcccccccccccccccdL;  // inverse of 5
    private static final long DIV_1_E_8_LE = Long.divideUnsigned(MAX_UINT_64, 100_000_000L);
    private static final long DIV_1_E_4_LE = Long.divideUnsigned(MAX_UINT_64, 10_000L);
    private static final long DIV_1_E_2_LE = Long.divideUnsigned(MAX_UINT_64, 100L);
    private static final long DIV_1_E_1_LE = Long.divideUnsigned(MAX_UINT_64, 10L);

    // safe overflow, x的计算是根据逆元的，商一定会被映射到2^64以内，p相比原始值的偏移量不会超过64，因此也没有溢出的风险
    // 算法的思路是用5对于2^64的逆元（不能直接使用10的逆元，因为10和2^64不互质）得到一个假定x是5^n的倍数时，对5^n的商值，如果此时x并不是5^n的倍数，这会将其映射到一个比（2^64 - 1）/ 5^n更大的空间中
    // 此时通过rotate可以实现对2^n的除法，并将低位余数转化到高位，通过与（2^64 - 1）/ 10^n的比较，可以得到是否完全整除的信息，且在完全整除的情况下，低位余数为0,rotate后的结果就是整除后的商
    // 该方法假定x后的0的个数小于等于16,因为64位浮点数的尾数占53位，对应10进制的16位数，如果要支持更高规格的浮点数，则需要采用更严谨的方式
    private static DecimalFp trimZeros(DecimalFp decimalFp) {
        long d = decimalFp.d();
        int p = decimalFp.e();
        // cut 1 zero, or else return.
        long tmp = Long.rotateRight(d * DIV_1_E_1_M, 1);
        if (Long.compareUnsigned(tmp, DIV_1_E_1_LE) > 0) {
            return decimalFp;
        }
        d = tmp;
        p += 1;
        // cut 8 zeros, then 4, then 2, then 1.
        tmp = Long.rotateRight(d * DIV_1_E_8_M, 8);
        if (Long.compareUnsigned(tmp, DIV_1_E_8_LE) <= 0) {
            d = tmp;
            p += 8;
        }
        tmp = Long.rotateRight(d * DIV_1_E_4_M, 4);
        if (Long.compareUnsigned(tmp, DIV_1_E_4_LE) <= 0) {
            d = tmp;
            p += 4;
        }
        tmp = Long.rotateRight(d * DIV_1_E_2_M, 2);
        if (Long.compareUnsigned(tmp, DIV_1_E_2_LE) <= 0) {
            d = tmp;
            p += 2;
        }
        tmp = Long.rotateRight(d * DIV_1_E_1_M, 1);
        if (Long.compareUnsigned(tmp, DIV_1_E_1_LE) <= 0) {
            d = tmp;
            p += 1;
        }
        return new DecimalFp(d, p);
    }

    // 中间产生的所有计算在float和double对应的spec以内，均不会产生溢出的情况,并且根据uscale算法的性质，输出的DecimalFp结果中的d，将一定是一个(0, Long.MAX_VALUE)以内的数，因此在格式化阶段可以不必作为无符号数进行处理
    private static DecimalFp toDecimalFp(BinaryFp binaryFp, FpSpec fpSpec) {
        long m = binaryFp.m();
        int e = binaryFp.e();
        int p;
        long min;
        int z = Double.SIZE - 1 - fpSpec.mantBits();
        if (m == (1L << (Double.SIZE - 1)) && e > fpSpec.minExp()) {
            p = -skewed(e + z);
            min = m - (1L << (z - 2));
        } else {
            if (e < fpSpec.minExp()) {
                z += (fpSpec.minExp() - e);
            }
            p = -log10Pow2(e + z);
            min = m - (1L << (z - 1));
        }
        long max = m + (1L << (z - 1));
        int odd = (int) (m >>> z) & 1;
        Scalers pre = prescale(e, p, log2Pow10(p));
        long dmin = uceil(unudge(uscale(min, pre), odd));
        long dmax = ufloor(unudge(uscale(max, pre), -odd));
        long d = Long.divideUnsigned(dmax, 10L);
        if(Long.compareUnsigned(d * 10L, dmin) >= 0) {
            return trimZeros(new DecimalFp(d, -(p - 1)));
        }
        return new DecimalFp(Long.compareUnsigned(dmin, dmax) < 0 ? uround(uscale(m, pre)) : dmin, -p);
    }

    // no overflow
    public static void writeFloat(float f, WriteBuffer writeBuffer) {
        // 先排除nan和inf的情况
        if (!Float.isFinite(f)) {
            throw new IllegalArgumentException("nan and infinite float are not supported");
        }
        int bits = Float.floatToRawIntBits(f);
        boolean negative = (bits >>> (Float.SIZE - 1)) == 1;
        // 排除+0和-0的情况
        if ((bits & 0x7FFFFFFF) == 0) {
            if (negative) {
                writeBuffer.writeShort(NEG_ZERO);
            } else {
                writeBuffer.writeByte(BYTE_ZERO);
            }
            return;
        }
        // 确保后续直接写入时，不会产生扩容问题
        writeBuffer.ensureCapacity(MAX_FLOAT_CAPACITY);
        if (negative) {
            bits &= ~(1 << (Float.SIZE - 1)); // 固定为正浮点数进行处理
            writeBuffer.writeByte(BYTE_MINUS); // 无论采用哪种格式化方式，开头的负号都是没有问题的
        }
        // 构建二进制浮点数表示
        BinaryFp binaryFp = buildBinaryFp(bits, FLOAT_SPEC);
        // 构建十进制浮点数表示
        DecimalFp decimalFp = toDecimalFp(binaryFp, FLOAT_SPEC);
        // 写入十进制浮点数的字符串形式
        writeDecimalFp(decimalFp, writeBuffer);
    }

    // no overflow
    public static void writeDouble(double f, WriteBuffer writeBuffer) {
        if (!Double.isFinite(f)) {
            throw new IllegalArgumentException("nan and infinite double are not supported");
        }
        long bits = Double.doubleToRawLongBits(f);
        boolean negative = (bits >>> (Double.SIZE - 1)) == 1L;
        if ((bits & 0x7FFFFFFFFFFFFFFFL) == 0L) {
            if (negative) {
                writeBuffer.writeShort(NEG_ZERO);
            } else {
                writeBuffer.writeByte(BYTE_ZERO);
            }
            return;
        }
        writeBuffer.ensureCapacity(MAX_DOUBLE_CAPACITY);
        if (negative) {
            bits &= ~(1L << (Double.SIZE - 1));
            writeBuffer.writeByte(BYTE_MINUS);
        }
        // 构建二进制浮点数表示
        BinaryFp binaryFp = buildBinaryFp(bits, DOUBLE_SPEC);
        // 构建十进制浮点数表示
        DecimalFp decimalFp = toDecimalFp(binaryFp, DOUBLE_SPEC);
        // 写入十进制浮点数的字符串形式
        writeDecimalFp(decimalFp, writeBuffer);
    }

    private static final int MIN_SCI_EXP = -3; // align with jdk format, inclusive
    private static final int MAX_SCI_EXP = 7; // align with jdk format, exclusive

    private static void writeDecimalFp(DecimalFp decimalFp, WriteBuffer writeBuffer) {
        long d = decimalFp.d();
        int e = decimalFp.e();
        int n = digitCount(d); // 可以确定，d的数值在[ 10^(n-1), 10^(n) )之间
        int sciE = e + n - 1; // 科学计数法表示的指数范围，实际值的大小在[ 10^(sciE-1), 10^(sciE) )之间
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> {
                int position = heapWriteBuffer.intPosition();
                byte[] bytes = heapWriteBuffer.rawByteArray();
                if (sciE >= MIN_SCI_EXP && sciE < MAX_SCI_EXP) {
                    position = writeFixedDecimalFp(d, e, bytes, position, n);
                } else {
                    position = writeSciDecimalFp(d, bytes, position, n, sciE);
                }
                heapWriteBuffer.setPosition(position);
            }
            case SegmentWriteBuffer segmentWriteBuffer -> {
                long position = segmentWriteBuffer.longPosition();
                MemorySegment segment = segmentWriteBuffer.rawSegment();
                if (sciE >= MIN_SCI_EXP && sciE < MAX_SCI_EXP) {
                    position = writeFixedDecimalFp(d, e, segment, position, n);
                } else {
                    position = writeSciDecimalFp(d, segment, position, n, sciE);
                }
                segmentWriteBuffer.setPosition(position);
            }
        }
    }

    // no overflow
    private static int writeFixedDecimalFp(long d, int e, byte[] bytes, int position, int n) {
        if (e >= 0) {
            position = writePositiveLongToBytes(d, bytes, position, n);
            if (e > 0) {
                int newPosition = position + e;
                Arrays.fill(bytes, position, newPosition, BYTE_ZERO);
                position = newPosition;
            }
        } else {
            int fracDigits = -e;
            if (fracDigits >= n) {
                ArrayAccess.setShort(bytes, position, ZERO_PERIOD);
                position += 2;
                int leadingZeros = fracDigits - n;
                if (leadingZeros > 0) {
                    int newPosition = position + leadingZeros;
                    Arrays.fill(bytes, position, newPosition, BYTE_ZERO);
                    position = newPosition;
                }
                position = writePositiveLongToBytes(d, bytes, position, n);
            } else {
                int dotPosition = position + (n - fracDigits);
                position = writePositiveLongToBytes(d, bytes, position, n) + 1;
                System.arraycopy(bytes, dotPosition, bytes, dotPosition + 1, fracDigits);
                bytes[dotPosition] = BYTE_PERIOD;
            }
        }
        return position;
    }

    // no overflow
    private static int writeSciDecimalFp(long d, byte[] bytes, int position, int n, int sciE) {
        if (n > 1) {
            int startPosition = position + 1;
            int endPosition = writePositiveLongToBytes(d, bytes, startPosition, n);
            bytes[position] = bytes[startPosition];
            bytes[startPosition] = BYTE_PERIOD;
            position = endPosition;
        } else {
            bytes[position++] = (byte) (BYTE_ZERO + d);
        }
        if(sciE < 0) {
            ArrayAccess.setShort(bytes, position, E_MINUS);
            position += 2;
            sciE = -sciE;
        } else {
            bytes[position++] = BYTE_E;
        }
        position = writePositiveIntToBytes(sciE, bytes, position, digitCount(sciE));
        return position;
    }

    // no overflow
    private static long writeFixedDecimalFp(long d, int e, MemorySegment segment, long position, int n) {
        if (e >= 0) {
            position = writePositiveLongToSegment(d, segment, position, n);
            if (e > 0) {
                segment.asSlice(position, e).fill(BYTE_ZERO);
                position += e;
            }
        } else {
            int fracDigits = -e;
            if (fracDigits >= n) {
                SegmentAccess.setShort(segment, position, ZERO_PERIOD);
                position += 2;
                int leadingZeros = fracDigits - n;
                if (leadingZeros > 0) {
                    segment.asSlice(position, leadingZeros).fill(BYTE_ZERO);
                    position += leadingZeros;
                }
                position = writePositiveLongToSegment(d, segment, position, n);
            } else {
                long dotPosition = position + (n - fracDigits);
                position = writePositiveLongToSegment(d, segment, position, n) + 1L;
                MemorySegment.copy(segment, dotPosition, segment, dotPosition + 1L, fracDigits);
                SegmentAccess.setByte(segment, dotPosition, BYTE_PERIOD);
            }
        }
        return position;
    }

    // no overflow
    private static long writeSciDecimalFp(long d, MemorySegment segment, long position, int n, int sciE) {
        if (n > 1) {
            long startPosition = position + 1L;
            long endPosition = writePositiveLongToSegment(d, segment, startPosition, n);
            SegmentAccess.setByte(segment, position, SegmentAccess.getByte(segment, startPosition));
            SegmentAccess.setByte(segment, startPosition, BYTE_PERIOD);
            position = endPosition;
        } else {
            SegmentAccess.setByte(segment, position++, (byte) (BYTE_ZERO + d));
        }
        if(sciE < 0) {
            SegmentAccess.setShort(segment, position, E_MINUS);
            position += 2;
            sciE = -sciE;
        } else {
            SegmentAccess.setByte(segment, position++, BYTE_E);
        }
        position = writePositiveIntToSegment(sciE, segment, position, digitCount(sciE));
        return position;
    }

    public static int readInt(ReadBuffer readBuffer) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> readHeapInt(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> readSegmentInt(segmentReadBuffer);
        };
    }

    private static final int N_DIV_10_I = Integer.MIN_VALUE / 10;
    private static final byte N_MOD_10_I = (byte) (Integer.MIN_VALUE % 10);

    private static boolean negativeIntOverflow(int r, byte b) {
        return r < N_DIV_10_I || (r == N_DIV_10_I && b < N_MOD_10_I);
    }

    private static int readHeapInt(HeapReadBuffer heapReadBuffer) {
        int position = heapReadBuffer.intPosition();
        if (position == heapReadBuffer.intLength()) {
            throw new NumberFormatException("empty buffer");
        }
        byte[] bytes = heapReadBuffer.rawByteArray();
        boolean negative = false;
        int r;
        byte firstByte = bytes[position++];
        if (firstByte == BYTE_MINUS) {
            negative = true;
            if (position == bytes.length) {
                throw new NumberFormatException("empty buffer");
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[position])];
            if (v >= 0) {
                throw new NumberFormatException("illegal negative value : " + v);
            }
            r = v;
            position++;
        } else if (firstByte == BYTE_ZERO) {
            if (position < bytes.length && ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[position])] <= 0) {
                throw new NumberFormatException("leading zero");
            }
            return 0;
        } else {
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
            if (v > 0) {
                throw new NumberFormatException("illegal value : " + v);
            }
            r = v;
        }
        while (position < bytes.length) {
            byte b = ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[position])];
            if (b <= 0) {
                if(negativeIntOverflow(r, b)) {
                    throw new ArithmeticException("integer overflow");
                }
                r = r * 10 + b;
                position++;
            } else {
                heapReadBuffer.setPosition(position);
                break;
            }
        }
        if(negative) {
            return r;
        }
        if(r == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        return -r;
    }

    private static int readSegmentInt(SegmentReadBuffer segmentReadBuffer) {
        int position = segmentReadBuffer.intPosition();
        int len = segmentReadBuffer.intLength();
        if (position == len) {
            throw new NumberFormatException("empty buffer");
        }
        MemorySegment segment = segmentReadBuffer.rawSegment();
        boolean negative = false;
        int r;
        byte firstByte = SegmentAccess.getByte(segment, position++);
        if (firstByte == BYTE_MINUS) {
            negative = true;
            if (position == len) {
                throw new NumberFormatException("empty buffer");
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))];
            if (v >= 0) {
                throw new NumberFormatException("empty buffer");
            }
            r = v;
            position++;
        } else if (firstByte == BYTE_ZERO) {
            if (position < len && ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))] <= 0) {
                throw new NumberFormatException("leading zero");
            }
            return 0;
        } else {
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
            if (v > 0) {
                throw new NumberFormatException("illegal value : " + v);
            }
            r = v;
        }
        while (position < len) {
            byte b = ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))];
            if (b <= 0) {
                if(negativeIntOverflow(r, b)) {
                    throw new ArithmeticException("integer overflow");
                }
                r =  r * 10 + b;
                position++;
            } else {
                segmentReadBuffer.setPosition(position);
                break;
            }
        }
        if(negative) {
            return r;
        }
        if(r == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        return -r;
    }

    public static long readLong(ReadBuffer readBuffer) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> readHeapLong(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> readSegmentLong(segmentReadBuffer);
        };
    }

    private static final long N_DIV_10_L = Long.MIN_VALUE / 10;
    private static final byte N_MOD_10_L = (byte) (Long.MIN_VALUE % 10);

    private static boolean negativeLongOverflow(long r, byte b) {
        return r < N_DIV_10_L || (r == N_DIV_10_L && b < N_MOD_10_L);
    }

    private static long readHeapLong(HeapReadBuffer heapReadBuffer) {
        int position = heapReadBuffer.intPosition();
        if (position == heapReadBuffer.intLength()) {
            throw new NumberFormatException("empty buffer");
        }
        byte[] bytes = heapReadBuffer.rawByteArray();
        boolean negative = false;
        long r;
        byte firstByte = bytes[position++];
        if (firstByte == BYTE_MINUS) {
            negative = true;
            if (position == bytes.length) {
                throw new NumberFormatException("empty buffer");
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[position])];
            if (v >= 0) {
                throw new NumberFormatException("illegal negative value : " + v);
            }
            r = v;
            position++;
        } else if (firstByte == BYTE_ZERO) {
            if (position < bytes.length && ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[position])] <= 0) {
                throw new NumberFormatException("leading zero");
            }
            return 0L;
        } else {
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
            if (v > 0) {
                throw new NumberFormatException("illegal value : " + v);
            }
            r = v;
        }
        while (position < bytes.length) {
            byte b = ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[position])];
            if (b <= 0) {
                if(negativeLongOverflow(r, b)) {
                    throw new ArithmeticException("long overflow");
                }
                r = r * 10L + b;
                position++;
            } else {
                heapReadBuffer.setPosition(position);
                break;
            }
        }
        if(negative) {
            return r;
        }
        if(r == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        return -r;
    }

    private static long readSegmentLong(SegmentReadBuffer segmentReadBuffer) {
        int position = segmentReadBuffer.intPosition();
        int len = segmentReadBuffer.intLength();
        if (position == len) {
            throw new NumberFormatException("empty buffer");
        }
        MemorySegment segment = segmentReadBuffer.rawSegment();
        boolean negative = false;
        long r;
        byte firstByte = SegmentAccess.getByte(segment, position++);
        if (firstByte == BYTE_MINUS) {
            negative = true;
            if (position == len) {
                throw new NumberFormatException("empty buffer");
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))];
            if (v >= 0) {
                throw new NumberFormatException("empty buffer");
            }
            r = v;
            position++;
        } else if (firstByte == BYTE_ZERO) {
            if (position < len && ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))] <= 0) {
                throw new NumberFormatException("leading zero");
            }
            return 0L;
        } else {
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
            if (v > 0) {
                throw new NumberFormatException("illegal value : " + v);
            }
            r = v;
        }
        while (position < len) {
            byte b = ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))];
            if (b <= 0) {
                if(negativeLongOverflow(r, b)) {
                    throw new ArithmeticException("long overflow");
                }
                r =  r * 10L + b;
                position++;
            } else {
                segmentReadBuffer.setPosition(position);
                break;
            }
        }
        if(negative) {
            return r;
        }
        if(r == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        return -r;
    }

    /**
     * floating-point parsing and printing algorithms are based on Russ Cox's
     * "Floating-Point Printing and Parsing Can Be Simple And Fast".
     *
     * @see <a href="https://research.swtch.com/fp-all">research.swtch.com/fp-all</a>
     */

    // exposed for test purpose 将字符串格式的浮点数解析为特定格式，注意实现对于格式是严格校验的，像.123E0123这种格式不能通过该方法，但可以被jdk所接收
    public static FpStr parseFpStr(ReadBuffer readBuffer) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> parseHeapFpStr(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> parseSegmentFpStr(segmentReadBuffer);
        };
    }

    // uint64最多可以容纳19位十进制的数字
    private static final int MAX_DECIMAL_ND = 19;

    // 最大的指数值位数，根据当前fp32和fp64的规范，绝对值超过10000的指数在实际的计算过程中绝对不会对最终的结果造成影响，注意这里的截断是必要的，否则可能会在log2Pow10 skewed等函数中产生溢出风险
    private static final int MAX_DECIMAL_NP = 4;

    private static FpStr parseHeapFpStr(HeapReadBuffer heapReadBuffer) {
        int position = heapReadBuffer.intPosition();
        int length = heapReadBuffer.intLength();
        if (position == length) {
            throw new NumberFormatException("empty buffer");
        }
        byte[] bytes = heapReadBuffer.rawByteArray();
        int index = position;
        boolean neg = false;
        boolean negExp = false;
        boolean trunc = false;
        long d;
        int frac = 0;
        int p = 0;
        // process first sign or digit
        byte target = bytes[index];
        byte v;
        if (target == BYTE_MINUS) {
            if (++index == length) {
                throw new NumberFormatException("leading minus sign with no digits");
            }
            neg = true;
            target = bytes[index];
        }
        v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
        if (v > 0) {
            throw new NumberFormatException("not a digit : " + target);
        }
        d = -v;
        index++;
        // process following digits
        int nd = 1;
        while (index < length) {
            target = bytes[index];
            v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
            if (v <= 0) {
                if(nd == 1) {
                    if(d == 0L) {
                        throw new NumberFormatException("leading zero");
                    }
                }
                if(nd < MAX_DECIMAL_ND) {
                    d = d * 10L - v;
                    nd++;
                } else {
                    trunc = true;
                }
                index++;
            } else {
                break;
            }
        }
        // processing optional fraction
        if (index < length && target == BYTE_PERIOD) {
            index++;
            while (index < length) {
                target = bytes[index];
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                if (v <= 0) {
                    if(nd < MAX_DECIMAL_ND) {
                        d = d * 10L - v;
                        nd++;
                        frac++;
                    } else {
                        trunc = true;
                    }
                    index++;
                } else {
                    break;
                }
            }
            if (frac == 0) {
                throw new NumberFormatException("leading period with no digits");
            }
        }
        // processing optional exponent
        if (index < length && (target == BYTE_E || target == BYTE_e)) {
            // processing first sign or digit
            if (++index == length) {
                throw new NumberFormatException("leading exponent sign with no digits");
            }
            target = bytes[index];
            if (target == BYTE_MINUS || target == BYTE_PLUS) {
                if (++index == length) {
                    throw new NumberFormatException("leading sign with no digits");
                }
                if (target == BYTE_MINUS) {
                    negExp = true;
                }
                target = bytes[index];
            }
            v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
            if (v > 0) {
                throw new NumberFormatException("illegal start of number : " + target);
            }
            p = -v;
            index++;
            // processing following exponent
            int np = 1;
            while (index < length) {
                target = bytes[index];
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                if (v <= 0) {
                    if(np == 1) {
                        if (p == 0) {
                            throw new NumberFormatException("leading zero");
                        }
                    }
                    if(np < MAX_DECIMAL_NP) {
                        p = p * 10 - v;
                        np++;
                    }
                    index++;
                } else {
                    break;
                }
            }
        }
        p = (negExp ? -p : p) - frac;
        return new FpStr(neg, trunc, d, frac, p, index - position);
    }

    private static FpStr parseSegmentFpStr(SegmentReadBuffer segmentReadBuffer) {
        int position = segmentReadBuffer.intPosition(); // forcing int
        int length = segmentReadBuffer.intLength();
        if (position == length) {
            throw new NumberFormatException("empty buffer");
        }
        MemorySegment segment = segmentReadBuffer.rawSegment();
        int index = position;
        boolean neg = false;
        boolean negExp = false;
        boolean trunc = false;
        long d;
        int frac = 0;
        int p = 0;
        // processing first sign or digit
        byte target = SegmentAccess.getByte(segment, index);
        byte v;
        if (target == BYTE_MINUS) {
            if (++index == length) {
                throw new NumberFormatException("leading minus sign with no digits");
            }
            neg = true;
            target = SegmentAccess.getByte(segment, index);
        }
        v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
        if (v > 0) {
            throw new NumberFormatException("not a digit : " + target);
        }
        d = -v;
        index++;
        // process following digits
        int nd = 1;
        while (index < length) {
            target = SegmentAccess.getByte(segment, index);
            v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
            if (v <= 0) {
                if(nd == 1) {
                    if(d == 0L) {
                        throw new NumberFormatException("leading zero");
                    }
                }
                if(nd < MAX_DECIMAL_ND) {
                    d = d * 10L - v;
                    nd++;
                } else {
                    trunc = true;
                }
                index++;
            } else {
                break;
            }
        }
        // processing optional fraction
        if (index < length && target == BYTE_PERIOD) {
            index++;
            while (index < length) {
                target = SegmentAccess.getByte(segment, index);
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                if (v <= 0) {
                    if(nd < MAX_DECIMAL_ND) {
                        d = d * 10L - v;
                        nd++;
                        frac++;
                    } else {
                        trunc = true;
                    }
                    index++;
                } else {
                    break;
                }
            }
            if (frac == 0) {
                throw new NumberFormatException("leading period with no digits");
            }
        }
        // processing optional exponent
        if (index < length && (target == BYTE_E || target == BYTE_e)) {
            // processing first sign or digit
            if (++index == length) {
                throw new NumberFormatException("leading exponent sign with no digits");
            }
            target = SegmentAccess.getByte(segment, index);
            if (target == BYTE_MINUS || target == BYTE_PLUS) {
                if (++index == length) {
                    throw new NumberFormatException("leading sign with no digits");
                }
                if (target == BYTE_MINUS) {
                    negExp = true;
                }
                target = SegmentAccess.getByte(segment, index);
            }
            v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
            if (v > 0) {
                throw new NumberFormatException("illegal start of number : " + target);
            }
            p = -v;
            index++;
            // processing following exponent
            int np = 1;
            while (index < length) {
                target = SegmentAccess.getByte(segment, index);
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                if (v <= 0) {
                    if(np == 1) {
                        if (p == 0) {
                            throw new NumberFormatException("leading zero");
                        }
                    }
                    if(np < MAX_DECIMAL_NP) {
                        p = p * 10 - v;
                        np++;
                    }
                    index++;
                } else {
                    break;
                }
            }
        }
        p = (negExp ? -p : p) - frac;
        return new FpStr(neg, trunc, d, frac, p, index - position);
    }

    private static final float[] FLOAT_POW_10 = {
            1e0f, 1e1f, 1e2f, 1e3f, 1e4f, 1e5f, 1e6f, 1e7f, 1e8f, 1e9f, 1e10f
    };
    private static final int FLOAT_EXACT_I = 7;
    private static final int FLOAT_EXACT_P = 10;
    private static final float FLOAT_EXACT_I_HIGH = 1e7f;
    private static final float FLOAT_EXACT_I_LOW = 1e-7f;

    public static float readFloat(ReadBuffer readBuffer) {
        FpStr fpStr = parseFpStr(readBuffer);
        if(fpStr.trunc()) {
            return readFloatFallback(readBuffer, fpStr);
        }
        int sign = fpStr.negative() ? (1 << (FLOAT_SPEC.mantBits() + FLOAT_SPEC.expBits())) : 0;
        long d = fpStr.d();
        int p = fpStr.p();
        if(d == 0L || p < FLOAT_SPEC.minDecExp() - MAX_DECIMAL_ND - 2) {
            return Float.intBitsToFloat(sign);
        }
        if(p > FLOAT_SPEC.maxDecExp() + 2) {
            return Float.intBitsToFloat(sign | (0xff << FLOAT_SPEC.mantBits()));
        }
        if(d >> FLOAT_SPEC.mantBits() == 0L) {
            float f = (float) Math.toIntExact(fpStr.negative() ? -d : d);
            if(p == 0) {
                return f;
            } else if(p > 0 && p <= FLOAT_EXACT_I + FLOAT_EXACT_P) {
                int tp = p;
                if(tp > FLOAT_EXACT_P) {
                    f *= FLOAT_POW_10[tp - FLOAT_EXACT_P];
                    tp = FLOAT_EXACT_P;
                }
                if(f >= FLOAT_EXACT_I_LOW && f <= FLOAT_EXACT_I_HIGH) {
                    return f * FLOAT_POW_10[tp];
                }
            } else if(p < 0 && p >= -FLOAT_EXACT_P) {
                return f /  FLOAT_POW_10[-p];
            }
        }
        int lp = log2Pow10(p);
        int shift = Long.numberOfLeadingZeros(d);
        int b = Long.SIZE - shift;
        int fe = Math.min(FLOAT_SPEC.mantBits() - FLOAT_SPEC.bias() - 1, FLOAT_SPEC.mantBits() + 1 - b - lp);
        Scalers scalers = prescale(fe - shift, p, lp);
        if(scalers.s() >= Long.SIZE) {
            return Float.intBitsToFloat(sign);
        }
        long u = uscale(d << shift, scalers);
        if (u >= umin(1L << (FLOAT_SPEC.mantBits() + 1))) {
            u = (u >>> 1) | (u & 1);
            fe--;
        }
        int m = sign | Math.toIntExact(uround(u));
        if((m & (1 << FLOAT_SPEC.mantBits())) == 0) {
            return Float.intBitsToFloat(m);
        }
        int e = -fe;
        if(e >= (1 << FLOAT_SPEC.expBits()) - 1 - FLOAT_SPEC.mantBits() + FLOAT_SPEC.bias()) {
            return readFloatFallback(readBuffer, fpStr);
        }
        return Float.intBitsToFloat(
                (m & ~(1 << FLOAT_SPEC.mantBits())) |
                        (FLOAT_SPEC.mantBits() - FLOAT_SPEC.bias() + e) << FLOAT_SPEC.mantBits());
    }

    private static float readFloatFallback(ReadBuffer readBuffer, FpStr fpStr) {
        int len = fpStr.len();
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                byte[] bytes = heapReadBuffer.rawByteArray();
                int position = heapReadBuffer.intPosition();
                String s = new String(bytes, position, len, StandardCharsets.US_ASCII);
                heapReadBuffer.setPosition(position + len);
                return Float.parseFloat(s);
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                MemorySegment segment = segmentReadBuffer.rawSegment();
                long position = segmentReadBuffer.longPosition();
                byte[] bytes = segment.asSlice(position, len).toArray(ValueLayout.JAVA_BYTE);
                String s = new String(bytes, StandardCharsets.US_ASCII);
                segmentReadBuffer.setPosition(position + len);
                return Float.parseFloat(s);
            }
        }
    }

    private static final double[] DOUBLE_POW_10 = {
            1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9,
            1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17,
            1e18, 1e19, 1e20, 1e21, 1e22
    };
    private static final int DOUBLE_EXACT_I = 15;
    private static final int DOUBLE_EXACT_P = 22;
    private static final double DOUBLE_EXACT_I_HIGH = 1e15;
    private static final double DOUBLE_EXACT_I_LOW = 1e-15;

    public static double readDouble(ReadBuffer readBuffer) {
        FpStr fpStr = parseFpStr(readBuffer);
        if(fpStr.trunc()) {
            return readDoubleFallback(readBuffer, fpStr);
        }
        long sign = fpStr.negative() ? (1L << (DOUBLE_SPEC.mantBits() + DOUBLE_SPEC.expBits())) : 0L;
        long d = fpStr.d();
        int p = fpStr.p();
        if(d == 0L || p < DOUBLE_SPEC.minDecExp() - MAX_DECIMAL_ND - 2) {
            return Double.longBitsToDouble(sign);
        }
        if(p > DOUBLE_SPEC.maxDecExp() + 2) {
            return Double.longBitsToDouble(sign | (0x7ffL << DOUBLE_SPEC.mantBits()));
        }
        if(d >>> DOUBLE_SPEC.mantBits() == 0L) {
            double f = (double) (fpStr.negative() ? -d : d);
            if(p == 0) {
                return f;
            } else if(p > 0 && p <= DOUBLE_EXACT_I + DOUBLE_EXACT_P) {
                int tp = p;
                if(tp > DOUBLE_EXACT_P) {
                    f *= DOUBLE_POW_10[tp - DOUBLE_EXACT_P];
                    tp = DOUBLE_EXACT_P;
                }
                if(f >= DOUBLE_EXACT_I_LOW && f <= DOUBLE_EXACT_I_HIGH) {
                    return f * DOUBLE_POW_10[tp];
                }
            } else if(p < 0 && p >= -DOUBLE_EXACT_P) {
                return f / DOUBLE_POW_10[-p];
            }
        }
        int lp = log2Pow10(p);
        int shift = Long.numberOfLeadingZeros(d);
        int b = Long.SIZE - shift;
        int fe = Math.min(DOUBLE_SPEC.mantBits() - DOUBLE_SPEC.bias() - 1, DOUBLE_SPEC.mantBits() + 1 - b - lp);
        Scalers scalers = prescale(fe - shift, p, lp);
        if(scalers.s() >= Long.SIZE) {
            return Double.longBitsToDouble(sign);
        }
        long u = uscale(d << shift, scalers);
        if (u >= umin(1L << (DOUBLE_SPEC.mantBits() + 1))) {
            u = (u >>> 1) | (u & 1);
            fe--;
        }
        long m = sign | uround(u);
        if((m & (1L << DOUBLE_SPEC.mantBits())) == 0L) {
            return Double.longBitsToDouble(m);
        }
        int e = -fe;
        if(e >= (1 << DOUBLE_SPEC.expBits()) - 1 - DOUBLE_SPEC.mantBits() + DOUBLE_SPEC.bias()) {
            return readDoubleFallback(readBuffer, fpStr);
        }
        return Double.longBitsToDouble(
                (m & ~(1L << DOUBLE_SPEC.mantBits())) |
                        ((long) (DOUBLE_SPEC.mantBits() - DOUBLE_SPEC.bias() + e)) << DOUBLE_SPEC.mantBits());
    }

    private static double readDoubleFallback(ReadBuffer readBuffer, FpStr fpStr) {
        int len = fpStr.len();
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                byte[] bytes = heapReadBuffer.rawByteArray();
                int position = heapReadBuffer.intPosition();
                String s = new String(bytes, position, len, StandardCharsets.US_ASCII);
                heapReadBuffer.setPosition(position + len);
                return Double.parseDouble(s);
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                MemorySegment segment = segmentReadBuffer.rawSegment();
                long postion = segmentReadBuffer.longPosition();
                byte[] bytes = segment.asSlice(postion, len).toArray(ValueLayout.JAVA_BYTE);
                String s = new String(bytes, StandardCharsets.US_ASCII);
                segmentReadBuffer.setPosition(postion + len);
                return Double.parseDouble(s);
            }
        }
    }
}
