package io.jingproject.marshalljson;

import io.jingproject.common.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * floating-point parsing and printing algorithms are based on Russ Cox's
 * "Floating-Point Printing and Parsing Can Be Simple And Fast".
 *
 * @see <a href="https://research.swtch.com/fp-all">research.swtch.com/fp-all</a>
 */
public final class JsonNumberUtil {
    public static final byte BYTE_ZERO = (byte) '0';
    public static final byte BYTE_NINE = (byte) '9';
    public static final byte BYTE_MINUS = (byte) '-';
    public static final byte BYTE_PLUS = (byte) '+';
    public static final byte BYTE_PERIOD = (byte) '.';
    public static final byte BYTE_e = (byte) 'e';
    public static final byte BYTE_E = (byte) 'E';

    private static final byte[] MIN_INT_BYTES = String.valueOf(Integer.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MIN_LONG_BYTES = String.valueOf(Long.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final int[] LEN_TABLE = makeLenTable();
    private static final long[] POW_TABLE = makePowTable();
    private static final short[] ITOA_LUT_TABLE = makeItoaLutTable();

    private static final FpSpec FLOAT_SPEC = new FpSpec(23, 8, -127, -189, 38, -45);
    private static final FpSpec DOUBLE_SPEC = new FpSpec(52, 11, -1023, -1085, 308, -324);
    private static final int MAX_FLOAT_CAPACITY = 15; // same as MAX_CHARS in jdk/internal/math/FloatToDecimal.java
    private static final int MAX_DOUBLE_CAPACITY = 24; // same as MAX_CHARS in jdk/internal/math/DoubleToDecimal.java
    private static final short NEG_ZERO = Utils.compact(BYTE_MINUS, BYTE_ZERO);
    private static final short ZERO_PERIOD = Utils.compact(BYTE_ZERO, BYTE_PERIOD);
    private static final short E_MINUS = Utils.compact(BYTE_E, BYTE_MINUS);
    private static final int POW10MIN = -348;
    private static final int POW10MAX = 347;
    private static final long[] POW10TAB = makePow10Table(); // huge table
    private static final byte[] ZERO_NINE_TABLE = makeZeroNineTable();

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

    private static final int MIN_SCI_EXP = -3; // align with jdk format, inclusive
    private static final int MAX_SCI_EXP = 7; // align with jdk format, exclusive

    private static final int N_DIV_10_I = Integer.MIN_VALUE / 10;
    private static final byte N_MOD_10_I = (byte) (Integer.MIN_VALUE % 10);

    private static final long N_DIV_10_L = Long.MIN_VALUE / 10;
    private static final byte N_MOD_10_L = (byte) (Long.MIN_VALUE % 10);

    // Maximum decimal digits that fit in a uint64 (19)
    private static final int MAX_DECIMAL_ND = 19;

    // Maximum exponent digits. For fp32/fp64, exponents beyond 10000 in absolute
    // value never affect the final result. Truncation is necessary to avoid overflow
    // in log2Pow10, skewed, and similar functions.
    private static final int MAX_DECIMAL_P = 10000;

    private static final int MAX_FP_INPUT = 256;

    private static final float[] FLOAT_POW_10 = {
            1e0f, 1e1f, 1e2f, 1e3f, 1e4f, 1e5f, 1e6f, 1e7f, 1e8f, 1e9f, 1e10f
    };
    private static final int FLOAT_EXACT_I = 7;
    private static final int FLOAT_EXACT_P = 10;
    private static final float FLOAT_EXACT_I_HIGH = 1e7f;
    private static final float FLOAT_EXACT_I_LOW = 1e-7f;

    private static final double[] DOUBLE_POW_10 = {
            1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9,
            1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17,
            1e18, 1e19, 1e20, 1e21, 1e22
    };
    private static final int DOUBLE_EXACT_I = 15;
    private static final int DOUBLE_EXACT_P = 22;
    private static final double DOUBLE_EXACT_I_HIGH = 1e15;
    private static final double DOUBLE_EXACT_I_LOW = 1e-15;

    // no overflow
    private static int[] makeLenTable() {
        int[] r = new int[64];
        for (int i = 1; i <= 63; i++) {
            long maxNum = -1L >>> i;
            r[i] = String.valueOf(maxNum).getBytes(StandardCharsets.US_ASCII).length;
        }
        return r;
    }

    // no overflow
    private static long[] makePowTable() {
        long[] r = new long[20];
        for (int i = 2; i <= 19; i++) {
            r[i] = Math.powExact(10L, i - 1);
        }
        return r;
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

    private static long[] makePow10Table() {
        BigInteger mask64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
        BigInteger[] pow10 = new BigInteger[-POW10MIN + 1];
        pow10[0] = BigInteger.ONE;
        for(int i = 1; i <= -POW10MIN; i++) {
            pow10[i] = pow10[i - 1].multiply(BigInteger.TEN);
        }
        int count = POW10MAX - POW10MIN + 1;
        long[] r = new long[count * 2];
        int index = 0;
        for (int e = POW10MIN; e <= POW10MAX; e++) {
            BigInteger[] qr = computeScaledPower(e, pow10);
            long uhi = qr[0].shiftRight(64).longValue();
            long ulo = qr[0].and(mask64).longValue();
            if (qr[1].signum() != 0) {
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

    private static BigInteger[] computeScaledPower(int e, BigInteger[] pow10) {
        BigInteger num = e >= 0 ? pow10[e] : BigInteger.ONE;
        BigInteger den = e >= 0 ? BigInteger.ONE : pow10[-e];
        BigInteger shifted = den.shiftLeft(128);
        int numPreShifted = shifted.bitLength() - num.bitLength() - 1;
        if(numPreShifted > 0) {
            num = num.shiftLeft(numPreShifted);
        }
        while (num.compareTo(shifted) < 0) {
            num = num.shiftLeft(1);
        }
        int denPreShifted = num.bitLength() - shifted.bitLength() - 1;
        if(denPreShifted > 0) {
            den = den.shiftLeft(denPreShifted);
            shifted = shifted.shiftLeft(denPreShifted);
        }
        while (num.compareTo(shifted) >= 0) {
            den = den.shiftLeft(1);
            shifted = shifted.shiftLeft(1);
        }
        return num.divideAndRemainder(den);
    }

    private static byte[] makeZeroNineTable() {
        byte[] r = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
        Arrays.fill(r, Byte.MAX_VALUE);
        for (byte b = BYTE_ZERO; b <= BYTE_NINE; b++) {
            int index = Byte.toUnsignedInt(b);
            r[index] = (byte) (BYTE_ZERO - b);
        }
        return r;
    }

    private JsonNumberUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    private static int digitCount(int n) {
        assert n > 0;
        int leadingZeros = Integer.numberOfLeadingZeros(n);
        int count = LEN_TABLE[leadingZeros + Long.SIZE - Integer.SIZE];
        if (n < POW_TABLE[count]) {
            return count - 1;
        }
        return count;
    }

    private static int digitCount(long n) {
        assert n > 0L;
        int leadingZeros = Long.numberOfLeadingZeros(n);
        int count = LEN_TABLE[leadingZeros];
        if (n < POW_TABLE[count]) {
            return count - 1;
        }
        return count;
    }

    public static void writeInt(int value, WriteBuffer writeBuffer) {
        if (value == 0) {
            writeBuffer.writeByte(BYTE_ZERO);
        } else if (value == Integer.MIN_VALUE) {
            writeBuffer.writeBytes(MIN_INT_BYTES);
        } else if (value > 0) {
            writeBuffer.setPosition(writePositiveInt(value, writeBuffer));
        } else {
            writeBuffer.writeByte(BYTE_MINUS);
            writeBuffer.setPosition(writePositiveInt(-value, writeBuffer));
        }
    }

    private static int writePositiveInt(int value, WriteBuffer writeBuffer) {
        assert value > 0;
        int n = digitCount(value);
        writeBuffer.ensureCapacity(n);
        int position = writeBuffer.intPosition();
        return switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writePositiveIntToHeap(value, n, heapWriteBuffer.rawByteArray(), position);
            case SegmentWriteBuffer segmentWriteBuffer -> writePositiveIntToSegment(value, n, segmentWriteBuffer.rawSegment(), position);
        };
    }

    private static int writePositiveIntToHeap(int value, int n, byte[] bytes, int position) {
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

    private static int writePositiveIntToSegment(int value, int n, MemorySegment segment, int position) {
        long lp = position;
        int v;
        switch (n) {
            case 10:
                v = value / 100000000;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 100000000;
            case 8:
                v = value / 1000000;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1000000;
            case 6:
                v = value / 10000;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 10000;
            case 4:
                v = value / 100;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 100;
            case 2:
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[value]);
                lp += 2;
                break;
            case 9:
                v = value / 10000000;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 10000000;
            case 7:
                v = value / 100000;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 100000;
            case 5:
                v = value / 1000;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1000;
            case 3:
                v = value / 10;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 10;
            case 1:
                SegmentAccess.setByte(segment, lp, (byte) (BYTE_ZERO + value));
                lp++;
        }
        return Math.toIntExact(lp);
    }

    public static void writeLong(long value, WriteBuffer writeBuffer) {
        if (value == 0L) {
            writeBuffer.writeByte(BYTE_ZERO);
        } else if (value == Long.MIN_VALUE) {
            writeBuffer.writeBytes(MIN_LONG_BYTES);
        } else if (value > 0) {
            writeBuffer.setPosition(writePositiveLong(value, writeBuffer));
        } else {
            writeBuffer.writeByte(BYTE_MINUS);
            writeBuffer.setPosition(writePositiveLong(-value, writeBuffer));
        }
    }

    private static int writePositiveLong(long value, WriteBuffer writeBuffer) {
        assert value > 0L;
        int n = digitCount(value);
        writeBuffer.ensureCapacity(n);
        int position = writeBuffer.intPosition();
        return switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writePositiveLongToHeap(value, n, heapWriteBuffer.rawByteArray(), position);
            case SegmentWriteBuffer segmentWriteBuffer -> writePositiveLongToSegment(value, n, segmentWriteBuffer.rawSegment(), position);
        };
    }

    private static int writePositiveLongToHeap(long value, int n, byte[] bytes, int position) {
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

    private static int writePositiveLongToSegment(long value, int n, MemorySegment segment, int position) {
        long lp = position;
        int v;
        switch (n) {
            case 18:
                v = (int) (value / 1_000_000_000_000_000_0L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_000_000_0L;
            case 16:
                v = (int) (value / 1_000_000_000_000_00L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_000_00L;
            case 14:
                v = (int) (value / 1_000_000_000_000L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_000L;
            case 12:
                v = (int) (value / 1_000_000_000_0L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_0L;
            case 10:
                v = (int) (value / 1_000_000_00L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_00L;
            case 8:
                v = (int) (value / 1_000_000L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000L;
            case 6:
                v = (int) (value / 1_000_0L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_0L;
            case 4:
                v = (int) (value / 100L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 100L;
            case 2:
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[(int) value]);
                lp += 2;
                break;
            case 19:
                v = (int) (value / 1_000_000_000_000_000_00L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_000_000_00L;
            case 17:
                v = (int) (value / 1_000_000_000_000_000L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_000_000L;
            case 15:
                v = (int) (value / 1_000_000_000_000_0L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_000_0L;
            case 13:
                v = (int) (value / 1_000_000_000_00L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_00L;
            case 11:
                v = (int) (value / 1_000_000_000L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000L;
            case 9:
                v = (int) (value / 1_000_000_0L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_0L;
            case 7:
                v = (int) (value / 1_000_00L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_00L;
            case 5:
                v = (int) (value / 1_000L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000L;
            case 3:
                v = (int) (value / 10L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 10L;
            case 1:
                SegmentAccess.setByte(segment, lp, (byte) (BYTE_ZERO + value));
                lp++;
        }
        return Math.toIntExact(lp);
    }

    public static void writeFloat(float f, WriteBuffer writeBuffer) {
        assert Float.isFinite(f);
        int bits = Float.floatToRawIntBits(f);
        boolean negative = (bits >>> (Float.SIZE - 1)) == 1;
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
            bits &= ~(1 << (Float.SIZE - 1));
            writeBuffer.writeByte(BYTE_MINUS);
        }
        BinaryFp binaryFp = buildBinaryFp(bits, FLOAT_SPEC);
        DecimalFp decimalFp = toDecimalFp(binaryFp, FLOAT_SPEC);
        writeDecimalFp(decimalFp, writeBuffer);
    }

    public static void writeDouble(double f, WriteBuffer writeBuffer) {
        assert Double.isFinite(f);
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
        BinaryFp binaryFp = buildBinaryFp(bits, DOUBLE_SPEC);
        DecimalFp decimalFp = toDecimalFp(binaryFp, DOUBLE_SPEC);
        writeDecimalFp(decimalFp, writeBuffer);
    }

    private static BinaryFp buildBinaryFp(long b, FpSpec fpSpec) {
        long mant = b & ((1L << fpSpec.mantBits()) - 1);
        int exp = (int) ((b >>> fpSpec.mantBits()) & ((1L << fpSpec.expBits()) - 1));
        if (exp == 0) {
            exp++;
        } else {
            mant |= (1L << fpSpec.mantBits());
        }
        exp += fpSpec.bias();
        int s = Long.numberOfLeadingZeros(mant);
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

    private static int skewed(int e) {
        // skewed computes the skewed footprint of m * 2**e,
        // which is ⌊log₁₀ 3/4 * 2**e⌋ = ⌊e*(log₁₀ 2)-(log₁₀ 4/3)⌋.
        assert e <= Integer.MAX_VALUE / 631305;
        return (e * 631305 - 261663) >> 21;
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

    private static DecimalFp trimZeros(DecimalFp decimalFp) {
        long d = decimalFp.d();
        int p = decimalFp.p();
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

    private static DecimalFp toDecimalFp(BinaryFp binaryFp, FpSpec fpSpec) {
        long m = binaryFp.m();
        int e = binaryFp.e();
        int p;
        long min;
        int z = Long.SIZE - 1 - fpSpec.mantBits();
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

    private static void writeDecimalFp(DecimalFp decimalFp, WriteBuffer writeBuffer) {
        long d = decimalFp.d();
        int p = decimalFp.p();
        int n = digitCount(d);
        int sciE = p + n - 1;
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writeDecimalFpToHeap(d, p, n, sciE, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer ->  writeDecimalFpToSegment(d, p, n, sciE, segmentWriteBuffer);
        }
    }

    private static void writeDecimalFpToHeap(long d, int p, int n, int sciE, HeapWriteBuffer heapWriteBuffer) {
        int position = heapWriteBuffer.intPosition();
        byte[] bytes = heapWriteBuffer.rawByteArray();
        if (sciE >= MIN_SCI_EXP && sciE < MAX_SCI_EXP) {
            position = writeFixedDecimalFpToHeap(d, p, n, bytes, position);
        } else {
            position = writeSciDecimalFpToHeap(d, sciE, n, bytes, position);
        }
        heapWriteBuffer.setPosition(position);
    }

    private static int writeFixedDecimalFpToHeap(long d, int p, int n, byte[] bytes, int position) {
        if(p >= 0) {
            position = writePositiveLongToHeap(d, n, bytes, position);
            if(p > 0) {
                int newPosition = position + p;
                Arrays.fill(bytes, position, newPosition, BYTE_ZERO);
                position = newPosition;
            }
        } else {
            int fracDigits = -p;
            if (fracDigits >= n) {
                ArrayAccess.setShort(bytes, position, ZERO_PERIOD);
                position += 2;
                int leadingZeros = fracDigits - n;
                if (leadingZeros > 0) {
                    int newPosition = position + leadingZeros;
                    Arrays.fill(bytes, position, newPosition, BYTE_ZERO);
                    position = newPosition;
                }
                position = writePositiveLongToHeap(d, n, bytes, position);
            } else {
                int dotPosition = position + (n - fracDigits);
                position = writePositiveLongToHeap(d, n, bytes, position) + 1;
                System.arraycopy(bytes, dotPosition, bytes, dotPosition + 1, fracDigits);
                bytes[dotPosition] = BYTE_PERIOD;
            }
        }
        return position;
    }

    private static int writeSciDecimalFpToHeap(long d, int sciE, int n, byte[] bytes, int position) {
        if (n > 1) {
            int startPosition = position + 1;
            int endPosition = writePositiveLongToHeap(d, n, bytes, startPosition);
            short shifted = Utils.compact(bytes[startPosition], BYTE_PERIOD);
            ArrayAccess.setShort(bytes, position, shifted);
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
        position = writePositiveIntToHeap(sciE, digitCount(sciE), bytes, position);
        return position;
    }


    private static void writeDecimalFpToSegment(long d, int p, int n, int sciE, SegmentWriteBuffer segmentWriteBuffer) {
        int position = segmentWriteBuffer.intPosition();
        MemorySegment segment = segmentWriteBuffer.rawSegment();
        if (sciE >= MIN_SCI_EXP && sciE < MAX_SCI_EXP) {
            position = writeFixedDecimalFpToSegment(d, p, n, segment, position);
        } else {
            position = writeSciDecimalFpToSegment(d, sciE, n, segment, position);
        }
        segmentWriteBuffer.setPosition(position);
    }

    private static int writeFixedDecimalFpToSegment(long d, int p, int n, MemorySegment segment, int position) {
        if (p >= 0) {
            position = writePositiveLongToSegment(d, n, segment, position);
            if (p > 0) {
                segment.asSlice(position, p).fill(BYTE_ZERO);
                position += p;
            }
        } else {
            int fracDigits = -p;
            if (fracDigits >= n) {
                SegmentAccess.setShort(segment, position, ZERO_PERIOD);
                position += 2;
                int leadingZeros = fracDigits - n;
                if (leadingZeros > 0) {
                    segment.asSlice(position, leadingZeros).fill(BYTE_ZERO);
                    position += leadingZeros;
                }
                position = writePositiveLongToSegment(d, n, segment, position);
            } else {
                long dotPosition = position + (n - fracDigits);
                position = writePositiveLongToSegment(d, n, segment, position) + 1;
                MemorySegment.copy(segment, dotPosition, segment, dotPosition + 1L, fracDigits);
                SegmentAccess.setByte(segment, dotPosition, BYTE_PERIOD);
            }
        }
        return position;
    }

    private static int writeSciDecimalFpToSegment(long d, int sciE, int n, MemorySegment segment, int position) {
        if (n > 1) {
            int startPosition = position + 1;
            int endPosition = writePositiveLongToSegment(d, n, segment, startPosition);
            short shifted = Utils.compact(SegmentAccess.getByte(segment, startPosition), BYTE_PERIOD);
            SegmentAccess.setShort(segment, position, shifted);
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
        position = writePositiveIntToSegment(sciE, digitCount(sciE), segment, position);
        return position;
    }

    public static int readInt(ReadBuffer readBuffer, byte firstByte) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> readHeapInt(heapReadBuffer, firstByte);
            case SegmentReadBuffer segmentReadBuffer -> readSegmentInt(segmentReadBuffer, firstByte);
        };
    }

    private static boolean negativeIntOverflow(int r, byte b) {
        return r < N_DIV_10_I || (r == N_DIV_10_I && b < N_MOD_10_I);
    }

    private static int readHeapInt(HeapReadBuffer heapReadBuffer, byte firstByte) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        int position = heapReadBuffer.intPosition();
        boolean positive = true;
        int r;
        if (firstByte == BYTE_MINUS) {
            if(position == bytes.length) {
                throw new NumberFormatException("illegal leading minus sign");
            }
            positive = false;
            firstByte = bytes[position++];
        }
        if (firstByte == BYTE_ZERO) {
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
            if(b > 0) {
                break ;
            }
            if(negativeIntOverflow(r, b)) {
                throw new ArithmeticException("integer overflow");
            }
            r = r * 10 + b;
            position++;
        }
        if(positive && r == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        heapReadBuffer.setPosition(position);
        return positive ? -r : r;
    }

    private static int readSegmentInt(SegmentReadBuffer segmentReadBuffer, byte firstByte) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        int position = segmentReadBuffer.intPosition();
        int len = Math.toIntExact(segment.byteSize());
        boolean positive = true;
        int r;
        if (firstByte == BYTE_MINUS) {
            if(position == segment.byteSize()) {
                throw new NumberFormatException("illegal leading minus sign");
            }
            positive = false;
            firstByte = SegmentAccess.getByte(segment, position++);
        }
        if (firstByte == BYTE_ZERO) {
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
            if(b > 0) {
                break ;
            }
            if(negativeIntOverflow(r, b)) {
                throw new ArithmeticException("integer overflow");
            }
            r =  r * 10 + b;
            position++;
        }
        if(positive && r == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        segmentReadBuffer.setPosition(position);
        return positive ? -r : r;
    }

    public static long readLong(ReadBuffer readBuffer, byte firstByte) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> readHeapLong(heapReadBuffer, firstByte);
            case SegmentReadBuffer segmentReadBuffer -> readSegmentLong(segmentReadBuffer, firstByte);
        };
    }

    private static boolean negativeLongOverflow(long r, byte b) {
        return r < N_DIV_10_L || (r == N_DIV_10_L && b < N_MOD_10_L);
    }

    private static long readHeapLong(HeapReadBuffer heapReadBuffer, byte firstByte) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        int position = heapReadBuffer.intPosition();
        boolean positive = true;
        long r;
        if (firstByte == BYTE_MINUS) {
            if(position == bytes.length) {
                throw new NumberFormatException("illegal leading minus sign");
            }
            positive = false;
            firstByte = bytes[position++];
        }
        if (firstByte == BYTE_ZERO) {
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
            if(b > 0) {
                break ;
            }
            if(negativeLongOverflow(r, b)) {
                throw new ArithmeticException("long overflow");
            }
            r = r * 10L + b;
            position++;
        }
        if(positive && r == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        heapReadBuffer.setPosition(position);
        return positive ? -r : r;
    }

    private static long readSegmentLong(SegmentReadBuffer segmentReadBuffer, byte firstByte) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        int position = segmentReadBuffer.intPosition();
        int len = Math.toIntExact(segment.byteSize());
        boolean positive = true;
        long r;
        if (firstByte == BYTE_MINUS) {
            if(position == segment.byteSize()) {
                throw new NumberFormatException("illegal leading minus sign");
            }
            positive = false;
            firstByte = SegmentAccess.getByte(segment, position++);
        }
        if (firstByte == BYTE_ZERO) {
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
            if(b > 0) {
                break ;
            }
            if(negativeLongOverflow(r, b)) {
                throw new ArithmeticException("long overflow");
            }
            r =  r * 10L + b;
            position++;
        }
        if(positive && r == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        segmentReadBuffer.setPosition(position);
        return positive ? -r : r;
    }

    // exposed for test purpose
    // Parses a string-format floating-point number into a specific format.
    // Enforces strict format validation; patterns like ".123E0123" are rejected,
    // although they are acceptable by the JDK parser.
    public static FpStrRep parseFpStrRep(ReadBuffer readBuffer, byte firstByte) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> parseHeapFpStrRep1(heapReadBuffer, firstByte);
            case SegmentReadBuffer segmentReadBuffer -> parseSegmentFpStrRep1(segmentReadBuffer, firstByte);
        };
    }

    private static FpStrRep parseHeapFpStrRep1(HeapReadBuffer heapReadBuffer, byte firstByte) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        final int position = heapReadBuffer.intPosition();
        final int end = position + Math.min(bytes.length - position, MAX_FP_INPUT);
        int index = position;
        boolean neg = false;
        boolean negExp = false;
        boolean trunc = false;
        long d;
        int frac = 0;
        int p = 0;
        if(firstByte == BYTE_MINUS) {
            if(index == end) {
                throw new NumberFormatException("illegal leading minus sign");
            }
            neg = true;
            firstByte = bytes[index++];
        }
        if(firstByte == BYTE_ZERO) {
            if(index == end) {
                return new FpStrRep(neg, false, 0L, frac, p, 0);
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[index])];
            if(v <= 0) {
                throw new NumberFormatException("leading zero");
            }
            d = 0L;
        } else {
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
            if (v > 0) {
                throw new NumberFormatException("not a digit : " + firstByte);
            }
            d = -v;
        }
        // process following digits part, we can ensure that d will not be 0 here
        byte b = Byte.MIN_VALUE;
        int nd = 1;
        while (index < end) {
            b = bytes[index];
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
            if(v > 0) {
                break ;
            }
            if(nd < MAX_DECIMAL_ND) {
                d = d * 10L - v;
                nd++;
            } else {
                trunc = true;
            }
            index++;
        }
        // processing optional fraction part
        if(b == BYTE_PERIOD) {
            if(++index == end) {
                throw new NumberFormatException("leading period with no digits");
            }
            b = bytes[index++];
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
            if(v > 0) {
                throw new NumberFormatException("illegal start of number : " + b);
            }
            if(nd < MAX_DECIMAL_ND) {
                d = d * 10L - v;
                nd++;
                frac++;
            }
            while (index < end) {
                b = bytes[index];
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
                if(v > 0) {
                    break ;
                }
                if(nd < MAX_DECIMAL_ND) {
                    d = d * 10L - v;
                    nd++;
                    frac++;
                } else {
                    trunc = true;
                }
                index++;
            }
        }
        // processing optional exponent part
        // note that leading zeros are allowed in the exponent partaccording to the JSON specification
        if(b == BYTE_E || b == BYTE_e) {
            if(++index == end) {
                throw new NumberFormatException("leading exponent with no digits");
            }
            b = bytes[index++];
            if(b == BYTE_MINUS || b == BYTE_PLUS) {
                if(index == end) {
                    throw new NumberFormatException("leading exponent sign with no digits");
                }
                negExp = b == BYTE_MINUS;
                b = bytes[index++];
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
            if(v > 0) {
                throw new NumberFormatException("illegal start of number : " + b);
            }
            p = -v;
            while (index < end) {
                b = bytes[index];
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
                if(v > 0) {
                    break ;
                }
                if(p < MAX_DECIMAL_P) {
                    p = p * 10 - v;
                }
                index++;
            }
        }
        p = (negExp ? -p : p) - frac;
        return new FpStrRep(neg, trunc, d, frac, p, index - position);
    }

    private static FpStrRep parseSegmentFpStrRep1(SegmentReadBuffer segmentReadBuffer, byte firstByte) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        final long position = segmentReadBuffer.longPosition();
        final long end = position + Math.min(segment.byteSize() - position, MAX_FP_INPUT);
        long index = position;
        boolean neg = false;
        boolean negExp = false;
        boolean trunc = false;
        long d;
        int frac = 0;
        int p = 0;
        if(firstByte == BYTE_MINUS) {
            if(index == end) {
                throw new NumberFormatException("illegal leading minus sign");
            }
            neg = true;
            firstByte = SegmentAccess.getByte(segment, index++);
        }
        if(firstByte == BYTE_ZERO) {
            if(index == end) {
                return new FpStrRep(neg, false, 0L, frac, p, 0);
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, index))];
            if(v <= 0) {
                throw new NumberFormatException("leading zero");
            }
            d = 0L;
        } else {
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
            if (v > 0) {
                throw new NumberFormatException("not a digit : " + firstByte);
            }
            d = -v;
        }
        // process following digits part, we can ensure that d will not be 0 here
        byte b = Byte.MIN_VALUE;
        int nd = 1;
        while (index < end) {
            b = SegmentAccess.getByte(segment, index);
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
            if(v > 0) {
                break ;
            }
            if(nd < MAX_DECIMAL_ND) {
                d = d * 10L - v;
                nd++;
            } else {
                trunc = true;
            }
            index++;
        }
        // processing optional fraction part
        if(b == BYTE_PERIOD) {
            if(++index == end) {
                throw new NumberFormatException("leading period with no digits");
            }
            b = SegmentAccess.getByte(segment, index++);
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
            if(v > 0) {
                throw new NumberFormatException("illegal start of number : " + b);
            }
            if(nd < MAX_DECIMAL_ND) {
                d = d * 10L - v;
                nd++;
                frac++;
            }
            while (index < end) {
                b = SegmentAccess.getByte(segment, index);
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
                if(v > 0) {
                    break ;
                }
                if(nd < MAX_DECIMAL_ND) {
                    d = d * 10L - v;
                    nd++;
                    frac++;
                } else {
                    trunc = true;
                }
                index++;
            }
        }
        // processing optional exponent part
        // note that leading zeros are allowed in the exponent partaccording to the JSON specification
        if(b == BYTE_E || b == BYTE_e) {
            if(++index == end) {
                throw new NumberFormatException("leading exponent with no digits");
            }
            b = SegmentAccess.getByte(segment, index++);
            if(b == BYTE_MINUS || b == BYTE_PLUS) {
                if(index == end) {
                    throw new NumberFormatException("leading exponent sign with no digits");
                }
                negExp = b == BYTE_MINUS;
                b = SegmentAccess.getByte(segment, index++);
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
            if(v > 0) {
                throw new NumberFormatException("illegal start of number : " + b);
            }
            p = -v;
            while (index < end) {
                b = SegmentAccess.getByte(segment, index);
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
                if(v > 0) {
                    break ;
                }
                if(p < MAX_DECIMAL_P) {
                    p = p * 10 - v;
                }
                index++;
            }
        }
        p = (negExp ? -p : p) - frac;
        return new FpStrRep(neg, trunc, d, frac, p, Math.toIntExact(index - position));
    }

    public static float readFloat(ReadBuffer readBuffer, byte firstByte) {
        FpStrRep rep = parseFpStrRep(readBuffer, firstByte);
        float r = parseFloat(readBuffer, rep);
        int position = readBuffer.intPosition();
        readBuffer.setPosition(position + rep.len());
        return r;
    }

    public static float parseFloat(ReadBuffer readBuffer, FpStrRep rep) {
        if(rep.trunc()) {
            return parseFloatFallback(readBuffer, rep);
        }
        int sign = rep.negative() ? (1 << (FLOAT_SPEC.mantBits() + FLOAT_SPEC.expBits())) : 0;
        long d = rep.d();
        int p = rep.p();
        if(d == 0L || p < FLOAT_SPEC.minDecExp() - MAX_DECIMAL_ND - 2) {
            return Float.intBitsToFloat(sign);
        }
        if(p > FLOAT_SPEC.maxDecExp() + 2) {
            return Float.intBitsToFloat(sign | (0xff << FLOAT_SPEC.mantBits()));
        }
        if(d >> FLOAT_SPEC.mantBits() == 0L) {
            float f = (float) Math.toIntExact(rep.negative() ? -d : d);
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
            return parseFloatFallback(readBuffer, rep);
        }
        return Float.intBitsToFloat(
                (m & ~(1 << FLOAT_SPEC.mantBits())) |
                        (FLOAT_SPEC.mantBits() - FLOAT_SPEC.bias() + e) << FLOAT_SPEC.mantBits());
    }

    private static float parseFloatFallback(ReadBuffer readBuffer, FpStrRep FpStrRep) {
        int len = FpStrRep.len();
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                byte[] bytes = heapReadBuffer.rawByteArray();
                int position = heapReadBuffer.intPosition();
                String s = new String(bytes, position - 1, len + 1, StandardCharsets.US_ASCII);
                return Float.parseFloat(s);
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                MemorySegment segment = segmentReadBuffer.rawSegment();
                long position = segmentReadBuffer.longPosition();
                byte[] bytes = segment.asSlice(position - 1L, len + 1L).toArray(ValueLayout.JAVA_BYTE);
                String s = new String(bytes, StandardCharsets.US_ASCII);
                return Float.parseFloat(s);
            }
        }
    }

    public static double readDouble(ReadBuffer readBuffer, byte firstByte) {
        FpStrRep rep = parseFpStrRep(readBuffer, firstByte);
        double r = parseDouble(readBuffer, rep);
        int position = readBuffer.intPosition();
        readBuffer.setPosition(position + rep.len());
        return r;
    }

    public static double parseDouble(ReadBuffer readBuffer, FpStrRep rep) {
        if(rep.trunc()) {
            return parseDoubleFallback(readBuffer, rep);
        }
        long sign = rep.negative() ? (1L << (DOUBLE_SPEC.mantBits() + DOUBLE_SPEC.expBits())) : 0L;
        long d = rep.d();
        int p = rep.p();
        if(d == 0L || p < DOUBLE_SPEC.minDecExp() - MAX_DECIMAL_ND - 2) {
            return Double.longBitsToDouble(sign);
        }
        if(p > DOUBLE_SPEC.maxDecExp() + 2) {
            return Double.longBitsToDouble(sign | (0x7ffL << DOUBLE_SPEC.mantBits()));
        }
        if(d >>> DOUBLE_SPEC.mantBits() == 0L) {
            double f = (double) (rep.negative() ? -d : d);
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
            return parseDoubleFallback(readBuffer, rep);
        }
        return Double.longBitsToDouble(
                (m & ~(1L << DOUBLE_SPEC.mantBits())) |
                        ((long) (DOUBLE_SPEC.mantBits() - DOUBLE_SPEC.bias() + e)) << DOUBLE_SPEC.mantBits());
    }

    private static double parseDoubleFallback(ReadBuffer readBuffer, FpStrRep FpStrRep) {
        int len = FpStrRep.len();
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                byte[] bytes = heapReadBuffer.rawByteArray();
                int position = heapReadBuffer.intPosition();
                String s = new String(bytes, position - 1, len + 1, StandardCharsets.US_ASCII);
                return Double.parseDouble(s);
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                MemorySegment segment = segmentReadBuffer.rawSegment();
                long postion = segmentReadBuffer.longPosition();
                byte[] bytes = segment.asSlice(postion - 1L, len + 1L).toArray(ValueLayout.JAVA_BYTE);
                String s = new String(bytes, StandardCharsets.US_ASCII);
                return Double.parseDouble(s);
            }
        }
    }

    /**
     * Represents a binary floating-point number as m * 2^e.
     */
    public record BinaryFp(long m, int e) {
    }

    /**
     * Represents a decimal floating-point number as d * 10^p.
     */
    public record DecimalFp(long d, int p) {
    }

    /**
     * Defines the specification parameters for a floating-point format (e.g., mantissa bits, exponent bits, bias, etc.).
     * Predefined constants are available for 32-bit and 64-bit floats.
     * Note: This implementation is strictly limited to processing 64-bit float values.
     * Do not apply this to larger formats like FP128, as the current algorithms cannot handle the extended range.
     */
    public record FpSpec(
            int mantBits,
            int expBits,
            int bias,
            int minExp,
            int maxDecExp,
            int minDecExp
    ) {
    }

    /**
     * Holds precomputed scaling constants (a 128-bit multiplier pm and a shift count s) for a given BinaryFp value.
     */
    public record Scalers (
            long pmHi,
            long pmLo,
            int s
    ) {
    }
}
