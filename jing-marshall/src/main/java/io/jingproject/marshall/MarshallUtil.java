package io.jingproject.marshall;

import io.jingproject.common.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class MarshallUtil {

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
    private static final int MAX_FLOAT_CAPACITY = 16;
    private static final int MAX_DOUBLE_CAPACITY = 25;
    private static final int MIN_DOUBLE_E = -324;
    private static final int MAX_DOUBLE_E = 308;
    private static final int MIN_FLOAT_E = -45;
    private static final int MAX_FLOAT_E = 38;
    private static final short NEG_ZERO = Utils.compact(BYTE_MINUS, BYTE_ZERO);
    private static final short ZERO_PERIOD = Utils.compact(BYTE_ZERO, BYTE_PERIOD);
    private static final long MAX_UINT_64 = 0xFFFFFFFFFFFFFFFFL;
    private static final long DIV_1_E_8_M = 0xc767074b22e90e21L;  // inverse of 5^8
    private static final long DIV_1_E_4_M = 0xd288ce703afb7e91L;  // inverse of 5^4
    private static final long DIV_1_E_2_M = 0x8f5c28f5c28f5c29L;  // inverse of 5^2
    private static final long DIV_1_E_1_M = 0xcccccccccccccccdL;  // inverse of 5
    private static final long DIV_1_E_8_LE = Long.divideUnsigned(MAX_UINT_64, 100_000_000L);
    private static final long DIV_1_E_4_LE = Long.divideUnsigned(MAX_UINT_64, 10_000L);
    private static final long DIV_1_E_2_LE = Long.divideUnsigned(MAX_UINT_64, 100L);
    private static final long DIV_1_E_1_LE = Long.divideUnsigned(MAX_UINT_64, 10L);
    private static final int POW10MIN = -348;
    private static final int POW10MAX = 347;
    private static final long[] POW10TAB = makePow10Table();
    private static final int FLOAT_MANT_BITS = 23;
    private static final int FLOAT_EXP_BITS = 8;
    private static final int FLOAT_BIAS = -127;
    private static final int FLOAT_MIN_EXP = -189;
    private static final int DOUBLE_MANT_BITS = 52;
    private static final int DOUBLE_EXP_BITS = 11;
    private static final int DOUBLE_BIAS = -1023;
    private static final int DOUBLE_MIN_EXP = -1085;
    private static final int MIN_SCI_EXP = -3;
    private static final int MAX_SCI_EXP = 7; // align with jdk format

    // string to integer constants
    private static final byte[] ZERO_NINE_TABLE = makeZeroNineTable();

    // float to integer constants
    private static final int STRTOD_MAX_INTEGER_DIGITS = 19;
    private static final int STRTOD_MAX_EXP_DIGITS = 9;

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
        BigInteger TWO = BigInteger.valueOf(2);
        BigInteger B1P64 = BigInteger.ONE.shiftLeft(64);
        BigInteger B1P128 = BigInteger.ONE.shiftLeft(128);
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
            while (num.compareTo(den.multiply(B1P128)) < 0) {
                num = num.multiply(TWO);
            }
            while (num.compareTo(den.multiply(B1P128)) >= 0) {
                den = den.multiply(TWO);
            }
            BigInteger d = num.divide(den);
            BigInteger[] hiLo = d.divideAndRemainder(B1P64);
            long uhi = hiLo[0].longValue();
            long ulo = hiLo[1].longValue();
            if (!num.mod(den).equals(BigInteger.ZERO)) {
                ulo++;
                if (ulo == 0L) {
                    uhi++;
                }
            }
            if (ulo != 0L) {
                uhi++;
                ulo = -ulo;
            }
            r[index++] = uhi;
            r[index++] = ulo;
        }
        return r;
    }

    private MarshallUtil() {
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

    // no overflow
    private static int digitCount(int n) {
        int leadingZeros = Integer.numberOfLeadingZeros(n);
        assert leadingZeros >= 1 && leadingZeros <= 31; // n should not be 0 or negative
        int count = INT_LEN_TABLE[leadingZeros];
        if (n < INT_POW_TABLE[count]) {
            return count - 1;
        }
        return count;
    }

    // no overflow
    private static int digitCount(long n) {
        int leadingZeros = Long.numberOfLeadingZeros(n);
        assert leadingZeros >= 1 && leadingZeros <= 63; // n should not be 0 or negative
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
        int position = heapWriteBuffer.intPosition();
        byte[] bytes = heapWriteBuffer.rawByteArray();
        int digitCount;
        if (value < 0) {
            value = -value;
            digitCount = digitCount(value);
            heapWriteBuffer.ensureCapacity(digitCount + 1);
            bytes[position++] = BYTE_MINUS;
        } else {
            digitCount = digitCount(value);
            heapWriteBuffer.ensureCapacity(digitCount);
        }
        position = writePositiveIntToBytes(value, bytes, position, digitCount);
        heapWriteBuffer.setPosition(position);
    }

    // no overflow
    private static int writePositiveIntToBytes(int value, byte[] bytes, int position, int digitCount) {
        int v;
        switch (digitCount) {
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
        long position = segmentWriteBuffer.longPosition();
        MemorySegment segment = segmentWriteBuffer.rawSegment();
        int digitCount;
        if (value < 0) {
            value = -value;
            digitCount = digitCount(value);
            segmentWriteBuffer.ensureCapacity(digitCount + 1); // no overflow
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
        } else {
            digitCount = digitCount(value);
            segmentWriteBuffer.ensureCapacity(digitCount);
        }
        position = writePositiveIntToSegment(value, segment, position, digitCount);
        segmentWriteBuffer.setPosition(position);
    }

    // no overflow
    private static long writePositiveIntToSegment(int value, MemorySegment segment, long position, int digitCount) {
        int v;
        switch (digitCount) {
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
        int position = heapWriteBuffer.intPosition();
        byte[] bytes = heapWriteBuffer.rawByteArray();
        int digitCount;
        if (value < 0) {
            value = -value;
            digitCount = digitCount(value);
            heapWriteBuffer.ensureCapacity(digitCount + 1); // no overflow
            bytes[position++] = BYTE_MINUS;
        } else {
            digitCount = digitCount(value);
            heapWriteBuffer.ensureCapacity(digitCount);
        }
        position = writePositiveLongToBytes(value, bytes, position, digitCount);
        heapWriteBuffer.setPosition(position);
    }

    // no overflow
    private static int writePositiveLongToBytes(long value, byte[] bytes, int position, int digitCount) {
        int v;
        switch (digitCount) {
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
        long position = segmentWriteBuffer.longPosition();
        MemorySegment segment = segmentWriteBuffer.rawSegment();
        int digitCount;
        if (value < 0) {
            value = -value;
            digitCount = digitCount(value);
            segmentWriteBuffer.ensureCapacity(digitCount + 1);
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
        } else {
            digitCount = digitCount(value);
            segmentWriteBuffer.ensureCapacity(digitCount);
        }
        position = writePositiveLongToSegment(value, segment, position, digitCount);
        segmentWriteBuffer.setPosition(position);
    }

    // no overflow
    private static long writePositiveLongToSegment(long value, MemorySegment segment, long position, int digitCount) {
        int v;
        switch (digitCount) {
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

    // no overflow
    private static FpRep unpack(long b, int mantBits, int expBits, int bias) {
        int exp = (int) ((b >>> mantBits) & ((1L << expBits) - 1));
        long mant = b & ((1L << mantBits) - 1);
        if (exp == 0) {
            exp++;
        } else {
            mant |= (1L << mantBits);
        }
        exp += bias;
        int s = Long.numberOfLeadingZeros(mant);
        return new FpRep(mant << s, exp - s - mantBits);
    }

    // no overflow
    private static long packDoubleBits(long m, int e) {
        if ((m & (1L << DOUBLE_MANT_BITS)) != 0L) {
            m = (m & ~(1L << DOUBLE_MANT_BITS)) | (((long) (DOUBLE_MANT_BITS - DOUBLE_BIAS + e)) << DOUBLE_MANT_BITS);
        }
        return m;
    }

    // no overflow
    private static int packFloatBits(long m, int e) {
        int im = Math.toIntExact(m);
        if ((im & (1 << FLOAT_MANT_BITS)) != 0) {
            im = (im & ~(1 << FLOAT_MANT_BITS)) | ((FLOAT_MANT_BITS - FLOAT_BIAS + e) << FLOAT_MANT_BITS);
        }
        return im;
    }

    // no overflow
    private static long ufloor(long u) {
        return (u) >>> 2;
    }

    // no overflow
    private static long uceil(long u) {
        return (u + 3L) >>> 2;
    }

    // no overflow
    private static long unudge(long u, int d) {
        return u + d;
    }

    // no overflow
    private static long uround(long u) {
        return (u + 1L + ((u >>> 2) & 1L)) >>> 2;
    }

    // no overflow
    private static long umin(long u) {
        return (u << 2) - 2L;
    }

    // safe overflow
    private static int log10Pow2(int x) {
        return (x * 78913) >> 18; // x * log₁₀2
    }

    // safe overflow
    private static int log2Pow10(int x) {
        return (x * 108853) >> 15; // x * log₂10
    }

    // safe overflow
    private static int skewed(int e) {
        return (e * 631305 - 261663) >> 21; // ⌊log₁₀ 3/4 * 2**p⌋
    }

    // no overflow
    private static Scalers prescale(int e, int p, int lp) {
        assert p >= POW10MIN && p <= POW10MAX;
        int s = -(e + lp + 3);
        assert s >= 0 && s < 64;
        int idx = (p - POW10MIN) << 1;
        long pmHi = POW10TAB[idx];
        long pmLo = POW10TAB[idx + 1];
        return new Scalers(pmHi, pmLo, s);
    }

    // no overflow
    private static long uscale(long x, Scalers c) {
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

    // safe overflow
    private static FpRep trimZeros(long x, int p) {
        long d;
        // cut 1 zero, or else return.
        d = Long.rotateRight(x * DIV_1_E_1_M, 1);
        if (Long.compareUnsigned(d, DIV_1_E_1_LE) > 0) {
            return new FpRep(x, p);
        }
        x = d;
        p += 1;
        // cut 8 zeros, then 4, then 2, then 1.
        d = Long.rotateRight(x * DIV_1_E_8_M, 8);
        if (Long.compareUnsigned(d, DIV_1_E_8_LE) <= 0) {
            x = d;
            p += 8;
        }
        d = Long.rotateRight(x * DIV_1_E_4_M, 4);
        if (Long.compareUnsigned(d, DIV_1_E_4_LE) <= 0) {
            x = d;
            p += 4;
        }
        d = Long.rotateRight(x * DIV_1_E_2_M, 2);
        if (Long.compareUnsigned(d, DIV_1_E_2_LE) <= 0) {
            x = d;
            p += 2;
        }
        d = Long.rotateRight(x * DIV_1_E_1_M, 1);
        if (Long.compareUnsigned(d, DIV_1_E_1_LE) <= 0) {
            x = d;
            p += 1;
        }
        return new FpRep(x, p);
    }

    // no overflow
    private static FpRep transform(FpRep r, int mantBits, int minExp) {
        long m = r.d();
        int e = r.e();
        int p;
        long min;
        int z = 63 - mantBits;
        if (m == (1L << 63) && e > minExp) {
            p = -skewed(e + z);
            min = m - (1L << (z - 2));
        } else {
            if (e < minExp) {
                z += (minExp - e);
            }
            p = -log10Pow2(e + z);
            min = m - (1L << (z - 1));
        }
        long max = m + (1L << (z - 1));
        int odd = (int) (m >>> z) & 1;
        Scalers pre = prescale(e, p, log2Pow10(p));
        long dmin = uceil(unudge(uscale(min, pre), odd));
        long dmax = ufloor(unudge(uscale(max, pre), -odd));
        long d0 = Long.divideUnsigned(dmax, 10L) * 10L;
        if (Long.compareUnsigned(d0, dmin) >= 0) {
            return trimZeros(Long.divideUnsigned(dmax, 10L), -(p - 1));
        }
        long d = dmin;
        if (Long.compareUnsigned(d, dmax) < 0) {
            d = uround(uscale(m, pre));
        }
        return new FpRep(d, -p);
    }

    // no overflow
    public static void writeFloat(float f, WriteBuffer writeBuffer) {
        if (!Float.isFinite(f)) {
            throw new IllegalArgumentException("nan and infinite float are not supported");
        }
        int bits = Float.floatToRawIntBits(f);
        boolean negative = (bits >>> 31) == 1;
        if ((bits & 0x7FFFFFFF) == 0) {
            if (negative) {
                writeBuffer.writeShort(NEG_ZERO);
            } else {
                writeBuffer.writeByte(BYTE_ZERO);
            }
            return;
        }
        writeBuffer.ensureCapacity(MAX_FLOAT_CAPACITY);
        if (negative) {
            bits &= ~(1 << 31);
            writeBuffer.writeByte(BYTE_MINUS);
        }
        FpRep r = transform(unpack(bits, FLOAT_MANT_BITS, FLOAT_EXP_BITS, FLOAT_BIAS), FLOAT_MANT_BITS, FLOAT_MIN_EXP);
        long d = r.d();
        int e = r.e();
        int digitCount = digitCount(d);
        int scientificExp = e + digitCount - 1;
        assert scientificExp >= MIN_FLOAT_E && scientificExp <= MAX_FLOAT_E;
        writeFpToWriteBuffer(d, e, digitCount, scientificExp, writeBuffer);
    }

    // no overflow
    public static void writeDouble(double f, WriteBuffer writeBuffer) {
        if (!Double.isFinite(f)) {
            throw new IllegalArgumentException("nan and infinite double are not supported");
        }
        long bits = Double.doubleToRawLongBits(f);
        boolean negative = (bits >>> 63) == 1L;
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
            bits &= ~(1L << 63);
            writeBuffer.writeByte(BYTE_MINUS);
        }
        FpRep r = transform(unpack(bits, DOUBLE_MANT_BITS, DOUBLE_EXP_BITS, DOUBLE_BIAS), DOUBLE_MANT_BITS, DOUBLE_MIN_EXP);
        long d = r.d();
        int e = r.e();
        int digitCount = digitCount(d);
        int scientificExp = e + digitCount - 1;
        assert scientificExp >= MIN_DOUBLE_E && scientificExp <= MAX_DOUBLE_E;
        writeFpToWriteBuffer(d, e, digitCount, scientificExp, writeBuffer);
    }

    // no overflow
    private static void writeFpToWriteBuffer(long d, int e, int digitCount, int scientificExp, WriteBuffer writeBuffer) {
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> {
                int position = heapWriteBuffer.intPosition();
                byte[] bytes = heapWriteBuffer.rawByteArray();
                if (scientificExp >= MIN_SCI_EXP && scientificExp < MAX_SCI_EXP) {
                    position = writeFixedFpToBytes(d, e, bytes, position, digitCount);
                } else {
                    position = writeScientificFpToBytes(d, bytes, position, digitCount, scientificExp);
                }
                heapWriteBuffer.setPosition(position);
            }
            case SegmentWriteBuffer segmentWriteBuffer -> {
                long position = segmentWriteBuffer.longPosition();
                MemorySegment segment = segmentWriteBuffer.rawSegment();
                if (scientificExp >= MIN_SCI_EXP && scientificExp < MAX_SCI_EXP) {
                    position = writeFixedFpToSegment(d, e, segment, position, digitCount);
                } else {
                    position = writeScientificFpToSegment(d, segment, position, digitCount, scientificExp);
                }
                segmentWriteBuffer.setPosition(position);
            }
        }
    }

    // no overflow
    private static int writeFixedFpToBytes(long d, int e, byte[] bytes, int position, int digitCount) {
        if (e >= 0) {
            position = writePositiveLongToBytes(d, bytes, position, digitCount);
            if (e > 0) {
                int newPosition = position + e;
                Arrays.fill(bytes, position, newPosition, BYTE_ZERO);
                position = newPosition;
            }
        } else {
            int fracDigits = -e;
            if (fracDigits >= digitCount) {
                ArrayAccess.setShort(bytes, position, ZERO_PERIOD);
                position += 2;
                int leadingZeros = fracDigits - digitCount;
                if (leadingZeros > 0) {
                    int newPosition = position + leadingZeros;
                    Arrays.fill(bytes, position, newPosition, BYTE_ZERO);
                    position = newPosition;
                }
                position = writePositiveLongToBytes(d, bytes, position, digitCount);
            } else {
                int dotPosition = position + (digitCount - fracDigits);
                position = writePositiveLongToBytes(d, bytes, position, digitCount) + 1;
                System.arraycopy(bytes, dotPosition, bytes, dotPosition + 1, fracDigits);
                bytes[dotPosition] = BYTE_PERIOD;
            }
        }
        return position;
    }

    // no overflow
    private static int writeScientificFpToBytes(long d, byte[] bytes, int position, int digitCount, int scientificExp) {
        if (digitCount > 1) {
            int startPosition = position + 1;
            int endPosition = writePositiveLongToBytes(d, bytes, startPosition, digitCount);
            bytes[position] = bytes[startPosition];
            bytes[startPosition] = BYTE_PERIOD;
            position = endPosition;
        } else {
            bytes[position++] = (byte) (BYTE_ZERO + d);
        }
        bytes[position++] = BYTE_E;
        if (scientificExp < 0) {
            bytes[position++] = BYTE_MINUS;
            scientificExp = -scientificExp;
        }
        int expDigitCount = digitCount(scientificExp);
        position = writePositiveIntToBytes(scientificExp, bytes, position, expDigitCount);
        return position;
    }

    // no overflow
    private static long writeFixedFpToSegment(long d, int e, MemorySegment segment, long position, int digitCount) {
        if (e >= 0) {
            position = writePositiveLongToSegment(d, segment, position, digitCount);
            if (e > 0) {
                segment.asSlice(position, e).fill(BYTE_ZERO);
                position += e;
            }
        } else {
            int fracDigits = -e;
            if (fracDigits >= digitCount) {
                SegmentAccess.setShort(segment, position, ZERO_PERIOD);
                position += 2;
                int leadingZeros = fracDigits - digitCount;
                if (leadingZeros > 0) {
                    segment.asSlice(position, leadingZeros).fill(BYTE_ZERO);
                    position += leadingZeros;
                }
                position = writePositiveLongToSegment(d, segment, position, digitCount);
            } else {
                long dotPosition = position + (digitCount - fracDigits);
                position = writePositiveLongToSegment(d, segment, position, digitCount) + 1L;
                MemorySegment.copy(segment, dotPosition, segment, dotPosition + 1L, fracDigits);
                SegmentAccess.setByte(segment, dotPosition, BYTE_PERIOD);
            }
        }
        return position;
    }

    // no overflow
    private static long writeScientificFpToSegment(long d, MemorySegment segment, long position, int digitCount, int scientificExp) {
        if (digitCount > 1) {
            long startPosition = position + 1L;
            long endPosition = writePositiveLongToSegment(d, segment, startPosition, digitCount);
            SegmentAccess.setByte(segment, position, SegmentAccess.getByte(segment, startPosition));
            SegmentAccess.setByte(segment, startPosition, BYTE_PERIOD);
            position = endPosition;
        } else {
            SegmentAccess.setByte(segment, position++, (byte) (BYTE_ZERO + d));
        }
        SegmentAccess.setByte(segment, position++, BYTE_E);
        if (scientificExp < 0) {
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
            scientificExp = -scientificExp;
        }
        int expDigitCount = digitCount(scientificExp);
        position = writePositiveIntToSegment(scientificExp, segment, position, expDigitCount);
        return position;
    }

    public static long readLong(ReadBuffer readBuffer) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> readLongFromHeapReadBuffer(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> readLongFromSegmentReadBuffer(segmentReadBuffer);
        };
    }

    // safe overflow
    private static long readLongFromHeapReadBuffer(HeapReadBuffer heapReadBuffer) {
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
                r = Math.addExact(Math.multiplyExact(r, 10L), b);
                position++;
            } else {
                heapReadBuffer.setPosition(position);
                break;
            }
        }
        if (negative) {
            return r;
        }
        if (r == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        return -r;
    }

    // safe overflow
    private static long readLongFromSegmentReadBuffer(SegmentReadBuffer segmentReadBuffer) {
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
                r = Math.addExact(Math.multiplyExact(r, 10L), b);
                position++;
            } else {
                segmentReadBuffer.setPosition(position);
                break;
            }
        }
        if (negative) {
            return r;
        }
        if (r == Long.MIN_VALUE) {
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

    // no overflow
    public static float readFloat(ReadBuffer readBuffer) {
        FpFormat fpFormat = parseFpFormat(readBuffer);
        if (fpFormatFallbackRequired(fpFormat, MAX_FLOAT_E)) {
            return readFloatFallback(readBuffer, fpFormat);
        }
        long d = fpFormat.d();
        int p = fpFormat.p();
        int lp = log2Pow10(p);
        int shift = Long.numberOfLeadingZeros(d);
        int b = 64 - shift;
        int e = Math.min(FLOAT_MANT_BITS - FLOAT_BIAS - 1, FLOAT_MANT_BITS + 1 - b - lp);
        long u = uscale(d << shift, prescale(e - shift, p, lp));
        if (u >= umin(1L << (FLOAT_MANT_BITS + 1))) {
            u = (u >>> 1) | (u & 1);
            e--;
        }
        int r = packFloatBits(uround(u), -e);
        if (fpFormat.negative()) {
            r |= 1 << (FLOAT_MANT_BITS + FLOAT_EXP_BITS);
        }
        return Float.intBitsToFloat(r);
    }

    // safe
    private static float readFloatFallback(ReadBuffer readBuffer, FpFormat fpFormat) {
        int len = fpFormat.len();
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

    // no overflow
    public static double readDouble(ReadBuffer readBuffer) {
        FpFormat fpFormat = parseFpFormat(readBuffer);
        if (fpFormatFallbackRequired(fpFormat, MAX_DOUBLE_E)) {
            return readDoubleFallback(readBuffer, fpFormat);
        }
        long d = fpFormat.d();
        int p = fpFormat.p();
        int lp = log2Pow10(p);
        int shift = Long.numberOfLeadingZeros(d);
        int b = 64 - shift;
        int e = Math.min(DOUBLE_MANT_BITS - DOUBLE_BIAS - 1, DOUBLE_MANT_BITS + 1 - b - lp);
        long u = uscale(d << shift, prescale(e - shift, p, lp));
        if (u >= umin(1L << (DOUBLE_MANT_BITS + 1))) {
            u = (u >>> 1) | (u & 1);
            e--;
        }
        long r = packDoubleBits(uround(u), -e);
        if (fpFormat.negative()) {
            r |= 1L << (DOUBLE_MANT_BITS + DOUBLE_EXP_BITS);
        }
        return Double.longBitsToDouble(r);
    }

    // safe
    private static double readDoubleFallback(ReadBuffer readBuffer, FpFormat fpFormat) {
        int len = fpFormat.len();
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

    private static boolean fpFormatFallbackRequired(FpFormat fpFormat, int maxExponent) {
        // If the length of the exponent part already exceeds the int range,
        // it means the value of p has overflowed and is invalid, so a fallback is needed.
        if (fpFormat.pLen() > STRTOD_MAX_EXP_DIGITS) {
            return true;
        }
        // The long type can only represent decimal numbers with up to STRTOD_MAX_INTEGER_DIGITS significant digits.
        // For numbers exceeding this limit, a fallback is needed.
        int digitCount = Math.addExact(fpFormat.dLen(), fpFormat.frac());
        if (digitCount > STRTOD_MAX_INTEGER_DIGITS) {
            return true;
        }
        // Values < 10^(digitCount + exp) <= 10^(maxExponent) < (max fp value) can be guaranteed not to cause arithmetic overflow.
        return Math.addExact(digitCount, fpFormat.p()) > maxExponent;
    }

    // exposed for test purpose
    public static FpFormat parseFpFormat(ReadBuffer readBuffer) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> parseFpFormatFromHeapReadBuffer(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> parseFpFormatFromSegmentReadBuffer(segmentReadBuffer);
        };
    }

    // safe overflow
    private static FpFormat parseFpFormatFromHeapReadBuffer(HeapReadBuffer heapReadBuffer) {
        int position = heapReadBuffer.intPosition();
        int length = heapReadBuffer.intLength();
        if (position == length) {
            throw new NumberFormatException("empty buffer");
        }
        byte[] bytes = heapReadBuffer.rawByteArray();
        int index = position;
        boolean negative = false;
        boolean negativeExponent = false;
        long d;
        int dLen = 0;
        int frac = 0;
        int p = 0;
        int pLen = 0;
        // processing first sign or digit
        byte target = bytes[index];
        byte v;
        if (target == BYTE_MINUS) {
            if (++index == length) {
                throw new NumberFormatException("leading minus sign with no digits");
            }
            negative = true;
            target = bytes[index];
        }
        v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
        if (v > 0) {
            throw new NumberFormatException("not a digit : " + target);
        }
        d = -v;
        dLen = Math.incrementExact(dLen);
        index++;
        // process following digits
        while (index < length) {
            target = bytes[index];
            v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
            if (v <= 0) {
                if (d == 0L) {
                    throw new NumberFormatException("multiple leading zero");
                }
                d = d * 10L - v;
                dLen = Math.incrementExact(dLen);
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
                    d = d * 10L - v;
                    frac = Math.incrementExact(frac);
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
                    negativeExponent = true;
                }
                target = bytes[index];
            }
            v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
            if (v > 0) {
                throw new NumberFormatException("illegal start of number : " + target);
            }
            p = -v;
            pLen = Math.incrementExact(pLen);
            index++;
            // processing following exponent
            while (index < length) {
                target = bytes[index];
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                if (v <= 0) {
                    if (p == 0) {
                        throw new NumberFormatException("multiple leading zero");
                    }
                    p = p * 10 - v;
                    pLen = Math.incrementExact(pLen);
                    index++;
                } else {
                    break;
                }
            }
        }
        p = Math.subtractExact(negativeExponent ? -p : p, frac);
        return new FpFormat(negative, d, dLen, frac, p, pLen, index - position);
    }

    // safe overflow
    private static FpFormat parseFpFormatFromSegmentReadBuffer(SegmentReadBuffer segmentReadBuffer) {
        int position = segmentReadBuffer.intPosition(); // forcing int
        int length = segmentReadBuffer.intLength();
        if (position == length) {
            throw new NumberFormatException("empty buffer");
        }
        MemorySegment segment = segmentReadBuffer.rawSegment();
        int index = position;
        boolean negative = false;
        boolean negativeExponent = false;
        long d;
        int dLen = 0;
        int frac = 0;
        int p = 0;
        int pLen = 0;
        // processing first sign or digit
        byte target = SegmentAccess.getByte(segment, index);
        byte v;
        if (target == BYTE_MINUS) {
            if (++index == length) {
                throw new NumberFormatException("leading minus sign with no digits");
            }
            negative = true;
            target = SegmentAccess.getByte(segment, index);
        }
        v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
        if (v > 0) {
            throw new NumberFormatException("not a digit : " + target);
        }
        d = -v;
        dLen = Math.incrementExact(dLen);
        index++;
        // process following digits
        while (index < length) {
            target = SegmentAccess.getByte(segment, index);
            v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
            if (v <= 0) {
                if (d == 0L) {
                    throw new NumberFormatException("multiple leading zero");
                }
                d = d * 10L - v;
                dLen = Math.incrementExact(dLen);
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
                    d = d * 10L - v;
                    frac = Math.incrementExact(frac);
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
                    negativeExponent = true;
                }
                target = SegmentAccess.getByte(segment, index);
            }
            v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
            if (v > 0) {
                throw new NumberFormatException("illegal start of number : " + target);
            }
            p = -v;
            pLen = Math.incrementExact(pLen);
            index++;
            // processing following exponent
            while (index < length) {
                target = SegmentAccess.getByte(segment, index);
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(target)];
                if (v <= 0) {
                    if (p == 0) {
                        throw new NumberFormatException("multiple leading zero");
                    }
                    p = p * 10 - v;
                    pLen = Math.incrementExact(pLen);
                    index++;
                } else {
                    break;
                }
            }
        }
        p = Math.subtractExact(negativeExponent ? -p : p, frac);
        return new FpFormat(negative, d, dLen, frac, p, pLen, index - position);
    }

}
